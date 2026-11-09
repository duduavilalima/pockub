# Kubernetes Learning Lab — CLAUDE.md

POC de estudo prático de Kubernetes. Foco em aprender os componentes K8s, não em regra de negócio complexa.

---

## Visão Geral

API REST de produtos implantada em Minikube, usando PostgreSQL (relacional) e MongoDB (auditoria/logs). A aplicação serve de pretexto para exercitar os recursos fundamentais do Kubernetes encontrados em ambientes corporativos.

Documentação completa: [`docs/PRD.md`](docs/PRD.md)

---

## Stack

| Camada            | Tecnologia            |
|-------------------|-----------------------|
| Linguagem         | Java 21               |
| Framework         | Spring Boot 4         |
| Banco relacional  | PostgreSQL 17         |
| Banco NoSQL       | MongoDB               |
| Build             | Maven                 |
| Container         | Docker                |
| Orquestração      | Kubernetes / Minikube |
| Empacotamento K8s | Helm                  |
| Observabilidade   | Prometheus + Grafana  |

Detalhes: [`docs/STACK.md`](docs/STACK.md)

---

## Estrutura do Repositório

```
pockub/
├── CLAUDE.md                  ← este arquivo
├── AGENTS.md                  ← symlink para CLAUDE.md
│
├── docs/
│   ├── PRD.md                 ← visão geral, roadmap, cenários, critérios de sucesso
│   ├── STACK.md               ← stack tecnológica
│   ├── RULES.md               ← requisitos funcionais e contrato de API
│   ├── MODEL.md               ← modelo de dados (PostgreSQL + MongoDB)
│   ├── adr/                   ← Architecture Decision Records
│   ├── tdd/                   ← Technical Design Documents (TDD-001 a TDD-008)
│   └── stories/               ← estórias de usuário
│
├── application/
│   └── product-api/           ← Java 21 + Spring Boot 4
│
├── docker/                    ← reservado (o Dockerfile fica em application/product-api/)
│
├── k8s/
│   ├── namespace/
│   ├── configmaps/
│   ├── secrets/
│   ├── deployments/
│   ├── services/
│   ├── ingress/
│   ├── postgresql/
│   ├── mongodb/
│   ├── daemonsets/
│   ├── jobs/
│   ├── cronjobs/
│   ├── hpa/
│   └── networkpolicies/
│
├── monitoring/
│   ├── prometheus/
│   └── grafana/
│
└── helm/
    └── kubernetes-learning-lab/
```

---

## Namespaces Kubernetes

| Namespace      | Conteúdo                                                  |
|----------------|-----------------------------------------------------------|
| `learning-lab` | Product API, PostgreSQL, MongoDB, DaemonSet, Job, CronJob |
| `monitoring`   | Prometheus, Grafana                                       |

---

## API — Endpoints

| Método | Endpoint             | Descrição                    | Status          |
|--------|----------------------|------------------------------|-----------------|
| POST   | /produtos            | Criar produto                | 201 / 400       |
| PUT    | /produtos/{id}       | Atualizar produto            | 200 / 400 / 404 |
| DELETE | /produtos/{id}       | Excluir produto              | 204 / 404       |
| GET    | /produtos/{id}       | Consultar produto + gera log | 200 / 404       |
| GET    | /produtos            | Listar produtos + gera log   | 200             |
| GET    | /logs                | Listar logs de acesso        | 200             |
| GET    | /logs?produtoId={id} | Filtrar logs por produto     | 200 / 400       |

Detalhes e exemplos: [`docs/RULES.md`](docs/RULES.md)

---

## Modelo de Dados

**Produto** → PostgreSQL, tabela `produtos`
**LogAcesso** → MongoDB, collection `access_logs`

`LogAcesso.nomeProduto` é snapshot imutável — não reflete renomeações posteriores (intencional para auditoria).

Detalhes, constraints e mapeamento Java: [`docs/MODEL.md`](docs/MODEL.md)

---

## Decisões de Arquitetura (ADRs)

