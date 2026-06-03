# US-001 — Criar Produto

**Requisito:** [RF-001](../RULES.md#rf-001--criar-produto)

---

## Estória

**Como** operador do sistema,
**quero** cadastrar um novo produto com nome, descrição, preço e quantidade em estoque,
**para que** o produto fique disponível para consulta e gerenciamento.

---

## Critérios de Aceite

- [ ] `POST /produtos` com body válido retorna `201 Created` e o produto criado com `id` gerado
- [ ] Produto persistido no PostgreSQL e recuperável via `GET /produtos/{id}`
- [ ] `nome` ausente retorna `400 Bad Request`
- [ ] `preco <= 0` retorna `400 Bad Request`
- [ ] `quantidadeEstoque < 0` retorna `400 Bad Request`
- [ ] `dataCriacao` e `dataAtualizacao` preenchidos automaticamente

---

## Exemplo de Requisição

```http
POST /produtos
Content-Type: application/json

{
  "nome": "Teclado Mecânico",
  "descricao": "Teclado com switches Cherry MX",
  "preco": 299.90,
  "quantidadeEstoque": 15
}
```

## Exemplo de Resposta

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 1,
  "nome": "Teclado Mecânico",
  "descricao": "Teclado com switches Cherry MX",
  "preco": 299.90,
  "quantidadeEstoque": 15,
  "dataCriacao": "2026-06-03T10:00:00",
  "dataAtualizacao": "2026-06-03T10:00:00"
}
```
