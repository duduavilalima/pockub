# TDD-005 — Escalabilidade e Confiabilidade

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [TDD-003](TDD-003-infraestrutura-kubernetes.md) · [ADR-001](../adr/ADR-001-minikube-local-cluster.md)

---

## 1. Visão Geral

Define as estratégias de Health Checks (Probes), Resource Requests/Limits, Horizontal Pod Autoscaler (HPA) e estratégias de deploy (Rolling Update/Rollback) para a Product API.

---

## 2. Health Checks (Probes)

Três probes são configurados no container da Product API, mapeados para os endpoints do Spring Actuator.

### 2.1 Startup Probe

Aguarda o startup completo da JVM antes de ativar Liveness e Readiness.

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  failureThreshold: 12
  periodSeconds: 5
```

Tolerância máxima: `12 × 5s = 60s`. Se o container não responder em 60s, é reiniciado.

### 2.2 Liveness Probe

Detecta containers travados. Se falhar, o Pod é reiniciado.

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  periodSeconds: 10
  failureThreshold: 3
```

### 2.3 Readiness Probe

Controla se o Pod recebe tráfego do Service. Se falhar, o Pod é removido do endpoint do Service sem ser reiniciado.

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  periodSeconds: 5
  failureThreshold: 3
```

### 2.4 Comportamento Combinado

```
Container inicia
    │
    ▼
[startupProbe] → falha por até 60s → reinicia
    │ sucesso
    ▼
[livenessProbe]  → falha 3x seguidas → reinicia
[readinessProbe] → falha 3x seguidas → remove do Service (não reinicia)
```

---

## 3. Resource Requests e Limits

### 3.1 Product API

```yaml
resources:
  requests:
    cpu: "250m"       # 0.25 vCPU garantido para scheduling
    memory: "256Mi"
  limits:
    cpu: "500m"       # teto de 0.5 vCPU
    memory: "512Mi"
```

### 3.2 PostgreSQL / MongoDB

```yaml
resources:
  requests:
    cpu: "250m"
    memory: "256Mi"
  limits:
    cpu: "500m"
    memory: "512Mi"
```

### 3.3 Por que requests são obrigatórios para o HPA

O HPA calcula o uso de CPU como percentual de `resources.requests.cpu`. Sem `requests` definido, o HPA não consegue calcular a métrica e permanece em estado `unknown`.

```
uso_atual (milicores)
─────────────────────  × 100 = % CPU usado
requests.cpu (250m)
```

---

## 4. Horizontal Pod Autoscaler (HPA)

### 4.1 Pré-requisitos

```bash
# Metrics Server precisa estar habilitado
minikube addons enable metrics-server

# Verificar
kubectl top pods -n learning-lab
```

### 4.2 Manifest

```yaml
# k8s/hpa/product-api-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: product-api-hpa
  namespace: learning-lab
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product-api
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 50
```

### 4.3 Comportamento

| CPU média | Réplicas |
|-----------|---------|
| < 50% | Reduz (mín: 1) |
| = 50% | Mantém |
| > 50% | Aumenta (máx: 5) |

Scale-down tem `stabilizationWindowSeconds: 300` por padrão — evita flapping.

### 4.4 Geração de Carga (Cenário 8)

```bash
# Gerar carga contínua
kubectl run load-generator \
  --image=busybox:1.36 \
  --restart=Never \
  -it \
  -- /bin/sh -c "while true; do wget -q -O- http://product-api/produtos; done"

# Monitorar HPA em tempo real
kubectl get hpa -n learning-lab -w

# Parar geração de carga
kubectl delete pod load-generator
```

---

## 5. Estratégia de Deploy

### 5.1 Rolling Update (padrão)

```yaml
# Trecho do Deployment
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1        # até 1 Pod extra durante o update
    maxUnavailable: 0  # nenhum Pod indisponível (zero downtime)
```

**Fluxo:**
```
Réplicas: [v1.0.0] [v1.0.0]

1. Cria novo Pod: [v1.0.0] [v1.0.0] [v1.1.0]
2. Aguarda readiness do novo Pod
3. Remove um Pod antigo: [v1.0.0] [v1.1.0]
4. Repete até todos atualizados: [v1.1.0] [v1.1.0]
```

### 5.2 Executar Rolling Update (Cenário 9)

```bash
# Atualizar imagem
kubectl set image deployment/product-api \
  product-api=localhost:5000/product-api:1.1.0 \
  -n learning-lab

# Monitorar
kubectl rollout status deployment/product-api -n learning-lab

# Verificar histórico
kubectl rollout history deployment/product-api -n learning-lab
```

### 5.3 Rollback (Cenário 10)

```bash
# Deploy de versão defeituosa
kubectl set image deployment/product-api \
  product-api=localhost:5000/product-api:1.0.0-broken \
  -n learning-lab

# Rollback para versão anterior
kubectl rollout undo deployment/product-api -n learning-lab

# Rollback para versão específica
kubectl rollout undo deployment/product-api \
  --to-revision=1 \
  -n learning-lab
```

---

## 6. Escalabilidade Manual (Cenário 7)

```bash
# Escalar para 3 réplicas
kubectl scale deployment product-api --replicas=3 -n learning-lab

# Verificar
kubectl get pods -n learning-lab -l app=product-api

# Reduzir
kubectl scale deployment product-api --replicas=1 -n learning-lab
```

---

## 7. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| `maxUnavailable: 0` com 1 réplica | Durante o update, o cluster precisa suportar 2 Pods temporariamente — garantir recursos no node |
| HPA + JVM | A JVM aquece com o tempo (JIT) — métricas de CPU podem ser altas no início e baixar depois; aguardar estabilização antes de avaliar |
| `stabilizationWindowSeconds` | Default 300s para scale-down — nos estudos pode ser reduzido para `60s` para ver o efeito mais rápido |
| Requests muito baixos | `requests.cpu: 250m` é adequado para estudo; se a JVM pressionar mais, aumentar para evitar `OOMKilled` |
