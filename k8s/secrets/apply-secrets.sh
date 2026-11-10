#!/usr/bin/env bash
# Cria/atualiza os Secrets do projeto a partir de k8s/secrets/.env (não versionado):
#   learning-lab/postgresql-secret   (POSTGRES_USER, POSTGRES_PASSWORD)
#   learning-lab/product-api-secret  (DB_USERNAME, DB_PASSWORD)
#   monitoring/grafana-secret        (admin-password)  — pulado se o namespace monitoring não existir
# Os valores passam por arquivos temporários (--from-env-file), nunca pela linha de comando.
#
# Uso: k8s/secrets/apply-secrets.sh        (ENV_FILE=outro.env para trocar o arquivo)
# Pré-requisito: namespaces criados (kubectl apply -f k8s/namespace/).
set -euo pipefail

ENV_FILE="${ENV_FILE:-$(dirname "$0")/.env}"
[[ -f "$ENV_FILE" ]] || { echo "Arquivo $ENV_FILE não encontrado (copie k8s/secrets/.env.example)." >&2; exit 1; }

# lê KEY=VALUE sem executar o arquivo
get() { { grep -E "^$1=" "$ENV_FILE" || true; } | tail -n1 | cut -d= -f2- ; }
need() { local v; v="$(get "$1")"; [[ -n "$v" ]] || { echo "$1 não definida em $ENV_FILE" >&2; exit 1; }; printf '%s' "$v"; }

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
PG_USER="$(need POSTGRES_USER)"; PG_PASS="$(need POSTGRES_PASSWORD)"

apply_secret() { # apply_secret <namespace> <nome> <arquivo-env>
  kubectl create secret generic "$2" -n "$1" --from-env-file="$3" --dry-run=client -o yaml | kubectl apply -f -
}

printf 'POSTGRES_USER=%s\nPOSTGRES_PASSWORD=%s\n' "$PG_USER" "$PG_PASS" > "$TMP/pg.env"
printf 'DB_USERNAME=%s\nDB_PASSWORD=%s\n' "$PG_USER" "$PG_PASS" > "$TMP/api.env"
apply_secret learning-lab postgresql-secret "$TMP/pg.env"
apply_secret learning-lab product-api-secret "$TMP/api.env"

if kubectl get ns monitoring >/dev/null 2>&1; then
  printf 'admin-password=%s\n' "$(need GRAFANA_ADMIN_PASSWORD)" > "$TMP/grafana.env"
  apply_secret monitoring grafana-secret "$TMP/grafana.env"
else
  echo "namespace monitoring não existe — grafana-secret ignorado"
fi
