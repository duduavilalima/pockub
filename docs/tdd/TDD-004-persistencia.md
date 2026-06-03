# TDD-004 — Persistência

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [MODEL.md](../MODEL.md) · [ADR-003](../adr/ADR-003-postgresql-relacional.md) · [ADR-004](../adr/ADR-004-mongodb-nosql.md) · [ADR-006](../adr/ADR-006-statefulset-databases.md)

---

## 1. Visão Geral

Define os recursos Kubernetes para persistência: StatefulSets, PersistentVolumes, PersistentVolumeClaims e StorageClass para PostgreSQL e MongoDB. Ambos os bancos rodam como StatefulSets para garantir identidade estável de Pod e volumes vinculados por nome.

---

## 2. StorageClass

O Minikube provisiona dinamicamente PVs via `standard` (hostPath). Não é necessário criar StorageClass manualmente.

```bash
kubectl get storageclass
# NAME                 PROVISIONER                RECLAIMPOLICY
# standard (default)   k8s.io/minikube-hostpath   Delete
```

`ReclaimPolicy: Delete` — ao deletar o PVC, o PV é removido. Adequado para o ambiente de estudo.

---

## 3. PostgreSQL

### 3.1 ConfigMap

```yaml
# k8s/postgres/postgresql-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgresql-config
  namespace: learning-lab
data:
  POSTGRES_DB: "pockub"
```

Usuário e senha vêm do Secret `postgresql-secret` (ver [TDD-003](TDD-003-infraestrutura-kubernetes.md)).

---

### 3.2 StatefulSet

```yaml
# k8s/postgres/postgresql-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgresql
  namespace: learning-lab
  labels:
    app: postgresql
spec:
  serviceName: postgresql
  replicas: 1
  selector:
    matchLabels:
      app: postgresql
  template:
    metadata:
      labels:
        app: postgresql
    spec:
      containers:
        - name: postgresql
          image: postgres:17-alpine
          ports:
            - containerPort: 5432
          envFrom:
            - configMapRef:
                name: postgresql-config
            - secretRef:
                name: postgresql-secret
          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          volumeMounts:
            - name: postgres-data
              mountPath: /var/lib/postgresql/data
          livenessProbe:
            exec:
              command: ["pg_isready", "-U", "pockub"]
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "pockub"]
            initialDelaySeconds: 5
            periodSeconds: 5
  volumeClaimTemplates:
    - metadata:
        name: postgres-data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: standard
        resources:
          requests:
            storage: 1Gi
```

**Nome do Pod:** `postgresql-0`
**Nome do PVC:** `postgres-data-postgresql-0`

---

### 3.3 Migração de Schema (Init Container)

O schema do PostgreSQL é criado antes do startup da API via Init Container no Deployment (alternativa ao Job):

```yaml
# Trecho do Deployment product-api
initContainers:
  - name: wait-for-postgres
    image: busybox:1.36
    command:
      - sh
      - -c
      - |
        until nc -z postgresql 5432; do
          echo "aguardando PostgreSQL..."; sleep 2
        done
```

O Flyway no startup da API executa as migrations automaticamente ao conectar.

---

## 4. MongoDB

### 4.1 StatefulSet

```yaml
# k8s/mongodb/mongodb-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mongodb
  namespace: learning-lab
  labels:
    app: mongodb
spec:
  serviceName: mongodb
  replicas: 1
  selector:
    matchLabels:
      app: mongodb
  template:
    metadata:
      labels:
        app: mongodb
    spec:
      containers:
        - name: mongodb
          image: mongo:7
          ports:
            - containerPort: 27017
          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          volumeMounts:
            - name: mongodb-data
              mountPath: /data/db
          livenessProbe:
            exec:
              command:
                - mongosh
                - --eval
                - "db.adminCommand('ping')"
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            exec:
              command:
                - mongosh
                - --eval
                - "db.adminCommand('ping')"
            initialDelaySeconds: 10
            periodSeconds: 5
  volumeClaimTemplates:
    - metadata:
        name: mongodb-data
      spec:
        accessModes: ["ReadWriteOnce"]
        storageClassName: standard
        resources:
          requests:
            storage: 1Gi
```

**Nome do Pod:** `mongodb-0`
**Nome do PVC:** `mongodb-data-mongodb-0`

---

## 5. Resumo dos PVCs

| PVC | Banco | Tamanho | Mount |
|-----|-------|---------|-------|
| `postgres-data-postgresql-0` | PostgreSQL | 1Gi | `/var/lib/postgresql/data` |
| `mongodb-data-mongodb-0` | MongoDB | 1Gi | `/data/db` |

---

## 6. Validação de Persistência

### Cenário PostgreSQL (Cenário 4 do PRD)

```bash
# 1. Criar produto via API
curl -X POST http://product-api.local/produtos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado","preco":299.90,"quantidadeEstoque":10}'

# 2. Deletar o Pod (StatefulSet recria automaticamente)
kubectl delete pod postgresql-0 -n learning-lab

# 3. Aguardar recriação
kubectl wait pod/postgresql-0 -n learning-lab --for=condition=Ready

# 4. Verificar que o produto persiste
curl http://product-api.local/produtos/1
# Esperado: 200 OK com os dados do produto
```

### Cenário MongoDB (Cenário 5 do PRD)

```bash
# 1. Gerar logs via consulta
curl http://product-api.local/produtos/1

# 2. Deletar o Pod
kubectl delete pod mongodb-0 -n learning-lab

# 3. Aguardar recriação
kubectl wait pod/mongodb-0 -n learning-lab --for=condition=Ready

# 4. Verificar que os logs persistem
curl http://product-api.local/logs
# Esperado: 200 OK com os logs anteriores
```

---

## 7. Ciclo de Vida do PVC

```
StatefulSet criado
    └──► PVC criado automaticamente via volumeClaimTemplate
             └──► PV provisionado pelo StorageClass (hostPath no Minikube)

kubectl delete statefulset postgresql
    └──► Pod excluído
    └──► PVC NÃO é excluído automaticamente ← comportamento importante!

kubectl delete pvc postgres-data-postgresql-0
    └──► PV excluído (ReclaimPolicy: Delete)
    └──► Dados perdidos permanentemente
```

---

## 8. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| PVC não removido com StatefulSet | Ao recriar o StatefulSet, o novo Pod se vincula ao PVC existente — dados preservados |
| `hostPath` no Minikube | PV fisicamente em `/tmp/hostpath-provisioner/` na VM do Minikube — perdido ao fazer `minikube delete` |
| Sem autenticação MongoDB | POC sem auth — em produção configurar usuário/senha via Secret |
| Sem replicação | 1 réplica por banco — sem HA; adequado para estudo |
| Startup do PostgreSQL | `pg_isready` na probe pode falhar nos primeiros segundos de inicialização — `initialDelaySeconds: 30` mitiga isso |
