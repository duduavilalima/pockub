# TDD-001 — Product API

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [STACK.md](../STACK.md) · [RULES.md](../RULES.md) · [MODEL.md](../MODEL.md) · [ADR-002](../adr/ADR-002-java-spring-boot.md)

---

## 1. Visão Geral

API REST responsável pelo CRUD de Produtos (PostgreSQL) e pelo registro automático de logs de acesso (MongoDB). Aplicação stateless — escala horizontalmente via Deployment no Kubernetes.

---

## 2. Stack

| Componente | Tecnologia |
|-----------|-----------|
| Linguagem | Java 21 |
| Framework | Spring Boot 4 |
| Persistência relacional | Spring Data JPA + PostgreSQL |
| Persistência NoSQL | Spring Data MongoDB |
| Observabilidade | Spring Boot Actuator + Micrometer |
| Build | Maven |

---

## 3. Estrutura de Pacotes

```
br.com.lima.pockub
├── PockubApplication.java

├── domain/
│   ├── Produto.java                  ← @Entity JPA
│   ├── LogAcesso.java                ← @Document MongoDB
│   └── Operacao.java                 ← enum: CONSULTA | LISTAGEM

├── repository/
│   ├── ProdutoRepository.java        ← JpaRepository<Produto, Long>
│   └── LogAcessoRepository.java      ← MongoRepository<LogAcesso, String>

├── service/
│   ├── ProdutoService.java           ← orquestra CRUD + dispara log
│   └── LogAcessoService.java         ← persiste LogAcesso de forma assíncrona

├── controller/
│   ├── ProdutoController.java        ← @RestController /produtos
│   └── LogAcessoController.java      ← @RestController /logs

├── dto/
│   ├── ProdutoRequest.java           ← input validado com Bean Validation
│   ├── ProdutoResponse.java          ← output serializado
│   └── LogAcessoResponse.java        ← output de /logs

├── exception/
│   ├── ProdutoNotFoundException.java ← 404
│   └── GlobalExceptionHandler.java   ← @RestControllerAdvice

└── config/
    └── AsyncConfig.java              ← @EnableAsync + ThreadPoolTaskExecutor
```

---

## 4. Domínio

### 4.1 Produto

```java
@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer quantidadeEstoque;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;
}
```

### 4.2 LogAcesso

```java
@Document(collection = "access_logs")
public class LogAcesso {

    @Id
    private String id;

    private Long produtoId;        // null para LISTAGEM
    private String nomeProduto;    // snapshot — não muda se produto for renomeado
    private Operacao operacao;
    private LocalDateTime timestamp;
    private String origemRequisicao;
}
```

### 4.3 Operacao

```java
public enum Operacao {
    CONSULTA,   // GET /produtos/{id}
    LISTAGEM    // GET /produtos
}
```

---

## 5. Camada de Serviço

### 5.1 ProdutoService

Responsabilidades:
- CRUD completo de Produto
- Após `findById` ou `findAll`, delega ao `LogAcessoService` de forma assíncrona

```
findById(id):
  1. busca Produto no PostgreSQL
  2. lança ProdutoNotFoundException se ausente
  3. dispara log assíncrono: operacao=CONSULTA, produtoId=id, nomeProduto
  4. retorna ProdutoResponse

findAll():
  1. busca todos os Produtos no PostgreSQL
  2. dispara log assíncrono: operacao=LISTAGEM
  3. retorna List<ProdutoResponse>
```

### 5.2 LogAcessoService

```java
@Async
public void registrar(Long produtoId, String nomeProduto,
                      Operacao operacao, String origem) {
    // persiste no MongoDB via LogAcessoRepository
    // falha silenciosa — loga o erro mas não propaga
}
```

Executa em thread separada via `@Async` — falha na gravação do log não afeta a resposta ao cliente.

---

## 6. Camada de Controller

### 6.1 ProdutoController

```
POST   /produtos              → criar
PUT    /produtos/{id}         → atualizar
DELETE /produtos/{id}         → excluir
GET    /produtos/{id}         → consultar (gera log CONSULTA)
GET    /produtos              → listar   (gera log LISTAGEM)
```

`origemRequisicao` extraída via `HttpServletRequest.getRemoteAddr()`.

### 6.2 LogAcessoController

```
GET /logs                     → listar todos os logs (ordem: timestamp DESC)
GET /logs?produtoId={id}      → filtrar por produto
```

---

## 7. DTOs e Validação

### ProdutoRequest

```java
public record ProdutoRequest(
    @NotBlank @Size(max = 255) String nome,
    String descricao,
    @NotNull @DecimalMin("0.01") BigDecimal preco,
    @NotNull @Min(0) Integer quantidadeEstoque
) {}
```

### ProdutoResponse

```java
public record ProdutoResponse(
    Long id,
    String nome,
    String descricao,
    BigDecimal preco,
    Integer quantidadeEstoque,
    LocalDateTime dataCriacao,
    LocalDateTime dataAtualizacao
) {}
```

---

## 8. Tratamento de Erros

| Exceção | Status HTTP | Mensagem |
|---------|------------|---------|
| `ProdutoNotFoundException` | 404 | `Produto {id} não encontrado` |
| `MethodArgumentNotValidException` | 400 | lista de erros de validação |
| `Exception` (catch-all) | 500 | mensagem genérica |

`GlobalExceptionHandler` via `@RestControllerAdvice` — resposta padronizada em JSON.

---

## 9. Configuração Spring Boot

### application.yaml

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  data:
    mongodb:
      uri: ${MONGO_URI}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always
```

Variáveis de ambiente injetadas via ConfigMap e Secret do Kubernetes (ver [TDD-003](TDD-003-infraestrutura-kubernetes.md)).

### AsyncConfig

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("log-async-");
        return executor;
    }
}
```

---

## 10. Actuator — Endpoints K8s

| Endpoint | Uso no Kubernetes |
|----------|------------------|
| `/actuator/health/liveness` | livenessProbe |
| `/actuator/health/readiness` | readinessProbe |
| `/actuator/prometheus` | scrape do Prometheus |
| `/actuator/info` | informações de versão |

---

## 11. Migrações de Banco

Flyway ou Liquibase gerencia o schema do PostgreSQL.

Localização dos scripts: `src/main/resources/db/migration/`

Convenção de nomenclatura: `V{versão}__{descricao}.sql`

```
V1__create_produtos_table.sql
```

`ddl-auto: validate` — o Hibernate valida o schema mas não o cria/altera.

---

## 12. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Startup lento da JVM | `startupProbe` com `failureThreshold: 12` e `periodSeconds: 5` (60s total) |
| Falha no MongoDB | Log assíncrono com tratamento silencioso — não derruba a API |
| Thread pool `@Async` | Monitorar saturação da fila via métricas do Actuator em carga alta |
| `ddl-auto: validate` | Schema precisa existir antes do startup — Migration deve rodar primeiro (via Init Container ou Job K8s) |
