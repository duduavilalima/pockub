# US-006 — Registro Automático de Log de Consulta

**Requisito:** [RF-006](../RULES.md#rf-006--registrar-log-de-consulta)

---

## Estória

**Como** administrador do sistema,
**quero** que toda operação de leitura no catálogo seja registrada automaticamente,
**para que** eu tenha rastreabilidade de acesso aos dados sem depender de ação manual.

---

## Critérios de Aceite

- [ ] Toda chamada a `GET /produtos/{id}` gera um log com `operacao: CONSULTA`
- [ ] Toda chamada a `GET /produtos` gera um log com `operacao: LISTAGEM`
- [ ] Operações de escrita (`POST`, `PUT`, `DELETE`) **não** geram log de acesso
- [ ] Log persiste no MongoDB na collection `access_logs`
- [ ] `timestamp` reflete o momento real da operação (não o momento de escrita no MongoDB)
- [ ] `origemRequisicao` captura o IP ou User-Agent da requisição HTTP
- [ ] Falha na gravação do log **não** propaga exceção para o cliente — resposta principal não é afetada

---

## Campos do Log

| Campo | Fonte |
|-------|-------|
| `produtoId` | Path variable `{id}` (null para LISTAGEM) |
| `nomeProduto` | Nome do produto no momento da consulta (null para LISTAGEM) |
| `operacao` | `CONSULTA` ou `LISTAGEM` |
| `timestamp` | `LocalDateTime.now()` no momento da operação |
| `origemRequisicao` | `HttpServletRequest.getRemoteAddr()` |

---

## Notas Técnicas

- Implementação sugerida via `@Async` ou evento de domínio para não atrasar a resposta
- Em caso de indisponibilidade do MongoDB, logar o erro internamente e prosseguir normalmente
