# US-004 — Consultar Produto

**Requisito:** [RF-004](../RULES.md#rf-004--consultar-produto)

---

## Estória

**Como** usuário do sistema,
**quero** consultar os detalhes de um produto pelo seu identificador,
**para que** eu possa visualizar suas informações completas.

---

## Critérios de Aceite

- [ ] `GET /produtos/{id}` retorna `200 OK` com os dados do produto
- [ ] `id` inexistente retorna `404 Not Found`
- [ ] Após retornar o produto, um `LogAcesso` é criado no MongoDB com `operacao: CONSULTA`
- [ ] A criação do log **não** atrasa a resposta para o cliente (assíncrono ou pós-resposta)
- [ ] Falha ao gravar o log **não** causa erro na resposta do produto

---

## Exemplo de Requisição

```http
GET /produtos/1
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

## Efeito Colateral — Log Criado no MongoDB

```json
{
  "_id": "...",
  "produtoId": 1,
  "nomeProduto": "Teclado Mecânico RGB",
  "operacao": "CONSULTA",
  "timestamp": "2026-06-03T12:00:00",
  "origemRequisicao": "192.168.1.10"
}
```
