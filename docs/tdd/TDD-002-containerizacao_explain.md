# TDD-002 — Containerização: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

| Arquivo | Descrição |
|---------|-----------|
| `application/product-api/Dockerfile` | Build multi-stage: `builder` (Maven + JDK 21) e `runtime` (JRE 21 Alpine) |
| `application/product-api/.dockerignore` | Exclui `target/`, `.git/`, `.idea/`, `*.md`, `.mvn/` do contexto de build |

Imagem gerada: `product-api:1.0.0`.

## 2. Por que cada decisão foi tomada

- **Multi-stage**: Maven e JDK completo servem só para compilar. Deixá-los no stage `builder` mantém a imagem final sem ferramentas de build (menor tamanho e menor superfície de ataque).
- **`COPY pom.xml` + `dependency:go-offline` antes de `COPY src/`**: o Docker cacheia por camada. Enquanto o `pom.xml` não muda, as dependências não são baixadas de novo; alterar só o código reaproveita essa camada.
- **`-DskipTests`**: os testes de integração usam Testcontainers (precisam de Docker). Dentro do `docker build` não há daemon disponível; os testes rodam antes, com `./mvnw test`.
- **Usuário não-root (`appuser`)**: exigido por clusters com PodSecurity e boa prática mesmo no Minikube. O JAR fica com dono root e somente leitura para o usuário, o que é desejável.
- **`-XX:MaxRAMPercentage=75.0`**: a JVM calcula o heap a partir do limite de memória do container (cgroup). Sem a flag, o padrão é 25% do limite.
- **`.dockerignore`**: evita enviar `target/` e `.git/` ao daemon (build mais rápido) e impede que um `target/` local contamine o build.

## 3. Alternativas descartadas

- **Registry (addon `registry` do Minikube) + `docker push`**: passo extra sem valor didático para a POC. Usa-se `eval $(minikube docker-env)`.
- **`imagePullPolicy: Always` / tag `latest`**: a imagem só existe no daemon do Minikube, então um pull falharia; `latest` não dá rastreabilidade.
- **Spring Boot buildpacks / Jib**: escondem o Dockerfile, que é justamente o que se quer estudar.

## 4. Pontos de atenção

- **Divergência de tamanho**: o TDD estimava ~110 MB; `docker images` mostra ~277 MB de *disk usage* (JRE Alpine + JAR do Spring Boot com Tomcat, Hibernate, drivers e Micrometer). Não há ferramentas de build na camada final; o valor estimado estava subestimado.
- **Daemon correto**: sem `eval $(minikube docker-env)` a imagem é construída no Docker local, e o kubelet do Minikube **não** a enxerga (`ErrImageNeverPull`/`ImagePullBackOff`). O shell precisa do `eval` em cada novo terminal.
- Reconstruir a imagem com a mesma tag não atualiza Pods já em execução; use uma tag nova (`1.1.0`) e `kubectl set image`/rollout.
- Variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `MONGO_URI` são obrigatórias (sem default no `application.yml`); sem elas a aplicação não sobe. Serão injetadas por ConfigMap/Secret (TDD-003).

## 5. Como validar

```bash
# Build (no daemon do Minikube, após `minikube start`)
eval $(minikube docker-env)
docker build -t product-api:1.0.0 application/product-api/

# Usuário não-root
docker run --rm --entrypoint id product-api:1.0.0     # uid=100(appuser)

# Tamanho e visibilidade
docker images product-api

# Sem ferramentas de build nas camadas finais
docker history product-api:1.0.0 | grep -i maven      # sem saída
```
