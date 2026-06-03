# TDD-002 — Containerização

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [TDD-001](TDD-001-product-api.md) · [ADR-002](../adr/ADR-002-java-spring-boot.md)

---

## 1. Visão Geral

Define o processo de build da imagem Docker da Product API. Usa multi-stage build para minimizar o tamanho da imagem final — apenas o JRE e o JAR compilado são incluídos na imagem de runtime.

---

## 2. Estratégia de Build

```
Stage 1: builder
  └── maven:3.9-eclipse-temurin-21
      ├── copia pom.xml
      ├── baixa dependências (camada cacheável)
      ├── copia src/
      └── mvn package -DskipTests → target/*.jar

Stage 2: runtime
  └── eclipse-temurin:21-jre-alpine
      ├── copia JAR do stage builder
      ├── configura usuário não-root
      └── ENTRYPOINT java -jar app.jar
```

---

## 3. Dockerfile

Localização: `application/product-api/Dockerfile`

```dockerfile
# Stage 1 — build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Baixa dependências separado do código (cache de camada)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Compila
COPY src/ src/
RUN mvn package -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Usuário não-root por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. .dockerignore

Localização: `application/product-api/.dockerignore`

```
target/
.git/
.idea/
*.md
.mvn/
```

---

## 5. Build e Push da Imagem

### Uso com Minikube Registry

O Minikube expõe um registry local na porta `5000`. Para evitar pull de imagem externa durante os estudos:

```bash
# Habilitar o addon
minikube addons enable registry

# Apontar o Docker local para o registry do Minikube
eval $(minikube docker-env)

# Build direto no contexto do Minikube
docker build -t localhost:5000/product-api:1.0.0 \
  application/product-api/

# Push para o registry do Minikube
docker push localhost:5000/product-api:1.0.0
```

### Convenção de Tags

| Tag | Uso |
|-----|-----|
| `1.0.0` | versão estável para demonstrações |
| `1.0.0-broken` | versão defeituosa para o cenário de Rollback |
| `latest` | não usar em manifests K8s — sem rastreabilidade |

---

## 6. Política de Pull no Kubernetes

```yaml
imagePullPolicy: IfNotPresent
```

Com o Minikube registry local, `IfNotPresent` evita tentativas de pull externo e funciona sem credenciais de registry.

---

## 7. Tamanho Esperado da Imagem

| Stage | Base | Tamanho estimado |
|-------|------|-----------------|
| builder | `maven:3.9-eclipse-temurin-21` | ~600 MB (descartado) |
| runtime | `eclipse-temurin:21-jre-alpine` | ~80 MB + JAR (~30 MB) ≈ 110 MB |

A separação de stages garante que ferramentas de build (Maven, JDK completo) não entrem na imagem de produção.

---

## 8. Variáveis de Ambiente

Todas as configurações sensíveis são injetadas via Kubernetes — a imagem não contém valores de ambiente embutidos.

| Variável | Fonte K8s | Exemplo |
|----------|-----------|---------|
| `DB_URL` | ConfigMap | `jdbc:postgresql://postgresql:5432/pockub` |
| `DB_USERNAME` | Secret | `pockub` |
| `DB_PASSWORD` | Secret | `*****` |
| `MONGO_URI` | Secret | `mongodb://mongodb:27017/pockub` |

Detalhes: [TDD-003](TDD-003-infraestrutura-kubernetes.md)

---

## 9. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Cache de dependências Maven | A separação do `COPY pom.xml` + `dependency:go-offline` antes do `COPY src/` garante reuso do cache quando apenas o código muda |
| Usuário não-root | Obrigatório em clusters com `PodSecurity` — boa prática mesmo no Minikube |
| `imagePullPolicy: Always` | Não usar no ambiente de estudo — força pull externo desnecessário |
| JVM flags | Para containers pequenos, considerar `-XX:MaxRAMPercentage=75.0` para limitar heap ao container |
