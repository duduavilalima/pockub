# TDD-001 — Product API: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

### pom.xml (modificado)
- `java.version` corrigido de 25 → 21 (alinhado com TDD-001 e CLAUDE.md)
- Starters corrigidos para nomes canônicos do Spring Boot 4: `spring-boot-starter-web`, `spring-boot-starter-data-mongodb`
- Dependências adicionadas:
  - `spring-boot-starter-validation` — Bean Validation para DTOs
  - `spring-boot-starter-actuator` — health/prometheus endpoints para K8s
  - `micrometer-registry-prometheus` — exportação de métricas no formato Prometheus
  - `flyway-core` + `flyway-database-postgresql` — gerenciamento de schema
- Dependências de teste corrigidas: os starters inexistentes (`*-data-jpa-test`, `*-mongodb-test`, `*-webmvc-test`) foram substituídos por `spring-boot-starter-test`

### Arquivos criados

| Arquivo | Descrição |
|---------|-----------|
| `domain/Operacao.java` | Enum `CONSULTA` / `LISTAGEM` |
| `domain/Produto.java` | Entidade JPA mapeada para a tabela `produtos` |
| `domain/LogAcesso.java` | Documento MongoDB para a collection `access_logs` |
| `repository/ProdutoRepository.java` | `JpaRepository<Produto, Long>` sem queries customizadas |
| `repository/LogAcessoRepository.java` | `MongoRepository` com dois métodos derivados ordenados por `timestamp DESC` |
| `dto/ProdutoRequest.java` | Record com Bean Validation para criação/atualização |
| `dto/ProdutoResponse.java` | Record de saída com factory method `from(Produto)` |
| `dto/LogAcessoResponse.java` | Record de saída com factory method `from(LogAcesso)` |
| `exception/ProdutoNotFoundException.java` | RuntimeException com mensagem padronizada |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` centralizando 404, 400 e 500 |
| `config/AsyncConfig.java` | Thread pool dedicado para gravação assíncrona de logs |
| `service/LogAcessoService.java` | Persiste `LogAcesso` no MongoDB de forma assíncrona (`@Async`) |
| `service/ProdutoService.java` | CRUD completo + orquestra o disparo de logs |
| `controller/ProdutoController.java` | 5 endpoints REST para `/produtos` |
| `controller/LogAcessoController.java` | 1 endpoint REST `/logs` com filtro opcional por `produtoId` |
| `resources/application.yaml` | Configuração externalizada via variáveis de ambiente |
| `resources/db/migration/V1__create_produtos_table.sql` | Criação da tabela `produtos` com constraints e índice em `nome` |

---

## 2. Por que cada decisão foi tomada

### Records para DTOs
Records são imutáveis por natureza, eliminam boilerplate de `equals/hashCode/toString` e tornam o contrato da API explícito. `ProdutoResponse` e `LogAcessoResponse` contêm um factory method `from(entity)` para encapsular a conversão e manter controllers e services limpos.

### `@Transactional(readOnly = true)` no ProdutoService
Marcar a classe com `readOnly = true` e sobrescrever apenas os métodos de escrita com `@Transactional` simples é o padrão recomendado: o Spring repassa a hint ao driver JDBC, potencialmente reduzindo lock overhead no PostgreSQL. Os métodos de escrita (`criar`, `atualizar`, `excluir`) sobrescrevem o padrão com a anotação sem `readOnly`.

### `@Async` no LogAcessoService com falha silenciosa
A gravação de log não deve bloquear a resposta ao cliente nem derrubar a API caso o MongoDB esteja indisponível. O `@Async` despacha a gravação para o thread pool `log-async-`, e qualquer exceção é capturada e logada sem propagação — conforme especificado no TDD-001 seção 5.2.

### `@Indexed` em `LogAcesso.produtoId` e `timestamp`
- `produtoId`: suporta o filtro `GET /logs?produtoId={id}` (RF-008)
- `timestamp`: suporta a query de limpeza do CronJob (Fase 9/TDD-006)

Os índices são criados pelo Spring Data MongoDB no startup quando `spring.data.mongodb.auto-index-creation=true` (default em dev). Em produção, criar os índices via migration ou script separado é mais seguro.

### Flyway com `ddl-auto: validate`
O Hibernate valida o schema existente mas não o cria ou altera. Isso força o uso do Flyway como única fonte de verdade para mudanças de schema, prevenindo divergências entre ambientes. Em Kubernetes, o Flyway roda no startup da aplicação, portanto o PostgreSQL deve estar disponível antes do Pod ficar `Ready` (controlado pelo `readinessProbe`).

### `open-in-view: false`
Desabilita o padrão "Open Session in View" do Spring MVC, que mantém a conexão JDBC aberta durante toda a renderização HTTP. Em APIs REST isso só aumenta o consumo de conexões sem benefício.

### `LogAcessoController` acessa o repository diretamente
Leitura de logs é uma operação simples de consulta sem regra de negócio. Adicionar um `LogAcessoService` intermediário seria uma abstração prematura — o controller acessa o `LogAcessoRepository` diretamente, mantendo o código enxuto.

---

## 3. Alternativas descartadas

### MapStruct para mapeamento DTO ↔ Entity
MapStruct geraria o mapeamento em tempo de compilação, mas adicionaria um processador de anotações extra e configuração Maven. Para este projeto com poucas entidades, os factory methods `from(entity)` nos records são suficientes e mais explícitos.

### Lombok `@Data` / `@Value` nos DTOs
`@Data` em entidades JPA causa problemas com `equals/hashCode` baseados em todos os campos (incluindo coleções lazy), e `@Value` tornaria a entidade imutável, incompatível com JPA. Optou-se por `@Getter @Setter @NoArgsConstructor` no `Produto` e records imutáveis nos DTOs.

### `CompletableFuture` em vez de `@Async`
`CompletableFuture` oferece mais controle sobre a composição assíncrona, mas é mais verboso. Como o log de acesso é "dispara e esquece" sem tratamento de resultado, `@Async` com thread pool configurado é mais simples e direto.

### Endpoint separado para `GET /logs` e `GET /logs?produtoId`
Poderia-se criar dois endpoints distintos (ex: `/logs` e `/logs/produto/{id}`). O TDD especifica explicitamente o uso de query param, seguindo o padrão de filtro em coleções RESTful.

---

## 4. Pontos de atenção

### Ordem de startup no Kubernetes
O Flyway roda no startup da aplicação e tenta conectar ao PostgreSQL. Se o Pod da API subir antes do PostgreSQL estar `Ready`, o startup falha. Soluções:
1. `initContainer` que aguarda o PostgreSQL
2. `startupProbe` com `failureThreshold` alto (conforme TDD-001 seção 12)
3. `depends_on` no Helm (via hooks)

### `data_atualizacao` no PostgreSQL
O Flyway cria a coluna com `DEFAULT now()`, mas a atualização automática precisa ser feita via `@UpdateTimestamp` do Hibernate — diferente do MySQL/MariaDB, o PostgreSQL não tem `ON UPDATE CURRENT_TIMESTAMP` nativo. Isso está correto: o Hibernate gerencia via `@UpdateTimestamp`.

### Thread pool do `@Async`
O bean `taskExecutor` configurado no `AsyncConfig` usa o nome `log-async-`. O Spring `@Async` busca o bean por tipo (`Executor`) — se houver múltiplos beans `Executor` no contexto, especifique o nome explicitamente com `@Async("taskExecutor")`.

### `produtoId` null para LISTAGEM
O campo `produtoId` em `LogAcesso` é `null` para operações `LISTAGEM`. O MongoDB armazena sem problema. A consulta `findByProdutoIdOrderByTimestampDesc` com `produtoId=null` retornaria logs de LISTAGEM, o que não é o comportamento esperado para `GET /logs?produtoId={id}`. Como `produtoId` no request param sempre será um `Long` não-nulo (a conversão falha antes), isso não é um problema prático.

---

## 5. Como validar

### Compilar

```bash
cd application/product-api
./mvnw compile
```

Esperado: `BUILD SUCCESS`

### Subir localmente (requer PostgreSQL e MongoDB)

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pockub
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export MONGO_URI=mongodb://localhost:27017/pockub

./mvnw spring-boot:run
```

