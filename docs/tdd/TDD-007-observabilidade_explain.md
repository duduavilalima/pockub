# TDD-007 — Observabilidade: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

| Artefato | Recurso |
|----------|---------|
| `monitoring/prometheus/prometheus-rbac.yaml` | ServiceAccount + ClusterRole + ClusterRoleBinding `prometheus` |
| `monitoring/prometheus/prometheus-config.yaml` | ConfigMap com `prometheus.yml` (jobs `product-api` e `kubernetes-cadvisor`) |
| `monitoring/prometheus/prometheus-deployment.yaml` | Deployment + Service NodePort `30090` |
| `monitoring/grafana/grafana-datasource.yaml` | ConfigMap de provisioning do datasource Prometheus |
| `monitoring/grafana/grafana-secret.yaml` | Secret com a senha do admin |
| `monitoring/grafana/grafana-deployment.yaml` | Deployment + Service NodePort `30300` |
| `monitoring/grafana/dashboards/pods-cpu-memoria.json` | Painel próprio de CPU/memória por Pod |

Actuator, Micrometer, annotations `prometheus.io/*` e Metrics Server **já existiam** (TDD-001/003/005); aqui apenas foram verificados.

## 2. Por que cada decisão foi tomada

- **`kubernetes_sd_configs` com `role: pod` + annotations**: um target por réplica; quando o HPA escala, novos Pods aparecem sozinhos. Scrape via Service misturaria as réplicas.
- **ClusterRole**: o Prometheus está em `monitoring` mas descobre Pods em `learning-lab` — acesso cross-namespace exige permissão de cluster (ou Role no namespace alvo).
- **Job `kubernetes-cadvisor` via API server proxy**: dá CPU/memória por Pod sem instalar kube-state-metrics/node-exporter.
- **Datasource por provisioning (ConfigMap)**: Grafana já sobe configurado, sem cliques.
- **Senha em Secret**: segue o padrão do projeto (credenciais fora do manifest do Deployment).
- **NodePort**: acesso simples no Minikube sem Ingress adicional.

## 3. Alternativas descartadas

- **kube-prometheus-stack / Operator**: esconderia os componentes que o estudo quer mostrar.
- **PVC para Prometheus**: perde métricas ao reiniciar, aceitável em estudo.
- **Provisionar os dashboards por ConfigMap**: os dashboards da Grafana.com têm `${DS_PROMETHEUS}` que exige o fluxo de import; foi usada a API de import.

## 4. Pontos de atenção

- **Divergência (cAdvisor + Docker)**: com cri-dockerd, `container_cpu_usage_seconds_total` e `container_memory_working_set_bytes` só têm séries a nível de Pod (sem o label `container`). O filtro `container!=""` do TDD retornava 0 séries; usa-se `pod!=""`.
- Os dashboards 4701 e 6756 são importados da internet (Grafana.com) e **não** ficam versionados; o painel próprio fica em `monitoring/grafana/dashboards/`. Dashboards importados via API vivem no banco do Grafana (sem PVC): somem se o Pod reiniciar.
- O Grafana pode demorar ~30 s para ficar pronto; o target do Prometheus leva um ciclo de scrape (15 s) para aparecer `UP`.
- O dashboard 6756 usa métricas com nomes do Spring Boot 2 (`http_server_requests_seconds_*`); alguns painéis podem ficar vazios no Spring Boot 4.
- `kubectl top` depende do addon `metrics-server`.

## 5. Como validar

```bash
kubectl apply -R -f monitoring/
kubectl get pods -n monitoring

# targets
curl -s http://$(minikube ip):30090/api/v1/targets | grep -o '"health":"[a-z]*"'

# Grafana: http://$(minikube ip):30300  (admin / admin) -> Dashboards
curl -s -u admin:admin http://$(minikube ip):30300/api/search | head

kubectl top pods -n learning-lab
kubectl top nodes
```
