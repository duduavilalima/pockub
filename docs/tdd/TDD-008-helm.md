# TDD-008 — Helm Chart

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [STACK.md](../STACK.md) · [TDD-003](TDD-003-infraestrutura-kubernetes.md) · [TDD-004](TDD-004-persistencia.md) · [TDD-007](TDD-007-observabilidade.md)
**Estórias:** [US-015](../stories/US-015-helm-chart.md)
**PRD:** [Fase 12 — Helm](../PRD.md#fase-12--helm)

---

## 1. Visão Geral

Empacota todos os manifests Kubernetes da plataforma em um Helm Chart, permitindo instalação, atualização e remoção com um único comando. Representa o padrão de distribuição de aplicações em ambientes Kubernetes corporativos.

---

## 2. Estrutura do Chart

```
helm/kubernetes-learning-lab/
├── Chart.yaml                   ← metadados do chart
├── values.yaml                  ← valores padrão parametrizáveis
└── templates/
    ├── _helpers.tpl             ← funções auxiliares (nomes, labels)
    ├── secret.yaml              ← product-api-secret + postgresql-secret
    ├── configmap.yaml
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    ├── hpa.yaml
    ├── postgresql/
    │   ├── statefulset.yaml
    │   └── service.yaml
    ├── mongodb/
    │   ├── statefulset.yaml
    │   └── service.yaml
    ├── jobs/
    │   └── seed-data.yaml
    ├── cronjobs/
    │   └── log-cleanup.yaml
    ├── daemonset.yaml
    └── monitoring/
        ├── namespace.yaml       ← somente o namespace `monitoring` (o `learning-lab` é criado pelo Helm)
        ├── prometheus-deployment.yaml
        ├── prometheus-configmap.yaml
        ├── prometheus-rbac.yaml
        ├── grafana-deployment.yaml
        └── grafana-configmap.yaml
```

---

## 3. Chart.yaml

```yaml
# helm/kubernetes-learning-lab/Chart.yaml
apiVersion: v2
name: kubernetes-learning-lab
description: POC de estudo Kubernetes — Product API com PostgreSQL e MongoDB
type: application
version: 1.0.0
appVersion: "1.0.0"
keywords:
  - kubernetes
  - study
  - spring-boot
maintainers:
  - name: Equipe de Estudos Kubernetes
```

---

## 4. values.yaml

```yaml
# helm/kubernetes-learning-lab/values.yaml

# O namespace da aplicação NÃO é um value: vem de `helm install --namespace learning-lab --create-namespace`
# e é referenciado nos templates como {{ .Release.Namespace }}.

# Product API
productApi:
  image:
    repository: product-api
    tag: "1.0.0"
    pullPolicy: IfNotPresent
  replicas: 1
  resources:
    requests:
      cpu: "250m"
      memory: "256Mi"
    limits:
      cpu: "500m"
      memory: "512Mi"
  service:
    type: ClusterIP
    port: 80
    targetPort: 8080
  nodePort: 30080   # usado quando service.type=NodePort (Fases 3–7); com Ingress o padrão é ClusterIP

# Ingress
ingress:
  enabled: true
  className: nginx
  host: product-api.local

# PostgreSQL
postgresql:
  image:
    repository: postgres
    tag: "17-alpine"
  resources:
    requests:
      cpu: "250m"
      memory: "256Mi"
    limits:
      cpu: "500m"
      memory: "512Mi"
  storage:
    size: 1Gi
    storageClassName: standard
  credentials:
    database: pockub
    username: pockub
    password: pockub123  # apenas para estudo — usar Secret externo em produção
    # o template secret.yaml gera `postgresql-secret` (POSTGRES_USER/PASSWORD)
    # e `product-api-secret` (DB_USERNAME/DB_PASSWORD) a partir destes valores

# MongoDB
mongodb:
  image:
    repository: mongo
    tag: "7"
  resources:
    requests:
      cpu: "250m"
      memory: "256Mi"
    limits:
      cpu: "500m"
      memory: "512Mi"
  storage:
    size: 1Gi
    storageClassName: standard

# HPA
hpa:
  enabled: true
  minReplicas: 1
  maxReplicas: 5
  targetCPUUtilizationPercentage: 50

# Job de seed
seedJob:
  enabled: true

# CronJob de limpeza
logCleanup:
  enabled: true
  schedule: "0 2 * * *"
  retentionDays: 30

# DaemonSet
logCollector:
  enabled: true

# Observabilidade
monitoring:
  enabled: true
  namespace: monitoring
  prometheus:
    image:
      repository: prom/prometheus
      tag: "v2.51.0"
    retention: "7d"
    nodePort: 30090
  grafana:
    image:
      repository: grafana/grafana
      tag: "10.4.0"
    adminPassword: admin
    nodePort: 30300
```

---

## 5. _helpers.tpl

```gotemplate
{{/*
Nome completo do chart
*/}}
{{- define "learning-lab.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Labels comuns aplicados a todos os recursos
*/}}
{{- define "learning-lab.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Selector labels para Deployments e Services
*/}}
{{- define "learning-lab.selectorLabels" -}}
app: {{ .name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
```

---

## 6. Exemplo de Template Parametrizado

### deployment.yaml

```gotemplate
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-api
  namespace: {{ .Release.Namespace }}
  labels:
    {{- include "learning-lab.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.productApi.replicas }}
  selector:
    matchLabels:
      app: product-api
  template:
    metadata:
      labels:
        app: product-api
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      containers:
        - name: product-api
          image: "{{ .Values.productApi.image.repository }}:{{ .Values.productApi.image.tag }}"
          imagePullPolicy: {{ .Values.productApi.image.pullPolicy }}
          ports:
            - containerPort: {{ .Values.productApi.service.targetPort }}
          resources:
            {{- toYaml .Values.productApi.resources | nindent 12 }}
```

---

## 7. Comandos de Uso

### Instalação

```bash
# Instalar a plataforma completa
helm install learning-lab helm/kubernetes-learning-lab/ \
  --namespace learning-lab \
  --create-namespace

# Com valores customizados
helm install learning-lab helm/kubernetes-learning-lab/ \
  --namespace learning-lab --create-namespace \
  --set productApi.replicas=2 \
  --set hpa.enabled=false
```

### Atualização

```bash
# Atualizar versão da imagem
helm upgrade learning-lab helm/kubernetes-learning-lab/ \
  --namespace learning-lab \
  --set productApi.image.tag=1.1.0

# Ver histórico de releases
helm history learning-lab -n learning-lab
```

### Rollback

```bash
# Rollback para revisão anterior
helm rollback learning-lab 1 -n learning-lab

# Ver revisões disponíveis
helm history learning-lab -n learning-lab
```

### Remoção

```bash
# Remover todos os recursos
helm uninstall learning-lab -n learning-lab

# PVCs precisam ser removidos manualmente (StatefulSet behavior).
# O seletor só funciona se o volumeClaimTemplate declarar o label (ver nota abaixo).
kubectl delete pvc -l app.kubernetes.io/instance=learning-lab -n learning-lab

# Alternativa sem depender de labels:
kubectl delete pvc postgres-data-postgresql-0 mongodb-data-mongodb-0 -n learning-lab
```

### Debugging

```bash
# Renderizar templates sem instalar
helm template learning-lab helm/kubernetes-learning-lab/

# Verificar manifests com valores específicos
helm template learning-lab helm/kubernetes-learning-lab/ \
  --set productApi.image.tag=1.1.0 | grep "image:"

# Lint do chart
helm lint helm/kubernetes-learning-lab/
```

---

## 8. Valores por Ambiente

Para demonstrar o padrão de multi-ambiente, criar arquivos de override:

```
helm/
└── kubernetes-learning-lab/
    ├── values.yaml            ← defaults
    ├── values-dev.yaml        ← overrides para desenvolvimento
    └── values-prod.yaml       ← overrides para produção (exemplo)
```

```yaml
# values-prod.yaml (exemplo)
productApi:
  replicas: 3
  image:
    pullPolicy: Always
hpa:
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
seedJob:
  enabled: false             # não executar seed em produção
```

```bash
helm install learning-lab helm/kubernetes-learning-lab/ \
  -f helm/kubernetes-learning-lab/values.yaml \
  -f helm/kubernetes-learning-lab/values-prod.yaml
```

---

## 9. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Secrets no values.yaml | Senhas em texto puro no values.yaml — usar `--set` via CI ou Sealed Secrets em produção |
| PVC não removido no uninstall | Comportamento esperado do StatefulSet — documentar para o operador |
| Labels nos PVCs | PVCs criados por `volumeClaimTemplates` não herdam os labels do chart. Declare `app.kubernetes.io/instance: {{ .Release.Name }}` em `volumeClaimTemplates[].metadata.labels` nos templates dos StatefulSets, senão o `kubectl delete pvc -l ...` não encontra nada |
| Namespace | `--create-namespace` cria o `learning-lab`; não há `namespace.yaml` para ele no chart (evita conflito de ownership do Helm). O namespace `monitoring` é criado por um template do chart e removido junto no `helm uninstall` |
| `helm rollback` vs `kubectl rollout undo` | `helm rollback` reverte todos os recursos do chart; `kubectl rollout undo` reverte apenas o Deployment |
| Chart version vs appVersion | Incrementar `version` em mudanças de chart; `appVersion` acompanha a versão da aplicação |


---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [ ] Artefatos implementados conforme este documento (divergências registradas aqui)
- [ ] `helm lint` e `helm template` sem erros
- [ ] `helm install` e `helm uninstall` da plataforma completa executados em cluster limpo
- [ ] Rollback com `helm rollback` validado
- [ ] Arquivo `TDD-008-helm_explain.md` gerado
- [ ] Estórias vinculadas atualizadas (US-015)
