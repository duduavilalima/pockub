# Modelo de Dados

> Parte do [PRD — Kubernetes Learning Lab](PRD.md)

---

## Produto — PostgreSQL

**Tabela:** `produtos`

| Campo | Tipo | Restrições |
|-------|------|------------|
| id | BIGSERIAL | PK, auto-increment |
| nome | VARCHAR(255) | NOT NULL |
| descricao | TEXT | nullable |
| preco | NUMERIC(10,2) | NOT NULL, CHECK > 0 |
| quantidade_estoque | INTEGER | NOT NULL, CHECK >= 0 |
| data_criacao | TIMESTAMP | NOT NULL, DEFAULT now() |
| data_atualizacao | TIMESTAMP | NOT NULL, atualizado automaticamente |

**Índices:**
- PK em `id`
- Índice futuro sugerido em `nome` para busca por texto

**Mapeamento Java:**

```java
@Entity
@Table(name = "produtos")
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String nome;
    private String descricao;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;
    @Column(nullable = false)
    private Integer quantidadeEstoque;
    @CreationTimestamp
    private LocalDateTime dataCriacao;
    @UpdateTimestamp
    private LocalDateTime dataAtualizacao;
}
```

---

## LogAcesso — MongoDB

**Collection:** `access_logs`

| Campo | Tipo | Restrições |
|-------|------|------------|
| id | String (ObjectId) | Gerado automaticamente pelo MongoDB |
| produtoId | Long | NOT NULL |
| nomeProduto | String | NOT NULL |
| operacao | Enum | `CONSULTA` \| `LISTAGEM` |
| timestamp | DateTime | NOT NULL, gerado no momento da operação |
| origemRequisicao | String | IP ou User-Agent da requisição |

**Índices:**
- `produtoId` — para queries de RF-008 (filtrar logs por produto)
- `timestamp` — para queries de limpeza no CronJob

**Enum `Operacao`:**

```java
public enum Operacao {
    CONSULTA,   // GET /produtos/{id}
    LISTAGEM    // GET /produtos
}
```

**Mapeamento Java:**

```java
@Document(collection = "access_logs")
public class LogAcesso {
    @Id
    private String id;
    private Long produtoId;
    private String nomeProduto;
    private Operacao operacao;
    private LocalDateTime timestamp;
    private String origemRequisicao;
}
```

---

## Relação entre os Modelos

```
Produto (PostgreSQL)
    id ──────────────────────┐
    nome                     │
    descricao                │  referência por valor
    preco                    │  (sem FK cross-database)
    quantidadeEstoque        │
                             ▼
                    LogAcesso (MongoDB)
                        produtoId  ← cópia do Produto.id
                        nomeProduto ← snapshot do Produto.nome
                        operacao
                        timestamp
                        origemRequisicao
```

> `nomeProduto` é um snapshot no momento do log — não é atualizado se o produto for renomeado. Isso é intencional para fins de auditoria.
