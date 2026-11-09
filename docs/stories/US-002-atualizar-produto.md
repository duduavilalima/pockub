# US-002 — Atualizar Produto

**Requisito:** [RF-002](../RULES.md#rf-002--atualizar-produto)
**Status:** In Progress
**PRD:** [Fase 2 — Aplicação](../PRD.md#fase-2--aplicação) · [Cenário 2 — CRUD de Produtos](../PRD.md#cenário-2--crud-de-produtos) · [RF-002 - Atualizar Produto](../PRD.md#rf-002---atualizar-produto)
**TDD:** [TDD-001](../tdd/TDD-001-product-api.md)

---

## Estória

**Como** operador do sistema,
**quero** atualizar os dados de um produto existente,
**para que** as informações do catálogo reflitam mudanças de preço, estoque ou descrição.

---

## Critérios de Aceite

- [ ] `PUT /produtos/{id}` com body válido retorna `200 OK` e o produto atualizado
- [ ] `dataAtualizacao` atualizado automaticamente após cada alteração
- [ ] `dataCriacao` **não** é alterado
- [ ] `id` inexistente retorna `404 Not Found`
- [ ] Validações de `nome`, `preco` e `quantidadeEstoque` idênticas ao RF-001
- [ ] Alteração persistida no PostgreSQL

---

## Exemplo de Requisição

```http
PUT /produtos/1
Content-Type: application/json

{
  "nome": "Teclado Mecânico RGB",
  "descricao": "Teclado com switches Cherry MX e iluminação RGB",
  "preco": 349.90,
  "quantidadeEstoque": 10
}
```

## Exemplo de Resposta

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 1,
  "nome": "Teclado Mecânico RGB",
  "descricao": "Teclado com switches Cherry MX e iluminação RGB",
  "preco": 349.90,
  "quantidadeEstoque": 10,
  "dataCriacao": "2026-06-03T10:00:00",
  "dataAtualizacao": "2026-06-03T11:30:00"
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
- [ ] Cenário 2 do PRD executado(s)