| ADR                                                   | Decisão                                        |
|-------------------------------------------------------|------------------------------------------------|
| [ADR-001](docs/adr/ADR-001-minikube-local-cluster.md) | Minikube como cluster local                    |
| [ADR-002](docs/adr/ADR-002-java-spring-boot.md)       | Java 21 e Spring Boot 4                        |
| [ADR-003](docs/adr/ADR-003-postgresql-relacional.md)  | PostgreSQL para persistência relacional        |
| [ADR-004](docs/adr/ADR-004-mongodb-nosql.md)          | MongoDB para logs de acesso                    |
| [ADR-005](docs/adr/ADR-005-dual-namespace.md)         | Dois namespaces: `learning-lab` e `monitoring` |
| [ADR-006](docs/adr/ADR-006-statefulset-databases.md)  | StatefulSet para bancos de dados               |
| [ADR-007](docs/adr/ADR-007-prometheus-grafana.md)     | Prometheus e Grafana para observabilidade      |

---

## Componentes Kubernetes Demonstrados

| Componente               | Aplicado em                                |
|--------------------------|--------------------------------------------|
| Deployment               | Product API                                |
| StatefulSet              | PostgreSQL, MongoDB                        |
| DaemonSet                | Log Collector                              |
| Job                      | Seed de dados (PostgreSQL)                 |
| CronJob                  | Limpeza de logs antigos (MongoDB)          |
| Service ClusterIP        | Comunicação interna API → bancos           |
| Service NodePort         | Exposição externa da API (pré-Ingress)     |
| Ingress                  | Entrada única com roteamento HTTP          |
| ConfigMap                | URLs, configurações, feature flags         |
| Secret                   | Credenciais dos bancos                     |
| PV + PVC                 | Persistência de PostgreSQL e MongoDB       |
| HPA                      | Escalabilidade automática da API           |
| Liveness Probe           | `/actuator/health/liveness`                |
| Readiness Probe          | `/actuator/health/readiness`               |
| Startup Probe            | Tolerância ao startup lento da JVM         |
| Resource Requests/Limits | Todos os containers — pré-requisito do HPA |
| NetworkPolicy            | Isolamento de PostgreSQL e MongoDB         |
| Helm Chart               | Empacotamento completo da plataforma       |

---

## Roadmap de Fases

| Fase            | Objetivo                | Entregável                                |
|-----------------|-------------------------|-------------------------------------------|
| 1               | Fundamentos K8s         | Minikube + kubectl operacionais           |
| 2               | Aplicação               | Imagem Docker da Product API              |
| 3               | Deploy                  | API no K8s via Deployment + Service       |
| 4               | Persistência            | PostgreSQL e MongoDB como StatefulSets    |
| 5               | Configuração            | ConfigMaps e Secrets                      |
| 6               | Confiabilidade          | Probes + Resource Requests/Limits         |
| 7               | Escalabilidade          | HPA + Metrics Server                      |
| 8               | Exposição externa       | Ingress Controller                        |
| 9               | Operação                | Job, CronJob, DaemonSet, kubectl logs/top |
| 10              | Observabilidade         | Prometheus + Grafana                      |
| 11              | Segurança de rede       | NetworkPolicy                             |
| 12              | Helm                    | Chart completo da plataforma              |
| 13              | Simulação de incidentes | Falhas, rollbacks, troubleshooting        |
| 14 *(opcional)* | Terraform (pós-POC)     | Provisionar cluster EKS/GKE/AKS em cloud  |

---

## Comandos Úteis

```bash
# Iniciar cluster
# --cni=calico é necessário para a NetworkPolicy ser aplicada (Fase 11)
minikube start --memory=6144 --cpus=4 --cni=calico
minikube addons enable ingress metrics-server registry

# Aplicar namespace
kubectl apply -f k8s/namespace/

# Ver pods de todos os namespaces
kubectl get pods -A

# Pods da aplicação
kubectl get pods -n learning-lab

# Pods de monitoring
kubectl get pods -n monitoring

# Logs da API
kubectl logs -f deployment/product-api -n learning-lab

# Métricas de recursos
kubectl top pods -n learning-lab
kubectl top nodes

# HPA
kubectl get hpa -n learning-lab

# Instalar via Helm
helm install learning-lab helm/kubernetes-learning-lab/ -n learning-lab
```

---

## Regras de Workflow

### Rastreabilidade, DoR e DoD

