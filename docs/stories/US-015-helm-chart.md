# US-015 — Empacotar a plataforma em Helm Chart

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Draft
**PRD:** [Fase 12 — Helm](../PRD.md#fase-12--helm)
**TDD:** [TDD-008](../tdd/TDD-008-helm.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** instalar, atualizar e remover a plataforma completa com Helm,
**para que** que o deploy seja reproduzível e parametrizável por ambiente.

---

## Critérios de Aceite

- [ ] `helm lint` e `helm template` executam sem erros
- [ ] `helm install` sobe a plataforma completa com um único comando
- [ ] Valores (imagem, réplicas, recursos) parametrizáveis via `values.yaml` e `--set`
- [ ] `helm upgrade`, `helm rollback` e `helm uninstall` funcionam; PVCs removidos manualmente


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-008 com status `Done`
