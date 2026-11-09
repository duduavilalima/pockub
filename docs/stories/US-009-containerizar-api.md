# US-009 — Containerizar a Product API

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** Draft
**PRD:** [Fase 2 — Aplicação](../PRD.md#fase-2--aplicação) · [Cenário 1 — Deploy Completo da Plataforma](../PRD.md#cenário-1--deploy-completo-da-plataforma)
**TDD:** [TDD-002](../tdd/TDD-002-containerizacao.md)

---

## Estória

**Como** desenvolvedor,
**quero** empacotar a Product API numa imagem Docker enxuta e versionada,
**para que** que o cluster consiga executar a aplicação de forma reproduzível.

---

## Critérios de Aceite

- [ ] `docker build` multi-stage conclui sem erro a partir de `application/product-api/`
- [ ] Imagem final contém apenas JRE e o JAR, e executa com usuário não-root
- [ ] Imagem versionada com tag explícita (`1.0.0`); `latest` não é usada nos manifests
- [ ] Imagem acessível pelo cluster Minikube com `imagePullPolicy: IfNotPresent`


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-002 com status `Done`
- [ ] Cenário 1 do PRD executado(s)