Toda a documentação de trabalho segue a cadeia abaixo. Nenhum artefato fica "órfão".

```
PRD (Fase / Cenário / RF)  ←──  Estória (US-xxx)  ←──►  TDD-xxx  ──►  implementação + _explain
```

**Regras de referência (obrigatórias, nos dois sentidos):**

1. Toda **estória** referencia: o RF em `docs/RULES.md` (quando houver), ao menos uma **Fase** e um **Cenário** do `docs/PRD.md`, e o **TDD** que a implementa (ou `a criar`).
2. Todo **TDD** referencia as **estórias** (`Estórias:`) e a **Fase/Cenário** do PRD (`PRD:`) que ele atende, além de ADRs e outros TDDs.
3. A referência é bidirecional: se o TDD cita `US-009`, a `US-009` cita o TDD. Ao criar ou alterar um lado, atualize o outro.
4. Toda Fase do PRD deve ter ao menos uma estória (exceto a Fase 1, setup de ambiente, e a Fase 14, opcional e pós-POC); a matriz de rastreabilidade fica em `docs/PRD.md` (seção "Matriz de Rastreabilidade").
5. Links usam caminhos relativos e âncoras válidas; não deixe link quebrado.

**Ciclo de status** (campo `**Status:**` no cabeçalho de estórias e TDDs):

| Status        | Significado                                                                  |
|---------------|------------------------------------------------------------------------------|
| `Draft`       | Em escrita ou revisão; DoR ainda não atendido                                |
| `Ready`       | DoR atendido; pode ser implementado                                          |
| `In Progress` | Implementação iniciada, DoD ainda não atendido                               |
| `Done`        | DoD atendido e validado                                                      |

O status da estória acompanha o do TDD: a estória só vai a `Done` quando todos os seus TDDs estiverem `Done`.
Só marque `[x]` num critério de aceite depois de executar a validação correspondente.

**Definition of Ready (DoR)**

- *Estória:* formato Como/quero/para; critérios de aceite verificáveis; referências ao PRD (Fase e Cenário) e ao TDD preenchidas.
- *TDD:* ao menos uma estória e uma Fase/Cenário do PRD referenciadas; ADRs relevantes existentes; dependências de outros TDDs listadas; seção de riscos preenchida.

**Definition of Done (DoD)**

- *Estória:* todos os critérios de aceite marcados `[x]` após validação; TDDs vinculados `Done`; Cenários do PRD correspondentes executados.
- *TDD:* artefatos implementados conforme o documento (divergências registradas no próprio TDD); validação executada (testes automatizados verdes para código; `kubectl apply --dry-run=server`, `helm lint` ou cenário do PRD para manifests); arquivo `_explain` gerado (regra abaixo); estórias vinculadas atualizadas.

Cada estória e cada TDD traz suas seções `Definition of Ready` e `Definition of Done`, com os itens específicos do artefato.

### Explicação pós-implementação de TDD

Após implementar qualquer TDD (ex: `TDD-001-product-api.md`), gere obrigatoriamente um arquivo de explicação com o sufixo `_explain` no mesmo diretório:

```
docs/tdd/TDD-001-product-api_explain.md
```

**Conteúdo obrigatório do arquivo `_explain`:**

1. **O que foi implementado** — resumo dos artefatos criados ou modificados (arquivos, classes, manifests)
2. **Por que cada decisão foi tomada** — justificativa técnica de cada escolha de implementação
3. **Alternativas descartadas** — o que foi considerado e por que não foi adotado
4. **Pontos de atenção** — armadilhas, comportamentos não óbvios, dependências de ordem ou configuração
5. **Como validar** — comandos ou passos para verificar que a implementação está correta

**Objetivo:** este é um projeto de estudo. O arquivo `_explain` serve como material de aprendizado — deve ser escrito como se o leitor estivesse vendo o componente pela primeira vez.

---

## Observabilidade

- `GET /actuator/health` — liveness e readiness
- `GET /actuator/prometheus` — métricas para scraping
- Prometheus: namespace `monitoring`, porta padrão 9090
- Grafana: namespace `monitoring`, porta padrão 3000

Dashboards sugeridos: JVM Overview, HTTP Rate/Latência, CPU/Memória dos Pods.
