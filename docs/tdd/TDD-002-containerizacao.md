# TDD-002 — Containerização

**Status:** Draft
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [TDD-001](TDD-001-product-api.md) · [ADR-002](../adr/ADR-002-java-spring-boot.md)
**Estórias:** [US-009](../stories/US-009-containerizar-api.md)
**PRD:** [Fase 2 — Aplicação](../PRD.md#fase-2--aplicação) · [Cenário 1 — Deploy Completo da Plataforma](../PRD.md#cenário-1--deploy-completo-da-plataforma)

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

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

`-XX:MaxRAMPercentage=75.0` faz o heap respeitar o limite do container (512Mi → ~384MB). Sem a flag, a JVM usaria só 25% do limite (~128MB) como heap máximo.

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

## 5. Build da Imagem

### Build direto no daemon do Minikube

`eval $(minikube docker-env)` aponta o `docker` local para o daemon Docker de dentro do Minikube. A imagem construída já fica disponível para o kubelet — não há registry, `docker push` nem pull externo:

```bash
eval $(minikube docker-env)

docker build -t product-api:1.0.0 application/product-api/

# Conferir
docker images product-api
```

> Alternativa descartada: addon `registry` + `docker push localhost:5000/...`. Exige um passo extra e, no driver Docker em Linux, port-forward para o registry ser alcançável do host; não agrega ao aprendizado desta POC.

### Convenção de Tags

A tag da imagem é independente da `<version>` do `pom.xml` (`0.0.1-SNAPSHOT`): ela versiona a imagem implantada, não o artefato Maven.

| Tag | Uso |
|-----|-----|
| `1.0.0` | versão estável para demonstrações |
| `1.0.0-broken` | versão defeituosa para o cenário de Rollback |
| `latest` | não usar em manifests K8s — sem rastreabilidade |

### Gerando as imagens dos cenários 9 e 10

```bash
eval $(minikube docker-env)

# 1.1.0 — nova versão para o Rolling Update (Cenário 9).
# Para uma mudança real, altere o código e rode o build com -t product-api:1.1.0;
# para só exercitar o rollout, basta uma nova tag da mesma imagem:
docker tag product-api:1.0.0 product-api:1.1.0

# 1.0.0-broken — imagem que falha ao subir (Cenário 10)
docker build -t product-api:1.0.0-broken - <<'EOF'
FROM product-api:1.0.0
ENTRYPOINT ["sh", "-c", "echo 'falha intencional'; exit 1"]
EOF
```

Com `maxUnavailable: 0`, os Pods da versão boa continuam atendendo enquanto o novo Pod entra em `CrashLoopBackOff`; `kubectl rollout undo` encerra o deploy defeituoso.

---

## 6. Política de Pull no Kubernetes

```yaml
imagePullPolicy: IfNotPresent
```

Como a imagem existe apenas no daemon do Minikube, `IfNotPresent` (ou `Never`) é obrigatório: `Always` tentaria um pull que falharia, pois a imagem não está em nenhum registry.

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
| `DB_URL` | ConfigMap `product-api-config` | `jdbc:postgresql://postgresql:5432/pockub` |
| `MONGO_URI` | ConfigMap `product-api-config` | `mongodb://mongodb:27017/pockub` (sem credenciais — MongoDB sem auth na POC) |
| `DB_USERNAME` | Secret `product-api-secret` | `pockub` |
| `DB_PASSWORD` | Secret `product-api-secret` | `*****` |

Detalhes: [TDD-003](TDD-003-infraestrutura-kubernetes.md)

---

## 9. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Cache de dependências Maven | A separação do `COPY pom.xml` + `dependency:go-offline` antes do `COPY src/` garante reuso do cache quando apenas o código muda |
| Usuário não-root | Obrigatório em clusters com `PodSecurity` — boa prática mesmo no Minikube |
| `imagePullPolicy: Always` | Não usar no ambiente de estudo — força pull externo desnecessário |
| JVM flags | `-XX:MaxRAMPercentage=75.0` já está no `ENTRYPOINT`; ajuste junto com os `limits` de memória |


---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [ ] Artefatos implementados conforme este documento (divergências registradas aqui)
- [ ] `docker build` conclui e a imagem `product-api:1.0.0` sobe com usuário não-root
- [ ] Imagem construída no daemon do Minikube e visível em `docker images` após `eval $(minikube docker-env)`
- [ ] Tamanho final da imagem conferido com `docker images`
- [ ] Arquivo `TDD-002-containerizacao_explain.md` gerado
- [ ] Estórias vinculadas atualizadas (US-009)
