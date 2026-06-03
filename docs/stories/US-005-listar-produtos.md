# US-005 — Listar Produtos

**Requisito:** [RF-005](../RULES.md#rf-005--listar-produtos)

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
    "preco": 349.90,
    "quantidadeEstoque": 10,
    "dataCriacao": "2026-06-03T10:00:00",
    "dataAtualizacao": "2026-06-03T11:30:00"
  },
  {
    "id": 2,
    "nome": "Mouse Gamer",
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
