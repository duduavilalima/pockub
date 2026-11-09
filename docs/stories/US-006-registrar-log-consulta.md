# US-006 — Registro Automático de Log de Consulta

**Requisito:** [RF-006](../RULES.md#rf-006--registrar-log-de-consulta)
**Status:** Done
**PRD:** [Fase 2 — Aplicação](../PRD.md#fase-2--aplicação) · [Cenário 3 — Consulta com Auditoria](../PRD.md#cenário-3--consulta-com-auditoria)
**TDD:** [TDD-001](../tdd/TDD-001-product-api.md)

---

## Estória

**Como** administrador do sistema,
**quero** que toda operação de leitura no catálogo seja registrada automaticamente,
**para que** eu tenha rastreabilidade de acesso aos dados sem depender de ação manual.

---

## Critérios de Aceite

- [x] Toda chamada a `GET /produtos/{id}` gera um log com `operacao: CONSULTA`
- [x] Toda chamada a `GET /produtos` gera um log com `operacao: LISTAGEM`
- [x] Operações de escrita (`POST`, `PUT`, `DELETE`) **não** geram log de acesso
- [x] Log persiste no MongoDB na collection `access_logs`
- [x] `timestamp` reflete o momento real da operação (não o momento de escrita no MongoDB)
- [x] `origemRequisicao` captura o IP do cliente da requisição HTTP (atrás do Ingress, via `X-Forwarded-For`)
- [x] Falha na gravação do log **não** propaga exceção para o cliente — resposta principal não é afetada

---

## Campos do Log

| Campo | Fonte |
|-------|-------|
| `produtoId` | Path variable `{id}` (null para LISTAGEM) |
| `nomeProduto` | Nome do produto no momento da consulta (null para LISTAGEM) |
| `operacao` | `CONSULTA` ou `LISTAGEM` |
| `timestamp` | `LocalDateTime.now()` capturado no thread da requisição, antes do disparo assíncrono |
| `origemRequisicao` | `HttpServletRequest.getRemoteAddr()` (com `server.forward-headers-strategy=native`, devolve o IP real do cliente atrás do Ingress) |

---

## Notas Técnicas

- Implementação sugerida via `@Async` ou evento de domínio para não atrasar a resposta
- Em caso de indisponibilidade do MongoDB, logar o erro internamente e prosseguir normalmente


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [x] Todos os critérios de aceite validados e marcados `[x]`
- [x] TDD-001 com status `Done`
- [x] Cenário 3 do PRD executado(s)
