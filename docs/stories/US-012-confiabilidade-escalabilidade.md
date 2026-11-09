# US-012 — Garantir confiabilidade e escalabilidade da API

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Draft
**PRD:** [Fase 6 — Confiabilidade](../PRD.md#fase-6--confiabilidade) · [Fase 7 — Escalabilidade](../PRD.md#fase-7--escalabilidade) · [Cenário 6 — Recuperação Automática de Pod](../PRD.md#cenário-6--recuperação-automática-de-pod) · [Cenário 7 — Escalabilidade Manual](../PRD.md#cenário-7--escalabilidade-manual) · [Cenário 8 — Horizontal Pod Autoscaler](../PRD.md#cenário-8--horizontal-pod-autoscaler) · [Cenário 9 — Rolling Update](../PRD.md#cenário-9--rolling-update) · [Cenário 10 — Rollback](../PRD.md#cenário-10--rollback)
**TDD:** [TDD-005](../tdd/TDD-005-escalabilidade-confiabilidade.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** configurar probes, requests/limits, HPA e estratégia de deploy,
**para que** que a API se recupere de falhas, escale com a carga e seja atualizada sem downtime.

---

## Critérios de Aceite

- [ ] Startup, liveness e readiness probes configuradas nos endpoints do Actuator
- [ ] Todos os containers com resource requests e limits
- [ ] `kubectl scale` altera o número de réplicas e o tráfego é balanceado entre elas
- [ ] HPA escala a API sob carga de CPU e reduz as réplicas após cessar a carga
- [ ] Rolling Update sem downtime e `kubectl rollout undo` restaura a versão anterior
- [ ] Pod da API excluído manualmente é recriado pelo Deployment


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-005 com status `Done`
- [ ] Cenário 6, Cenário 7, Cenário 8, Cenário 9, Cenário 10 do PRD executado(s)
