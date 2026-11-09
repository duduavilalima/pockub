# US-007 — Consultar Logs de Acesso

**Requisito:** [RF-007](../RULES.md#rf-007--consultar-logs)
**Status:** In Progress
**PRD:** [Fase 2 — Aplicação](../PRD.md#fase-2--aplicação) · [Cenário 3 — Consulta com Auditoria](../PRD.md#cenário-3--consulta-com-auditoria)
**TDD:** [TDD-001](../tdd/TDD-001-product-api.md)

---

## Estória

**Como** administrador do sistema,
**quero** consultar todos os logs de acesso registrados,
**para que** eu possa auditar o histórico de consultas ao catálogo.

---

## Critérios de Aceite

- [ ] `GET /logs` retorna `200 OK` com array de logs
- [ ] Retorna array vazio `[]` quando não há logs — não retorna `404`
- [ ] Logs ordenados por `timestamp` decrescente (mais recentes primeiro)
- [ ] Todos os campos do `LogAcesso` retornados na resposta

---

## Exemplo de Requisição

```http
GET /logs
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
    "id": "6660a0b1c2d3e4f5a6b7c8d9",
    "produtoId": null,
    "nomeProduto": null,
    "operacao": "LISTAGEM",
    "timestamp": "2026-06-03T11:55:00",
    "origemRequisicao": "192.168.1.10"
  }
]
```


---

## Definition of Ready

- [x] Estória no formato Como/quero/para
- [x] Critérios de aceite verificáveis
- [x] Referências ao PRD (Fase e Cenário) preenchidas
- [x] TDD associado identificado

## Definition of Done

- [ ] Todos os critérios de aceite validados e marcados `[x]`
- [ ] TDD-001 com status `Done`
- [ ] Cenário 3 do PRD executado(s)
