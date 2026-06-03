# TDD-003 — Infraestrutura Kubernetes Base

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [ADR-001](../adr/ADR-001-minikube-local-cluster.md) · [ADR-005](../adr/ADR-005-dual-namespace.md) · [TDD-004](TDD-004-persistencia.md) · [TDD-005](TDD-005-escalabilidade-confiabilidade.md)

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
  MONGO_HOST: "mongodb"
  MONGO_PORT: "27017"
  MONGO_DATABASE: "pockub"
  LOG_LEVEL: "INFO"
```

---

## 4. Secrets

### Credenciais PostgreSQL

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
  POSTGRES_DB: cG9ja3Vi            # base64: pockub
```

### Credenciais MongoDB

```yaml
# k8s/secrets/mongodb-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: mongodb-secret
  namespace: learning-lab
type: Opaque
data:
  MONGO_URI: bW9uZ29kYjovL21vbmdvZGI6MjcwMTcvcG9ja3Vi
  # base64: mongodb://mongodb:27017/pockub
```

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
    spec:
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
                name: postgresql-secret
            - secretRef:
                name: mongodb-secret

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
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
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
kubectl apply -f k8s/postgres/
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
| Ordem de startup | API sobe antes dos bancos — `readinessProbe` impede tráfego prematuro; Spring Boot retenta conexão via `spring.datasource.hikari.connection-timeout` |
| Secrets em Base64 | Não é criptografia — adequado apenas para estudo; em produção usar Sealed Secrets |
| NodePort fixo `30080` | Pode conflitar se outra aplicação usar a mesma porta no Minikube — ajustar se necessário |
| `/etc/hosts` manual | Necessário para resolver `product-api.local` localmente |
