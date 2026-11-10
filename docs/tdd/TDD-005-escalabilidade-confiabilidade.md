# TDD-005 — Escalabilidade e Confiabilidade

**Status:** Done
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [TDD-003](TDD-003-infraestrutura-kubernetes.md) · [ADR-001](../adr/ADR-001-minikube-local-cluster.md)
**Estórias:** [US-012](../stories/US-012-confiabilidade-escalabilidade.md)
**PRD:** [Fase 6 — Confiabilidade](../PRD.md#fase-6--confiabilidade) · [Fase 7 — Escalabilidade](../PRD.md#fase-7--escalabilidade) · [Cenário 6 — Recuperação Automática de Pod](../PRD.md#cenário-6--recuperação-automática-de-pod) · [Cenário 7 — Escalabilidade Manual](../PRD.md#cenário-7--escalabilidade-manual) · [Cenário 8 — Horizontal Pod Autoscaler](../PRD.md#cenário-8--horizontal-pod-autoscaler) · [Cenário 9 — Rolling Update](../PRD.md#cenário-9--rolling-update) · [Cenário 10 — Rollback](../PRD.md#cenário-10--rollback)

---

## 1. Visão Geral

Define as estratégias de Health Checks (Probes), Resource Requests/Limits, Horizontal Pod Autoscaler (HPA) e estratégias de deploy (Rolling Update/Rollback) para a Product API.

---

## 2. Health Checks (Probes)

Os três probes (startup, liveness e readiness) do container da Product API, mapeados para os endpoints do Spring Actuator, estão definidos no Deployment da Product API ([TDD-003 §5](TDD-003-infraestrutura-kubernetes.md)). Aqui, o comportamento:

| Probe | Endpoint | Parâmetros | Efeito da falha |
|-------|----------|------------|-----------------|
| Startup | `/actuator/health/liveness` | `periodSeconds: 5`, `failureThreshold: 12` (tolerância de 60s) | Container reiniciado se não subir em 60s; enquanto roda, desativa liveness/readiness |
| Liveness | `/actuator/health/liveness` | `periodSeconds: 10`, `failureThreshold: 3` | Container reiniciado (detecta travamento) |
| Readiness | `/actuator/health/readiness` | `periodSeconds: 5`, `failureThreshold: 3` | Pod removido dos endpoints do Service, sem reiniciar |

### 2.1 Comportamento Combinado

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

### 3.1 Valores adotados

Os manifests estão em [TDD-003 §5](TDD-003-infraestrutura-kubernetes.md) (API) e [TDD-004](TDD-004-persistencia.md) (bancos). Valores por container:

| Container | requests (cpu / memória) | limits (cpu / memória) |
|-----------|--------------------------|------------------------|
| Product API, PostgreSQL, MongoDB | `250m` / `256Mi` | `500m` / `512Mi` |

`requests` é o que o scheduler reserva (0,25 vCPU garantido); `limits` é o teto aplicado em runtime (CPU é limitada; estourar memória causa `OOMKilled`).

### 3.2 Por que requests são obrigatórios para o HPA

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

A estratégia está no Deployment ([TDD-003 §5](TDD-003-infraestrutura-kubernetes.md)): `RollingUpdate` com `maxSurge: 1` (até 1 Pod extra durante o update) e `maxUnavailable: 0` (nenhum Pod indisponível — zero downtime).

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
  product-api=product-api:1.1.0 \
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
  product-api=product-api:1.0.0-broken \
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
| Rolling Update e requisições perdidas | Observadas 3 falhas em 290 no Cenário 9; a mitigação (`preStop`) foi aplicada no Deployment e validada (0 falhas) no [TDD-010 §2.1](TDD-010-simulacao-incidentes.md#21-incidente-8--rolling-update-com-perda-de-requisições) |
| Requests muito baixos | `requests.cpu: 250m` é adequado para estudo; se a JVM pressionar mais, aumentar para evitar `OOMKilled` |


---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [x] Artefatos implementados conforme este documento (divergências registradas aqui)
- [x] Probes, requests e limits aplicados e verificados com `kubectl describe pod`
- [x] HPA com métrica `cpu` válida (não `<unknown>`) em `kubectl get hpa`
- [x] Cenários 6 a 10 do PRD executados (incluindo Rolling Update sem downtime e Rollback)
- [x] Arquivo `TDD-005-escalabilidade-confiabilidade_explain.md` gerado
- [x] Estórias vinculadas atualizadas (US-012)
