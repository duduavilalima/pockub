# TDD-007 — Observabilidade

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [ADR-007](../adr/ADR-007-prometheus-grafana.md) · [ADR-005](../adr/ADR-005-dual-namespace.md) · [TDD-001](TDD-001-product-api.md)

---

## 1. Visão Geral

Define a stack de observabilidade: configuração do Spring Actuator na Product API, deploy do Prometheus e Grafana no namespace `monitoring`, e integração cross-namespace para scraping de métricas.

---

## 2. Spring Boot Actuator

### 2.1 Dependências (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2.2 Configuração (application.yaml)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always
  metrics:
    tags:
      application: product-api
      namespace: learning-lab
```

### 2.3 Endpoints Disponíveis

| Endpoint | Propósito |
|----------|-----------|
| `/actuator/health` | Estado geral (inclui DB checks) |
| `/actuator/health/liveness` | livenessProbe do K8s |
| `/actuator/health/readiness` | readinessProbe do K8s |
| `/actuator/prometheus` | Métricas no formato Prometheus |
| `/actuator/metrics` | Catálogo de métricas disponíveis |
| `/actuator/info` | Versão e informações da build |

### 2.4 Métricas Expostas

| Métrica | Descrição |
|---------|-----------|
| `http_server_requests_seconds` | Latência e contagem de requisições HTTP |
| `jvm_memory_used_bytes` | Uso de memória da JVM |
| `jvm_gc_pause_seconds` | Pausas do Garbage Collector |
| `jvm_threads_live_threads` | Threads ativas |
| `process_cpu_usage` | CPU consumida pelo processo |
| `hikaricp_connections_active` | Conexões ativas no pool JDBC |
| `spring_data_repository_invocations` | Chamadas aos Repositories |

---

## 3. Prometheus

### 3.1 ServiceAccount e RBAC

Prometheus precisa de permissão para descobrir serviços e endpoints em outros namespaces.

```yaml
# monitoring/prometheus/prometheus-rbac.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: prometheus
  namespace: monitoring
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: prometheus
rules:
  - apiGroups: [""]
    resources: ["nodes", "pods", "services", "endpoints"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["configmaps"]
    verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: prometheus
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: prometheus
subjects:
  - kind: ServiceAccount
    name: prometheus
    namespace: monitoring
```

### 3.2 ConfigMap — prometheus.yml

```yaml
# monitoring/prometheus/prometheus-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    scrape_configs:
      - job_name: 'product-api'
        metrics_path: '/actuator/prometheus'
        static_configs:
          - targets: ['product-api.learning-lab.svc.cluster.local:80']
        relabel_configs:
          - source_labels: [__address__]
            target_label: instance

      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
          - role: pod
            namespaces:
              names: ['learning-lab']
        relabel_configs:
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: true
```

### 3.3 Deployment

```yaml
# monitoring/prometheus/prometheus-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      serviceAccountName: prometheus
      containers:
        - name: prometheus
          image: prom/prometheus:v2.51.0
          args:
            - "--config.file=/etc/prometheus/prometheus.yml"
            - "--storage.tsdb.retention.time=7d"
          ports:
            - containerPort: 9090
          resources:
            requests:
              cpu: "250m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          volumeMounts:
            - name: config
              mountPath: /etc/prometheus
      volumes:
        - name: config
          configMap:
            name: prometheus-config
---
apiVersion: v1
kind: Service
metadata:
  name: prometheus
  namespace: monitoring
spec:
  selector:
    app: prometheus
  ports:
    - port: 9090
      targetPort: 9090
  type: NodePort
```

---

## 4. Grafana

### 4.1 ConfigMap — Datasource

```yaml
# monitoring/grafana/grafana-datasource.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-datasources
  namespace: monitoring
data:
  datasources.yaml: |
    apiVersion: 1
    datasources:
      - name: Prometheus
        type: prometheus
        url: http://prometheus:9090
        access: proxy
        isDefault: true
```

### 4.2 Deployment

```yaml
# monitoring/grafana/grafana-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grafana
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: grafana
  template:
    metadata:
      labels:
        app: grafana
    spec:
      containers:
        - name: grafana
          image: grafana/grafana:10.4.0
          ports:
            - containerPort: 3000
          env:
            - name: GF_SECURITY_ADMIN_PASSWORD
              value: "admin"          # apenas para estudo
            - name: GF_USERS_ALLOW_SIGN_UP
              value: "false"
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
            limits:
              cpu: "200m"
              memory: "256Mi"
          volumeMounts:
            - name: datasources
              mountPath: /etc/grafana/provisioning/datasources
      volumes:
        - name: datasources
          configMap:
            name: grafana-datasources
---
apiVersion: v1
kind: Service
metadata:
  name: grafana
  namespace: monitoring
spec:
  selector:
    app: grafana
  ports:
    - port: 3000
      targetPort: 3000
      nodePort: 30300
  type: NodePort
```

---

## 5. Acesso à Stack

```bash
# URL do Prometheus
minikube service prometheus -n monitoring --url
# ou: http://$(minikube ip):30090 (se NodePort configurado)

# URL do Grafana
minikube service grafana -n monitoring --url
# ou: http://$(minikube ip):30300

# Credenciais Grafana: admin / admin
```

---

## 6. Dashboards Sugeridos

### 6.1 Importar via ID (Grafana.com)

| Dashboard | ID | Conteúdo |
|-----------|-----|---------|
| JVM Micrometer | `4701` | Heap, GC, Threads, CPU |
| Spring Boot Statistics | `6756` | HTTP requests, error rate |
| Kubernetes Cluster | `6417` | CPU/Memória dos nodes e pods |

### 6.2 Queries PromQL Úteis

```promql
# Taxa de requisições HTTP por endpoint (últimos 5min)
rate(http_server_requests_seconds_count{application="product-api"}[5m])

# Latência p99 por endpoint
histogram_quantile(0.99,
  rate(http_server_requests_seconds_bucket{application="product-api"}[5m])
)

# Uso de memória JVM
jvm_memory_used_bytes{application="product-api", area="heap"}

# Conexões ativas no pool JDBC
hikaricp_connections_active{application="product-api"}
```

---

## 7. Metrics Server (kubectl top)

```bash
# Habilitar
minikube addons enable metrics-server

# Verificar uso por Pod
kubectl top pods -n learning-lab

# Verificar uso por node
kubectl top nodes
```

O Metrics Server é a fonte de dados do HPA — sem ele o HPA fica em estado `unknown`.

---

## 8. Fluxo de Dados de Observabilidade

```
Product API
  /actuator/prometheus
        │
        │ scrape a cada 15s
        ▼
  [ Prometheus ]
  namespace: monitoring
        │
        │ datasource
        ▼
  [ Grafana ]
  namespace: monitoring
        │
        └──► Dashboards JVM + HTTP + K8s

  Metrics Server ──► kubectl top ──► HPA
```

---

## 9. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Prometheus sem PVC | Dados de métricas perdidos ao reiniciar o Pod — adequado para estudo, mas adicionar PVC para persistência em uso contínuo |
| RBAC cross-namespace | ClusterRole necessário — em produção restringir ao namespace específico |
| Grafana admin/admin | Senha padrão apenas para estudo — alterar via Secret em ambientes reais |
| Memória do Minikube | Prometheus + Grafana + aplicação somam ~1.5GB de requests — iniciar Minikube com `--memory=4096` |
| `retention.time=7d` | Retenção de 7 dias suficiente para estudo sem estourar disco |
