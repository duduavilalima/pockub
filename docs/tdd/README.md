# Technical Design Documents

> Detalhamento técnico de cada camada do [Kubernetes Learning Lab](../PRD.md)

| TDD                                                 | Título                          | Escopo                                                         |
|-----------------------------------------------------|---------------------------------|----------------------------------------------------------------|
| [TDD-001](TDD-001-product-api.md)                   | Product API                     | Pacotes, domínio, serviços, controllers, DTOs, Actuator        |
| [TDD-002](TDD-002-containerizacao.md)               | Containerização                 | Dockerfile multi-stage, registry Minikube, tags                |
| [TDD-003](TDD-003-infraestrutura-kubernetes.md)     | Infraestrutura Kubernetes       | Namespaces, Deployment, Services, Ingress, ConfigMaps, Secrets |
| [TDD-004](TDD-004-persistencia.md)                  | Persistência                    | PostgreSQL StatefulSet, MongoDB StatefulSet, PV/PVC            |
| [TDD-005](TDD-005-escalabilidade-confiabilidade.md) | Escalabilidade e Confiabilidade | Probes, Resource Limits, HPA, Rolling Update, Rollback         |
| [TDD-006](TDD-006-workloads-especiais.md)           | Workloads Especiais             | Job (seed), CronJob (limpeza), DaemonSet (log collector)       |
| [TDD-007](TDD-007-observabilidade.md)               | Observabilidade                 | Actuator, Prometheus, Grafana, Metrics Server                  |
| [TDD-008](TDD-008-helm.md)                          | Helm Chart                      | Chart structure, values.yaml, templates, ciclo de release      |

---

## Dependências entre TDDs

```
TDD-001 (API)
    └──► TDD-002 (Dockerfile)
             └──► TDD-003 (Infra K8s base)
                      ├──► TDD-004 (Persistência)
                      ├──► TDD-005 (Escalabilidade)
                      ├──► TDD-006 (Workloads especiais)
                      └──► TDD-007 (Observabilidade)
                               └──► TDD-008 (Helm — empacota tudo)
```
