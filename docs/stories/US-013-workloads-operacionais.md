# US-013 — Executar workloads operacionais (Job, CronJob, DaemonSet)

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Ready
**PRD:** [Fase 9 — Operação](../PRD.md#fase-9--operação) · [Cenário 14 — Job (Seed de Dados)](../PRD.md#cenário-14--job-seed-de-dados) · [Cenário 15 — CronJob (Limpeza de Logs)](../PRD.md#cenário-15--cronjob-limpeza-de-logs) · [Cenário 16 — DaemonSet (Log Collector)](../PRD.md#cenário-16--daemonset-log-collector)
**TDD:** [TDD-006](../tdd/TDD-006-workloads-especiais.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** usar Job, CronJob e DaemonSet para seed, limpeza e agente por node,
**para que** que eu compreenda os workloads de ciclo de vida diferente de Deployment.

---

## Critérios de Aceite

- [ ] Job `seed-data` termina com status `Complete` e os produtos aparecem em `GET /produtos`
- [ ] CronJob `log-cleanup` agendado para `0 2 * * *` e disparável manualmente
- [ ] Execução do CronJob remove logs com mais de 30 dias do MongoDB
- [ ] DaemonSet `log-collector` com 1 Pod por node do cluster


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-006 com status `Done`
- [ ] Cenário 14, Cenário 15, Cenário 16 do PRD executado(s)
