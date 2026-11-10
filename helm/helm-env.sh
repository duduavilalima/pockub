#!/usr/bin/env bash
# Wrapper do helm que lê as senhas de helm/.env e as injeta via --set-file
# (o conteúdo vai para arquivos temporários, nunca para a linha de comando).
#
# Uso: helm/helm-env.sh <comando helm> [args...]
#   helm/helm-env.sh install learning-lab helm/kubernetes-learning-lab -n learning-lab --create-namespace
#   helm/helm-env.sh upgrade learning-lab helm/kubernetes-learning-lab -n learning-lab --set hpa.enabled=false
#   ENV_FILE=outro.env helm/helm-env.sh template x helm/kubernetes-learning-lab
set -euo pipefail

ENV_FILE="${ENV_FILE:-$(dirname "$0")/.env}"
[[ -f "$ENV_FILE" ]] || { echo "Arquivo $ENV_FILE não encontrado (copie helm/.env.example)." >&2; exit 1; }

# lê KEY=VALUE sem executar o arquivo (aceita espaços e $ no valor)
get() { { grep -E "^$1=" "$ENV_FILE" || true; } | tail -n1 | cut -d= -f2- ; }

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
args=()
add() { # add <variável do .env> <chave do values> [obrigatória]
  local v; v="$(get "$1")"
  if [[ -z "$v" ]]; then
    [[ "${3:-}" == required ]] && { echo "$1 não definida em $ENV_FILE" >&2; exit 1; }
    return 0
  fi
  printf '%s' "$v" > "$TMP/$1"; args+=(--set-file "$2=$TMP/$1")
}
add POSTGRES_PASSWORD postgresql.credentials.password required
add GRAFANA_ADMIN_PASSWORD monitoring.grafana.adminPassword

helm "$@" "${args[@]}"
