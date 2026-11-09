# US-010 — Implantar a infraestrutura Kubernetes base

**Requisito:** não funcional — plataforma Kubernetes (ver PRD)
**Status:** In Progress
**PRD:** [Fase 3 — Deploy da Aplicação](../PRD.md#fase-3--deploy-da-aplicação) · [Fase 5 — Configuração](../PRD.md#fase-5--configuração) · [Fase 8 — Exposição Externa](../PRD.md#fase-8--exposição-externa) · [Cenário 1 — Deploy Completo da Plataforma](../PRD.md#cenário-1--deploy-completo-da-plataforma) · [Cenário 11 — ConfigMap](../PRD.md#cenário-11--configmap) · [Cenário 12 — Secrets](../PRD.md#cenário-12--secrets) · [Cenário 17 — Labels e Selectors](../PRD.md#cenário-17--labels-e-selectors)
**TDD:** [TDD-003](../tdd/TDD-003-infraestrutura-kubernetes.md)

---

## Estória

**Como** estudante de Kubernetes,
**quero** implantar a Product API com namespaces, Deployment, Services, ConfigMap, Secret e Ingress,
**para que** que a aplicação rode no cluster com configuração externalizada e acesso unificado.

---

## Critérios de Aceite

- [x] Namespaces `learning-lab` e `monitoring` criados
- [x] Deployment `product-api` com Pod em `Running` e ReplicaSet criado automaticamente
- [x] Configuração (URLs) vem de ConfigMap e credenciais de Secret, sem rebuild da imagem
- [x] API acessível via NodePort (`$(minikube ip):30080`) e via Ingress (`product-api.local`)
- [x] `kubectl get pods -l app=product-api -n learning-lab` retorna apenas os Pods da API
- [x] Alterar o ConfigMap e executar `kubectl rollout restart` aplica a nova configuração


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-003 com status `Done`
- [ ] Cenário 1, Cenário 11, Cenário 12, Cenário 17 do PRD executado(s)
