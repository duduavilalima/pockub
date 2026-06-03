# US-008 — Filtrar Logs por Produto

**Requisito:** [RF-008](../RULES.md#rf-008--filtrar-logs-por-produto)

---

## Estória

**Como** administrador do sistema,
**quero** filtrar os logs de acesso por produto,
**para que** eu possa auditar especificamente quais consultas foram feitas a um produto em particular.

---

## Critérios de Aceite

- [ ] `GET /logs?produtoId={id}` retorna `200 OK` com logs filtrados pelo produto
- [ ] Retorna array vazio `[]` quando não há logs para o produto — não retorna `404`
- [ ] Logs ordenados por `timestamp` decrescente
- [ ] Logs de `LISTAGEM` (sem `produtoId`) **não** aparecem no filtro por produto
- [ ] `produtoId` inválido (não numérico) retorna `400 Bad Request`
- [ ] Funciona mesmo se o produto tiver sido excluído do PostgreSQL (logs são imutáveis)

---

## Exemplo de Requisição

```http
GET /logs?produtoId=1
```

## Exemplo de Resposta

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "6660a1b2c3d4e5f6a7b8c9d0",
    "produtoId": 1,
    "nomeProduto": "Teclado Mecânico RGB",
    "operacao": "CONSULTA",
    "timestamp": "2026-06-03T12:00:00",
    "origemRequisicao": "192.168.1.10"
  },
  {
    "id": "6659f9a8b7c6d5e4f3a2b1c0",
    "produtoId": 1,
    "nomeProduto": "Teclado Mecânico",
    "operacao": "CONSULTA",
    "timestamp": "2026-06-03T09:30:00",
    "origemRequisicao": "192.168.1.15"
  }
]
```

## Nota

O segundo log mostra `nomeProduto: "Teclado Mecânico"` (nome anterior) — evidência de que o campo é snapshot imutável do momento da consulta, independente de atualizações posteriores no produto.
