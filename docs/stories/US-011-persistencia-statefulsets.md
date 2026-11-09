# US-011 — Persistir dados com StatefulSets

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Ready
**PRD:** [Fase 4 — Persistência](../PRD.md#fase-4--persistência) · [Cenário 4 — Persistência PostgreSQL](../PRD.md#cenário-4--persistência-postgresql) · [Cenário 5 — Persistência MongoDB](../PRD.md#cenário-5--persistência-mongodb)
**TDD:** [TDD-004](../tdd/TDD-004-persistencia.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** executar PostgreSQL e MongoDB como StatefulSets com PV/PVC,
**para que** que os dados sobrevivam à recriação dos Pods.

---

## Critérios de Aceite

- [ ] StatefulSets `postgresql` e `mongodb` com Pod `Running` e PVC `Bound`
- [ ] Produto criado via API continua disponível após `kubectl delete pod postgresql-0`
- [ ] Logs de acesso continuam disponíveis após `kubectl delete pod mongodb-0`
- [ ] Pod recriado mantém o mesmo nome e reutiliza o mesmo PVC
- [ ] API conecta nos bancos via Services ClusterIP (`postgresql:5432`, `mongodb:27017`)


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-004 com status `Done`
- [ ] Cenário 4, Cenário 5 do PRD executado(s)
