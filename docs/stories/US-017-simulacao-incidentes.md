# US-017 — Simular incidentes e praticar troubleshooting

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Ready
**PRD:** [Fase 13 — Simulação de Incidentes](../PRD.md#fase-13--simulação-de-incidentes) · [Cenário 6 — Recuperação Automática de Pod](../PRD.md#cenário-6--recuperação-automática-de-pod) · [Cenário 8 — Horizontal Pod Autoscaler](../PRD.md#cenário-8--horizontal-pod-autoscaler) · [Cenário 9 — Rolling Update](../PRD.md#cenário-9--rolling-update) · [Cenário 10 — Rollback](../PRD.md#cenário-10--rollback) · [Cenário 18 — NetworkPolicy](../PRD.md#cenário-18--networkpolicy)
**TDD:** [TDD-010](../tdd/TDD-010-simulacao-incidentes.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** simular falhas, deploy defeituoso, sobrecarga e violação de NetworkPolicy,
**para que** que eu pratique diagnóstico e recuperação como em um ambiente corporativo.

---

## Critérios de Aceite

- [ ] Falha de Pod simulada e auto-recuperação observada
- [ ] Deploy com falha intencional revertido com `kubectl rollout undo`
- [ ] Sobrecarga simulada e reação do HPA observada
- [ ] Violação de NetworkPolicy simulada e diagnosticada com `kubectl describe`, `kubectl logs` e `kubectl events`
- [ ] Perda de requisições no Rolling Update reproduzida e eliminada com `preStop` (0 falhas no loop de `curl`)


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-010 com status `Done`
- [ ] Cenário 6, Cenário 8, Cenário 9, Cenário 10, Cenário 18 do PRD executado(s)
