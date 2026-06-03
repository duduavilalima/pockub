# US-003 — Excluir Produto

**Requisito:** [RF-003](../RULES.md#rf-003--excluir-produto)

---

## Estória

**Como** operador do sistema,
**quero** excluir um produto do catálogo,
**para que** produtos descontinuados não fiquem disponíveis para consulta.

---

## Critérios de Aceite

- [ ] `DELETE /produtos/{id}` retorna `204 No Content` quando o produto existe e é excluído
- [ ] Produto removido do PostgreSQL — `GET /produtos/{id}` retorna `404` após a exclusão
- [ ] `id` inexistente retorna `404 Not Found`
- [ ] Logs de acesso do produto no MongoDB **não** são excluídos (preservação de auditoria)

---

## Exemplo de Requisição

```http
DELETE /produtos/1
```

## Exemplo de Resposta

```http
HTTP/1.1 204 No Content
```

## Nota de Auditoria

Os registros de `LogAcesso` referentes ao produto excluído permanecem no MongoDB. O campo `nomeProduto` no log é um snapshot do momento da consulta e não é afetado pela exclusão.
