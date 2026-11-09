# US-016 — Isolar os bancos com NetworkPolicy

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Draft
**PRD:** [Fase 11 — Segurança de Rede](../PRD.md#fase-11--segurança-de-rede) · [Cenário 18 — NetworkPolicy](../PRD.md#cenário-18--networkpolicy)
**TDD:** [TDD-009](../tdd/TDD-009-networkpolicy.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** restringir o acesso a PostgreSQL e MongoDB por NetworkPolicy,
**para que** que apenas os componentes autorizados conectem nos bancos.

---

## Critérios de Aceite

- [ ] Cluster iniciado com CNI que aplica NetworkPolicy (`--cni=calico`)
- [ ] Product API conecta ao PostgreSQL e ao MongoDB
- [ ] CronJob `log-cleanup` conecta ao MongoDB (regra própria)
- [ ] Qualquer outro Pod é bloqueado nas portas 5432 e 27017


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-009 com status `Done`
- [ ] Cenário 18 do PRD executado(s)
