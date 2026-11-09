# Technical Design Documents

> Detalhamento técnico de cada camada do [Kubernetes Learning Lab](../PRD.md)

| TDD | Título | Escopo | Estórias | Status |
|-----|--------|--------|----------|--------|
| [TDD-001](TDD-001-product-api.md)                   | Product API                     | Pacotes, domínio, serviços, controllers, DTOs, Actuator | [US-001](../stories/US-001-criar-produto.md), [US-002](../stories/US-002-atualizar-produto.md), [US-003](../stories/US-003-excluir-produto.md), [US-004](../stories/US-004-consultar-produto.md), [US-005](../stories/US-005-listar-produtos.md), [US-006](../stories/US-006-registrar-log-consulta.md), [US-007](../stories/US-007-consultar-logs.md), [US-008](../stories/US-008-filtrar-logs-produto.md) | In Progress |
| [TDD-002](TDD-002-containerizacao.md)               | Containerização                 | Dockerfile multi-stage, build no daemon do Minikube, tags | [US-009](../stories/US-009-containerizar-api.md) | Draft |
| [TDD-003](TDD-003-infraestrutura-kubernetes.md)     | Infraestrutura Kubernetes       | Namespaces, Deployment, Services, Ingress, ConfigMaps, Secrets | [US-010](../stories/US-010-infraestrutura-kubernetes-base.md) | Draft |
| [TDD-004](TDD-004-persistencia.md)                  | Persistência                    | PostgreSQL StatefulSet, MongoDB StatefulSet, PV/PVC | [US-011](../stories/US-011-persistencia-statefulsets.md) | Draft |
| [TDD-005](TDD-005-escalabilidade-confiabilidade.md) | Escalabilidade e Confiabilidade | Probes, Resource Limits, HPA, Rolling Update, Rollback | [US-012](../stories/US-012-confiabilidade-escalabilidade.md) | Draft |
| [TDD-006](TDD-006-workloads-especiais.md)           | Workloads Especiais             | Job (seed), CronJob (limpeza), DaemonSet (log collector) | [US-013](../stories/US-013-workloads-operacionais.md) | Draft |
| [TDD-007](TDD-007-observabilidade.md)               | Observabilidade                 | Actuator, Prometheus, Grafana, Metrics Server | [US-014](../stories/US-014-observabilidade.md) | Draft |
| [TDD-008](TDD-008-helm.md)                          | Helm Chart                      | Chart structure, values.yaml, templates, ciclo de release | [US-015](../stories/US-015-helm-chart.md) | Draft |
| [TDD-009](TDD-009-networkpolicy.md) | NetworkPolicy | Isolamento de PostgreSQL e MongoDB, CNI Calico | [US-016](../stories/US-016-networkpolicy.md) | Draft |
| [TDD-010](TDD-010-simulacao-incidentes.md) | Simulação de Incidentes | Runbook de falhas, rollback, troubleshooting | [US-017](../stories/US-017-simulacao-incidentes.md) | Draft |

---

## Dependências entre TDDs

```
TDD-001 (API)
    └──► TDD-002 (Dockerfile)
             └──► TDD-003 (Infra K8s base)
                      ├──► TDD-004 (Persistência)
                      ├──► TDD-005 (Escalabilidade)
                      ├──► TDD-006 (Workloads especiais)
                      ├──► TDD-007 (Observabilidade)
                      └──► TDD-009 (NetworkPolicy; libera o CronJob do TDD-006)
                               └──► TDD-008 (Helm — empacota tudo)

TDD-010 (Incidentes) — runbook sobre a plataforma completa (TDD-003 a TDD-009)
```
