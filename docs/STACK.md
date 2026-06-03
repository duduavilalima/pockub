# Stack Tecnológica

> Parte do [PRD — Kubernetes Learning Lab](PRD.md)

---

## Backend

| Tecnologia | Versão | Papel |
|-----------|--------|-------|
| Java | 21 | Linguagem da aplicação |
| Spring Boot | 4 | Framework principal |
| Spring Data JPA | — | Persistência relacional (PostgreSQL) |
| Spring Data MongoDB | — | Persistência NoSQL (MongoDB) |
| Spring Actuator | — | Health checks e métricas |
| Maven | — | Build e gerenciamento de dependências |

---

## Banco de Dados

| Tecnologia | Versão | Papel |
|-----------|--------|-------|
| PostgreSQL | 17 | Armazenamento relacional de Produtos |
| MongoDB | latest | Armazenamento NoSQL de Logs de Acesso |

---

## Containers e Orquestração

| Tecnologia | Versão | Papel |
|-----------|--------|-------|
| Docker | latest | Runtime de containers e build de imagens |
| Kubernetes | — | Orquestração de containers |
| Minikube | latest | Cluster Kubernetes local para estudo |
| kubectl | — | CLI de administração do cluster |

---

## Empacotamento e Distribuição

| Tecnologia | Papel |
|-----------|-------|
| Helm | Empacotamento dos manifests K8s em Chart para deploy reproduzível |

---

## Observabilidade

| Tecnologia | Papel |
|-----------|-------|
| Spring Actuator | Expose `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` |
| Metrics Server | Coleta de CPU/memória para HPA e `kubectl top` |
| Prometheus | Coleta e armazenamento de métricas (namespace `monitoring`) |
| Grafana | Dashboards de visualização (namespace `monitoring`) |

---

## Decisões de Arquitetura Relacionadas

* [ADR-001 — Minikube como cluster local](adr/ADR-001-minikube-local-cluster.md)
* [ADR-002 — Java 21 e Spring Boot 4](adr/ADR-002-java-spring-boot.md)
* [ADR-003 — PostgreSQL para persistência relacional](adr/ADR-003-postgresql-relacional.md)
* [ADR-004 — MongoDB para logs de acesso](adr/ADR-004-mongodb-nosql.md)
* [ADR-007 — Prometheus e Grafana para observabilidade](adr/ADR-007-prometheus-grafana.md)
