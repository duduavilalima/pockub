# TDD-004 — Persistência: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

| Arquivo | Recurso |
|---------|---------|
| `k8s/postgresql/postgresql-config.yaml` | ConfigMap com `POSTGRES_DB` |
| `k8s/postgresql/postgresql-statefulset.yaml` | StatefulSet `postgresql` (`postgres:17-alpine`, PVC 1Gi) |
| `k8s/mongodb/mongodb-statefulset.yaml` | StatefulSet `mongodb` (`mongo:7`, PVC 1Gi) |

Os Services `postgresql` e `mongodb` já existiam (TDD-003) e são o `serviceName` dos StatefulSets. PVCs criados automaticamente: `postgres-data-postgresql-0` e `mongodb-data-mongodb-0`.

## 2. Por que cada decisão foi tomada

- **StatefulSet em vez de Deployment**: dá nome estável (`postgresql-0`) e um PVC próprio por réplica, que o Pod recriado reencontra pelo nome.
- **`volumeClaimTemplates`**: o StatefulSet cria os PVCs; a StorageClass `standard` do Minikube provisiona o PV (hostPath) dinamicamente.
- **Credenciais do PostgreSQL via `envFrom`** (ConfigMap + Secret do TDD-003): o banco é criado com os mesmos usuário/senha que a API usa.
- **Probes `pg_isready` / `mongosh ping`**: o Pod só entra no Service quando o banco realmente aceita conexões, o que reduz a corrida com o Flyway da API.
- **1 réplica sem replicação**: HA fica fora do escopo de estudo.

## 3. Alternativas descartadas

- **Deployment + PVC manual**: perde identidade estável e o vínculo automático Pod↔PVC.
- **PV estático**: o provisionador dinâmico do Minikube torna desnecessário.
- **Job de migration**: o Flyway da API já cria o schema no startup.

## 4. Pontos de atenção

- **Divergência do TDD**: as probes do MongoDB receberam `timeoutSeconds: 5` e `--quiet`. O default de 1s é curto para o `mongosh`, que demora a iniciar, e geraria reinícios por falso negativo.
- **Apagar o StatefulSet não apaga o PVC**; apagar o PVC apaga o PV (`Delete`) e os dados.
- **`minikube delete` destrói os dados** (hostPath dentro da VM/container do Minikube).
- **MongoDB sem autenticação** — só para estudo.
- Os bancos temporários usados na validação do TDD-003 já tinham sido removidos; os nomes `postgresql-0`/`mongodb-0` não conflitam.

## 5. Como validar

```bash
kubectl apply -f k8s/postgresql/ -f k8s/mongodb/
kubectl get pods,pvc -n learning-lab          # Pods Running, PVCs Bound

# Cenários 4 e 5
H="Host: product-api.local"; IP=$(minikube ip)
curl -H "$H" -H 'Content-Type: application/json' -d '{"nome":"Teclado","preco":299.90,"quantidadeEstoque":10}' $IP/produtos
curl -H "$H" $IP/produtos/1
kubectl delete pod postgresql-0 mongodb-0 -n learning-lab
kubectl wait pod/postgresql-0 pod/mongodb-0 -n learning-lab --for=condition=Ready
curl -H "$H" $IP/produtos/1    # 200, mesmo produto
curl -H "$H" $IP/logs          # logs anteriores presentes
```

### Resultado da validação (execução real)

- `postgresql-0` e `mongodb-0` `Running`; PVCs `Bound` (1Gi, `standard`).
- Produto criado e consultado; após `kubectl delete pod postgresql-0 mongodb-0`, os Pods voltaram com o mesmo nome e o **mesmo PVC/PV**, `GET /produtos/1` → 200 e `GET /logs` trouxe o log gerado antes da exclusão.
- API conecta via `postgresql:5432` e `mongodb:27017` (restart do Deployment com os bancos reais, sem restarts do container).
