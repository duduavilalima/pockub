# TDD-003 — Infraestrutura Kubernetes Base: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

| Arquivo | Recurso |
|---------|---------|
| `k8s/namespace/learning-lab.yaml`, `monitoring.yaml` | Namespaces |
| `k8s/configmaps/product-api-config.yaml` | ConfigMap com `DB_URL`, `MONGO_URI`, `LOGGING_LEVEL_ROOT` |
| `k8s/secrets/postgresql-secret.yaml` | Credenciais consumidas pelo container do PostgreSQL |
| `k8s/secrets/product-api-secret.yaml` | `DB_USERNAME` / `DB_PASSWORD` consumidos pela API |
| `k8s/deployments/product-api.yaml` | Deployment (1 réplica, RollingUpdate, initContainer, probes, resources) |
| `k8s/services/{postgresql,mongodb,product-api}-service.yaml` | Dois ClusterIP (bancos) e um NodePort (API, porta 30080) |
| `k8s/ingress/product-api-ingress.yaml` | Ingress `product-api.local` → `product-api:80` |

Os manifests seguem exatamente o TDD; não houve divergência de conteúdo.

## 2. Por que cada decisão foi tomada

- **ConfigMap × Secret**: URLs não são sensíveis (ConfigMap); usuário/senha vão em Secret. Ambos entram no container por `envFrom`, e as chaves coincidem com os placeholders do `application.yaml`, então a imagem não muda entre ambientes.
- **Secrets duplicados** (`postgresql-secret` e `product-api-secret`): cada consumidor recebe os nomes de variável que espera (`POSTGRES_USER` vs `DB_USERNAME`). Os valores precisam ser iguais.
- **initContainer `wait-for-postgres`**: o Flyway roda no startup e o Hikari não retenta; sem espera, a API cai em `CrashLoopBackOff` se subir antes do banco.
- **Um único Service para a API**: `NodePort` já inclui um ClusterIP, que atende também Ingress, Prometheus e Jobs.
- **Ingress sem `rewrite-target`**: a API já atende em `/produtos` e `/logs`; reescrever tudo para `/` quebraria as rotas.
- **Services dos bancos aqui, StatefulSets no TDD-004**: o initContainer e a `DB_URL` dependem apenas dos nomes DNS `postgresql` e `mongodb`.

## 3. Alternativas descartadas

- **Dois Services para a API** (NodePort + ClusterIP): redundante.
- **Credenciais no ConfigMap ou na imagem**: expõe segredos e exige rebuild.
- **Ingress com host `localhost`/IP direto**: o host nomeado demonstra o roteamento por `Host`.

## 4. Pontos de atenção

- **Sem os bancos, o Pod não fica `Ready`** (preso no initContainer). É esperado até o TDD-004.
- **`nc -z` só prova que a porta abriu**, não que o banco aceita conexões. Na validação o Pod teve **1 restart** porque o PostgreSQL abriu a porta antes de terminar a inicialização e o Flyway falhou uma vez; o Kubernetes reiniciou o container e a API subiu. Com StatefulSet + readinessProbe (TDD-004) isso tende a sumir.
- **Variáveis de ambiente não são recarregadas**: após editar o ConfigMap, `kubectl rollout restart deployment/product-api -n learning-lab` (Cenário 11).
- **Base64 não é criptografia.**
- **Ingress exige o addon** (`minikube addons enable ingress`) e a entrada `$(minikube ip) product-api.local` no `/etc/hosts`. Sem editar o hosts, teste com `curl -H "Host: product-api.local" http://$(minikube ip)/produtos`.
- **Imagem**: precisa estar no daemon do Minikube (`eval $(minikube docker-env)`; ver TDD-002), pois `imagePullPolicy: IfNotPresent`.
- **Fase 8**: ao adotar o Ingress como entrada única, trocar o Service para `ClusterIP` e remover `nodePort`.

## 5. Como validar

```bash
minikube addons enable ingress
kubectl apply --dry-run=server -f k8s/secrets/ -f k8s/configmaps/ -f k8s/services/ -f k8s/deployments/
kubectl apply -f k8s/namespace/ -f k8s/secrets/ -f k8s/configmaps/
kubectl apply -f k8s/postgresql/ -f k8s/mongodb/      # TDD-004
kubectl apply -f k8s/deployments/ -f k8s/services/ -f k8s/ingress/

kubectl get pods -l app=product-api -n learning-lab   # só Pods da API (Cenário 17)
curl http://$(minikube ip):30080/produtos             # NodePort
curl -H "Host: product-api.local" http://$(minikube ip)/produtos   # Ingress

# Cenário 11
kubectl patch configmap product-api-config -n learning-lab --type merge -p '{"data":{"LOGGING_LEVEL_ROOT":"DEBUG"}}'
kubectl rollout restart deployment/product-api -n learning-lab
kubectl exec deploy/product-api -n learning-lab -- env | grep LOGGING
```

### Resultado da validação (execução real)

- Dry-run server de secrets, configmap, services e deployment: OK.
- Com PostgreSQL e MongoDB **temporários** (Pods avulsos, depois removidos; os manifests reais são do TDD-004): Deployment `Running`, ReplicaSet criado, `/produtos` e `/actuator/health` → 200 via NodePort, `/produtos` → 200 via Ingress.
- ConfigMap alterado + `rollout restart`: nova variável visível no Pod.
- Não executado: Cenário 12 completo e `/etc/hosts` (testado com header `Host`).
