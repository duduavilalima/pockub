# ADR-007 — Prometheus e Grafana para Observabilidade

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

O projeto precisa de uma stack de observabilidade para demonstrar métricas e dashboards em K8s. As opções avaliadas foram:

| Opção | Prós | Contras |
|-------|------|---------|
| **Prometheus + Grafana** | Padrão de mercado, pull-based, integração nativa com Spring Actuator | Configuração inicial mais verbosa |
| Datadog / New Relic | Simples de configurar | Custo, dependência de conta externa |
| ELK Stack | Logs + métricas + traces | Pesado demais para Minikube |
| Apenas kubectl top | Zero configuração | Sem histórico, sem dashboard |

---

## Decisão

Usar **Prometheus** para coleta de métricas e **Grafana** para dashboards, ambos no namespace `monitoring`.

Justificativas:
- Stack open-source mais adotada em ambientes Kubernetes corporativos
- Spring Boot Actuator expõe `/actuator/prometheus` sem dependência adicional (exceto `micrometer-registry-prometheus`)
- Demonstra padrão pull-based de scraping — fundamental para entender observabilidade em K8s
- Grafana permite criar dashboards que tornam visíveis os efeitos do HPA, Rolling Update e health checks
- Reflete a realidade de times de plataforma que mantêm stack de monitoring separada da aplicação

---

## Consequências

- Prometheus precisa de ServiceAccount com permissão para service discovery (Pods) cross-namespace (ver [ADR-005](ADR-005-dual-namespace.md))
- Requer `scrape_config` com `kubernetes_sd_configs` (role `pod`) em `learning-lab`, filtrando Pods pela annotation `prometheus.io/scrape` — assim cada réplica da API é um target separado
- Grafana precisa ser configurado com Prometheus como datasource
- Recursos adicionais consumidos no Minikube — pode exigir aumento de memória do cluster (`minikube start --memory=6144 --cpus=4`)
- Dados do Prometheus são efêmeros por padrão — sem PVC, métricas são perdidas ao reiniciar o Pod
