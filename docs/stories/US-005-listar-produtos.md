# US-005 — Listar Produtos

**Requisito:** [RF-005](../RULES.md#rf-005--listar-produtos)
**Status:** In Progress
**PRD:** [Fase 2 — Aplicação](../PRD.md#fase-2--aplicação) · [Cenário 3 — Consulta com Auditoria](../PRD.md#cenário-3--consulta-com-auditoria) · [RF-005 - Listar Produtos](../PRD.md#rf-005---listar-produtos)
**TDD:** [TDD-001](../tdd/TDD-001-product-api.md)

---

## Estória

**Como** usuário do sistema,
**quero** listar todos os produtos cadastrados,
**para que** eu possa ter uma visão geral do catálogo.

---

## Critérios de Aceite

- [ ] `GET /produtos` retorna `200 OK` com array de produtos
- [ ] Retorna array vazio `[]` quando não há produtos cadastrados — não retorna `404`
- [ ] Após retornar a lista, um `LogAcesso` é criado no MongoDB com `operacao: LISTAGEM`
- [ ] O log registra a operação mesmo quando a lista retornada é vazia
- [ ] A criação do log **não** atrasa a resposta para o cliente

---

## Exemplo de Requisição

```http
GET /produtos
```

## Exemplo de Resposta

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": 1,
    "nome": "Teclado Mecânico RGB",
    "descricao": "Teclado com switches Cherry MX e iluminação RGB",
    "preco": 349.90,
    "quantidadeEstoque": 10,
    "dataCriacao": "2026-06-03T10:00:00",
    "dataAtualizacao": "2026-06-03T11:30:00"
  },
  {
    "id": 2,
    "nome": "Mouse Gamer",
    "descricao": "DPI ajustável",
    "preco": 199.90,
    "quantidadeEstoque": 25,
    "dataCriacao": "2026-06-03T09:00:00",
    "dataAtualizacao": "2026-06-03T09:00:00"
  }
]
```

## Efeito Colateral — Log Criado no MongoDB

```json
{
  "_id": "...",
  "produtoId": null,
  "nomeProduto": null,
  "operacao": "LISTAGEM",
  "timestamp": "2026-06-03T12:05:00",
  "origemRequisicao": "192.168.1.10"
}
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
