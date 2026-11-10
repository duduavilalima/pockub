# US-014 — Observar a plataforma com Prometheus e Grafana

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Done
**PRD:** [Fase 10 — Observabilidade Avançada](../PRD.md#fase-10--observabilidade-avançada) · [Cenário 13 — Observabilidade](../PRD.md#cenário-13--observabilidade)
**TDD:** [TDD-007](../tdd/TDD-007-observabilidade.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** coletar métricas da API e visualizá-las em dashboards no namespace `monitoring`,
**para que** que eu acompanhe o comportamento da aplicação e dos efeitos de HPA e deploys.

---

## Critérios de Aceite

- [x] Prometheus e Grafana em `Running` no namespace `monitoring`
- [x] Target `product-api` com status `UP` no Prometheus (um target por réplica)
- [x] Grafana com Prometheus como datasource e dashboards de JVM e HTTP exibindo dados
- [x] `kubectl top pods` e `kubectl top nodes` funcionam com o Metrics Server


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [x] Todos os critérios de aceite validados e marcados `[x]`
- [x] TDD-007 com status `Done`
- [x] Cenário 13 do PRD executado(s)
