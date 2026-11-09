# TDD-003 — Infraestrutura Kubernetes Base

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [ADR-001](../adr/ADR-001-minikube-local-cluster.md) · [ADR-005](../adr/ADR-005-dual-namespace.md) · [TDD-004](TDD-004-persistencia.md) · [TDD-005](TDD-005-escalabilidade-confiabilidade.md)
**Estórias:** [US-010](../stories/US-010-infraestrutura-kubernetes-base.md)
**PRD:** [Fase 3 — Deploy da Aplicação](../PRD.md#fase-3--deploy-da-aplicação) · [Fase 5 — Configuração](../PRD.md#fase-5--configuração) · [Fase 8 — Exposição Externa](../PRD.md#fase-8--exposição-externa) · [Cenário 1 — Deploy Completo da Plataforma](../PRD.md#cenário-1--deploy-completo-da-plataforma) · [Cenário 11 — ConfigMap](../PRD.md#cenário-11--configmap) · [Cenário 12 — Secrets](../PRD.md#cenário-12--secrets) · [Cenário 17 — Labels e Selectors](../PRD.md#cenário-17--labels-e-selectors)

---

## 1. Visão Geral

Define os recursos Kubernetes base que sustentam toda a plataforma: namespaces, Deployment da API, Services, Ingress, ConfigMaps e Secrets. Esses recursos são o ponto de partida antes da persistência e da observabilidade.

---

## 2. Namespaces

### learning-lab

```yaml
# k8s/namespace/learning-lab.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: learning-lab
  labels:
    app.kubernetes.io/managed-by: kubectl
    env: study
```

### monitoring

```yaml
# k8s/namespace/monitoring.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: monitoring
  labels:
    app.kubernetes.io/managed-by: kubectl
    env: study
```

---

## 3. ConfigMap — Product API

```yaml
# k8s/configmaps/product-api-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: product-api-config
  namespace: learning-lab
data:
  DB_URL: "jdbc:postgresql://postgresql:5432/pockub"
  MONGO_URI: "mongodb://mongodb:27017/pockub"   # sem credenciais (MongoDB sem auth na POC)
  LOGGING_LEVEL_ROOT: "INFO"                    # relaxed binding → logging.level.root
```

> As chaves do ConfigMap viram variáveis de ambiente via `envFrom` e precisam coincidir com os placeholders do `application.yaml` (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MONGO_URI`). Variáveis de ambiente **não** são atualizadas em Pods em execução: após alterar o ConfigMap, use `kubectl rollout restart deployment/product-api -n learning-lab`.

---

## 4. Secrets

### Credenciais do PostgreSQL (consumidas pelo container do banco)

```yaml
# k8s/secrets/postgresql-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgresql-secret
  namespace: learning-lab
type: Opaque
data:
  POSTGRES_USER: cG9ja3Vi          # base64: pockub
  POSTGRES_PASSWORD: cG9ja3ViMTIz  # base64: pockub123
```

O nome do banco (`POSTGRES_DB`) não é sensível e fica no ConfigMap `postgresql-config` ([TDD-004](TDD-004-persistencia.md)).

### Credenciais da Product API (nomes esperados pelo `application.yaml`)

```yaml
# k8s/secrets/product-api-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: product-api-secret
  namespace: learning-lab
type: Opaque
data:
  DB_USERNAME: cG9ja3Vi            # base64: pockub
  DB_PASSWORD: cG9ja3ViMTIz        # base64: pockub123
```

> Os valores de `DB_USERNAME`/`DB_PASSWORD` devem ser idênticos aos de `POSTGRES_USER`/`POSTGRES_PASSWORD`. Não há Secret para o MongoDB: a POC não usa autenticação ([ADR-004](../adr/ADR-004-mongodb-nosql.md)); se a auth for habilitada, a `MONGO_URI` (com credenciais) passa a ir para o `product-api-secret`.

> **Atenção:** Secrets em Base64 não são criptografados — apenas codificados. Em produção, usar Sealed Secrets ou External Secrets Operator.

---

## 5. Deployment — Product API

```yaml
# k8s/deployments/product-api.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-api
  namespace: learning-lab
  labels:
    app: product-api
    version: "1.0.0"
spec:
  replicas: 1
  selector:
    matchLabels:
      app: product-api
  template:
    metadata:
      labels:
        app: product-api
        version: "1.0.0"
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
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
          resources:
            requests:
              cpu: "10m"
              memory: "16Mi"
            limits:
              cpu: "50m"
              memory: "32Mi"
      containers:
        - name: product-api
          image: localhost:5000/product-api:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080

          envFrom:
            - configMapRef:
                name: product-api-config
            - secretRef:
                name: product-api-secret

          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"

          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            failureThreshold: 12
            periodSeconds: 5    # aguarda até 60s pelo startup da JVM

          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 0
            periodSeconds: 10
            failureThreshold: 3

          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 0
            periodSeconds: 5
            failureThreshold: 3
```

---

## 6. Services

### ClusterIP — PostgreSQL (interno)

```yaml
# k8s/services/postgresql-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: postgresql
  namespace: learning-lab
spec:
  selector:
    app: postgresql
  ports:
    - port: 5432
      targetPort: 5432
  type: ClusterIP
```

### ClusterIP — MongoDB (interno)

```yaml
# k8s/services/mongodb-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: mongodb
  namespace: learning-lab
spec:
  selector:
    app: mongodb
  ports:
    - port: 27017
      targetPort: 27017
  type: ClusterIP
```

### NodePort — Product API (acesso externo direto)

Usado nas fases iniciais, antes do Ingress.

```yaml
# k8s/services/product-api-nodeport.yaml
apiVersion: v1
kind: Service
metadata:
  name: product-api-nodeport
  namespace: learning-lab
spec:
  selector:
    app: product-api
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080
  type: NodePort
```

Acesso: `http://$(minikube ip):30080`

### ClusterIP — Product API (usado pelo Ingress)

```yaml
# k8s/services/product-api-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: product-api
  namespace: learning-lab
spec:
  selector:
    app: product-api
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

---

## 7. Ingress

Requer addon `ingress` habilitado: `minikube addons enable ingress`

```yaml
# k8s/ingress/product-api-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: product-api-ingress
  namespace: learning-lab
spec:
  ingressClassName: nginx
  rules:
    - host: product-api.local
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: product-api
                port:
                  number: 80
```

> Não use `nginx.ingress.kubernetes.io/rewrite-target: /` com `path: /`: toda requisição seria reescrita para `/` (`/produtos` viraria `/`). Como a API já atende em `/produtos` e `/logs`, nenhuma reescrita é necessária.

Adicionar ao `/etc/hosts`:
```
$(minikube ip)  product-api.local
```

---

## 8. Fluxo de Rede

```
Externo
  │
  ▼
[Ingress: product-api.local]
  │
  ▼
[Service ClusterIP: product-api:80]
  │
  ▼
[Pod: product-api:8080]
  │
  ├──► [Service ClusterIP: postgresql:5432]  ──► [Pod: postgresql-0:5432]
  │
  └──► [Service ClusterIP: mongodb:27017]    ──► [Pod: mongodb-0:27017]
```

---

## 9. Labels e Convenções

| Label | Valores usados | Propósito |
|-------|---------------|-----------|
| `app` | `product-api`, `postgresql`, `mongodb` | Selector de Service |
| `version` | `1.0.0`, `1.0.0-broken` | Rastreabilidade de versão |
| `env` | `study` | Identificação do ambiente |

---

## 10. Ordem de Aplicação dos Manifests

```bash
# 1. Namespaces
kubectl apply -f k8s/namespace/

# 2. Secrets e ConfigMaps
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/configmaps/

# 3. Persistência (PostgreSQL + MongoDB)
kubectl apply -f k8s/postgresql/
kubectl apply -f k8s/mongodb/

# 4. Aplicação
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/services/
kubectl apply -f k8s/ingress/
```

---

## 11. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Ordem de startup | Se a API subir antes do PostgreSQL, o Flyway falha no startup e o container entra em `CrashLoopBackOff` (o Hikari não retenta). O `initContainer` `wait-for-postgres` evita isso; o MongoDB não bloqueia o startup (o log é assíncrono e tolerante a falhas) |
| Secrets em Base64 | Não é criptografia — adequado apenas para estudo; em produção usar Sealed Secrets |
| NodePort fixo `30080` | Pode conflitar se outra aplicação usar a mesma porta no Minikube — ajustar se necessário |
| `/etc/hosts` manual | Necessário para resolver `product-api.local` localmente |


---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [ ] Artefatos implementados conforme este documento (divergências registradas aqui)
- [ ] Manifests aplicados na ordem da seção 10 sem erro (`kubectl apply --dry-run=server` antes)
- [ ] API responde via NodePort e via Ingress (`product-api.local`)
- [ ] Cenários 1, 11, 12 e 17 do PRD executados
- [ ] Arquivo `TDD-003-infraestrutura-kubernetes_explain.md` gerado
- [ ] Estórias vinculadas atualizadas (US-010)
