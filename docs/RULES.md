# Requisitos Funcionais

> Parte do [PRD — Kubernetes Learning Lab](PRD.md)
> Estórias de usuário em [docs/stories/](stories/)

---

## RF-001 — Criar Produto

O sistema deve permitir cadastrar produtos.

| Método | Endpoint | Body | Status |
|--------|----------|------|--------|
| POST | /produtos | `{ nome, descricao, preco, quantidadeEstoque }` | 201 / 400 |

**Validações:**
- `nome` obrigatório, máximo 255 caracteres
- `preco` obrigatório, maior que 0
- `quantidadeEstoque` obrigatório, maior ou igual a 0

**Estória:** [US-001 — Criar Produto](stories/US-001-criar-produto.md)

---

## RF-002 — Atualizar Produto

O sistema deve permitir atualizar produtos existentes.

| Método | Endpoint | Body | Status |
|--------|----------|------|--------|
| PUT | /produtos/{id} | `{ nome, descricao, preco, quantidadeEstoque }` | 200 / 400 / 404 |

**Validações:** mesmas do RF-001.

**Estória:** [US-002 — Atualizar Produto](stories/US-002-atualizar-produto.md)

---

## RF-003 — Excluir Produto

O sistema deve permitir excluir produtos.

| Método | Endpoint | Body | Status |
|--------|----------|------|--------|
| DELETE | /produtos/{id} | — | 204 / 404 |

**Estória:** [US-003 — Excluir Produto](stories/US-003-excluir-produto.md)

---

## RF-004 — Consultar Produto

O sistema deve permitir consultar um produto pelo identificador.

| Método | Endpoint | Body | Status |
|--------|----------|------|--------|
| GET | /produtos/{id} | — | 200 / 404 |

**Comportamento:** após retornar o produto, registra automaticamente um `LogAcesso` com `operacao: CONSULTA` (ver RF-006).

**Estória:** [US-004 — Consultar Produto](stories/US-004-consultar-produto.md)

---

## RF-005 — Listar Produtos

O sistema deve permitir listar todos os produtos cadastrados.

| Método | Endpoint | Body | Status |
|--------|----------|------|--------|
| GET | /produtos | — | 200 |

**Comportamento:** após retornar a lista, registra automaticamente um `LogAcesso` com `operacao: LISTAGEM` (ver RF-006).

**Estória:** [US-005 — Listar Produtos](stories/US-005-listar-produtos.md)

---

## RF-006 — Registrar Log de Consulta

Toda operação de consulta (RF-004) ou listagem (RF-005) deve gerar automaticamente um registro no MongoDB.

**Comportamento:** interno — sem endpoint próprio. Disparado pelos casos de uso de leitura.

| Campo | Valor |
|-------|-------|
| `operacao` | `CONSULTA` (RF-004) ou `LISTAGEM` (RF-005) |
| `timestamp` | Momento exato da operação |
| `origemRequisicao` | IP ou User-Agent da requisição |

**Estória:** [US-006 — Registro Automático de Log](stories/US-006-registrar-log-consulta.md)

---

## RF-007 — Consultar Logs

O sistema deve permitir consultar todos os logs registrados.

| Método | Endpoint | Body | Status |
|--------|----------|------|--------|
| GET | /logs | — | 200 |

**Estória:** [US-007 — Consultar Logs](stories/US-007-consultar-logs.md)

---

## RF-008 — Filtrar Logs por Produto

O sistema deve permitir consultar logs filtrados por produto.

| Método | Endpoint | Body | Status |
|--------|----------|------|--------|
| GET | /logs?produtoId={id} | — | 200 |

**Estória:** [US-008 — Filtrar Logs por Produto](stories/US-008-filtrar-logs-produto.md)