### Testar endpoints

```bash
# Criar produto
curl -s -X POST http://localhost:8080/produtos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado","descricao":"Mecânico","preco":299.90,"quantidadeEstoque":10}' | jq

# Listar produtos (gera log LISTAGEM)
curl -s http://localhost:8080/produtos | jq

# Consultar produto por ID (gera log CONSULTA)
curl -s http://localhost:8080/produtos/1 | jq

# Atualizar produto
curl -s -X PUT http://localhost:8080/produtos/1 \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado Pro","descricao":"Mecânico RGB","preco":399.90,"quantidadeEstoque":5}' | jq

# Excluir produto
curl -s -X DELETE http://localhost:8080/produtos/1 -w "%{http_code}"

# Listar logs
curl -s http://localhost:8080/logs | jq

# Filtrar logs por produto
curl -s "http://localhost:8080/logs?produtoId=1" | jq

# Health check (para K8s probes)
curl -s http://localhost:8080/actuator/health | jq

# Métricas Prometheus
curl -s http://localhost:8080/actuator/prometheus | head -20
```

### Validar tratamento de erros

```bash
# 404 — produto inexistente
curl -s http://localhost:8080/produtos/999 | jq
# {"erro":"Produto 999 não encontrado"}

# 400 — payload inválido
curl -s -X POST http://localhost:8080/produtos \
  -H "Content-Type: application/json" \
  -d '{"nome":"","preco":-1,"quantidadeEstoque":-5}' | jq
# {"erros":["não deve estar em branco","deve ser maior que 0","deve ser maior que ou igual a 0"]}
```
