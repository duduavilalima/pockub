# TDD-001 — Product API

**Status:** In Progress
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [STACK.md](../STACK.md) · [RULES.md](../RULES.md) · [MODEL.md](../MODEL.md) · [ADR-002](../adr/ADR-002-java-spring-boot.md)
**Estórias:** [US-001](../stories/US-001-criar-produto.md) · [US-002](../stories/US-002-atualizar-produto.md) · [US-003](../stories/US-003-excluir-produto.md) · [US-004](../stories/US-004-consultar-produto.md) · [US-005](../stories/US-005-listar-produtos.md) · [US-006](../stories/US-006-registrar-log-consulta.md) · [US-007](../stories/US-007-consultar-logs.md) · [US-008](../stories/US-008-filtrar-logs-produto.md)
**PRD:** [Fase 2 — Aplicação](../PRD.md#fase-2--aplicação) · [Cenário 2 — CRUD de Produtos](../PRD.md#cenário-2--crud-de-produtos) · [Cenário 3 — Consulta com Auditoria](../PRD.md#cenário-3--consulta-com-auditoria)

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
│   └── LogAcessoController.java      ← @RestController /logs (consulta o LogAcessoRepository direto — leitura sem regra de negócio)

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
                      Operacao operacao, LocalDateTime timestamp, String origem) {
    // timestamp é capturado pelo chamador (thread da requisição), não aqui,
    // para refletir o momento real da operação e não o da gravação no MongoDB
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

`origemRequisicao` extraída via `HttpServletRequest.getRemoteAddr()` na thread da requisição (não é acessível no `@Async`). Atrás do Ingress, `server.forward-headers-strategy: native` faz o `getRemoteAddr()` devolver o IP real do cliente (`X-Forwarded-For`).

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
server:
  forward-headers-strategy: native

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  mongodb:
    uri: ${MONGO_URI}   # Spring Boot 4: spring.data.mongodb.uri foi removida (erro de deprecação)
  threads:
    virtual:
      enabled: true     # Tomcat em Virtual Threads (ADR-002); o @Async usa pool próprio

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: always  # apenas estudo; em produção, when-authorized
  metrics:
    tags:
      application: product-api
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

Flyway gerencia o schema do PostgreSQL e executa as migrations no startup da própria API.

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
| `show-details: always` | Expõe detalhes de DB/Mongo em `/actuator/health` sem autenticação — aceitável só no estudo; use `when-authorized` fora dele |
| Thread pool `@Async` | Monitorar saturação da fila via métricas do Actuator em carga alta |
| `ddl-auto: validate` | O Flyway roda no startup da API antes do Hibernate validar; basta o PostgreSQL estar acessível — por isso o Deployment usa um `initContainer` que aguarda a porta 5432 (ver [TDD-004](TDD-004-persistencia.md)) |


---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [ ] Artefatos implementados conforme este documento (divergências registradas aqui)
- [ ] Suíte `./mvnw test` verde, com PostgreSQL e MongoDB disponíveis
- [ ] Cada critério de aceite de US-001 a US-008 coberto por teste automatizado
- [ ] Cenários 2 e 3 do PRD executados com `curl` (ver `_explain`)
- [ ] Arquivo `TDD-001-product-api_explain.md` gerado
- [ ] Estórias vinculadas atualizadas (US-001, US-002, US-003, US-004, US-005, US-006, US-007, US-008)
