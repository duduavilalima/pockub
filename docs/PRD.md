# Kubernetes Learning Lab

## Documento de Requisitos do Produto (PRD)

### Versão

1.1

### Status

Draft

### Autor

Equipe de Estudos Kubernetes

### Data

Junho/2026

---

## Documentos Relacionados

| Documento             | Conteúdo                                |
|-----------------------|-----------------------------------------|
| [STACK.md](STACK.md)  | Stack tecnológica completa              |
| [RULES.md](RULES.md)  | Requisitos funcionais e contrato de API |
| [MODEL.md](MODEL.md)  | Modelo de dados (PostgreSQL e MongoDB)  |
| [tdd/](tdd/README.md) | Technical Design Documents              |
| [adr/](adr/)          | Architecture Decision Records           |
| [stories/](stories/)  | Estórias de usuário                     |

### ADRs

| ADR                                              | Decisão                                        |
|--------------------------------------------------|------------------------------------------------|
| [ADR-001](adr/ADR-001-minikube-local-cluster.md) | Minikube como cluster local                    |
| [ADR-002](adr/ADR-002-java-spring-boot.md)       | Java 21 e Spring Boot 4                        |
| [ADR-003](adr/ADR-003-postgresql-relacional.md)  | PostgreSQL para persistência relacional        |
| [ADR-004](adr/ADR-004-mongodb-nosql.md)          | MongoDB para logs de acesso                    |
| [ADR-005](adr/ADR-005-dual-namespace.md)         | Dois namespaces: `learning-lab` e `monitoring` |
| [ADR-006](adr/ADR-006-statefulset-databases.md)  | StatefulSet para bancos de dados               |
| [ADR-007](adr/ADR-007-prometheus-grafana.md)     | Prometheus e Grafana para observabilidade      |

### Estórias de Usuário

| ID                                                 | Estória                    |
|----------------------------------------------------|----------------------------|
| [US-001](stories/US-001-criar-produto.md)          | Criar Produto              |
| [US-002](stories/US-002-atualizar-produto.md)      | Atualizar Produto          |
| [US-003](stories/US-003-excluir-produto.md)        | Excluir Produto            |
| [US-004](stories/US-004-consultar-produto.md)      | Consultar Produto          |
| [US-005](stories/US-005-listar-produtos.md)        | Listar Produtos            |
| [US-006](stories/US-006-registrar-log-consulta.md) | Registro Automático de Log |
| [US-007](stories/US-007-consultar-logs.md)         | Consultar Logs de Acesso   |
| [US-008](stories/US-008-filtrar-logs-produto.md)   | Filtrar Logs por Produto   |
| [US-009](stories/US-009-containerizar-api.md) | Containerizar a Product API |
| [US-010](stories/US-010-infraestrutura-kubernetes-base.md) | Implantar a infraestrutura Kubernetes base |
| [US-011](stories/US-011-persistencia-statefulsets.md) | Persistir dados com StatefulSets |
| [US-012](stories/US-012-confiabilidade-escalabilidade.md) | Garantir confiabilidade e escalabilidade da API |
| [US-013](stories/US-013-workloads-operacionais.md) | Executar workloads operacionais (Job, CronJob, DaemonSet) |
| [US-014](stories/US-014-observabilidade.md) | Observar a plataforma com Prometheus e Grafana |
| [US-015](stories/US-015-helm-chart.md) | Empacotar a plataforma em Helm Chart |
| [US-016](stories/US-016-networkpolicy.md) | Isolar os bancos com NetworkPolicy |
| [US-017](stories/US-017-simulacao-incidentes.md) | Simular incidentes e praticar troubleshooting |

### Matriz de Rastreabilidade

Fase do PRD → Estórias → TDDs. Regras e DoR/DoD em [CLAUDE.md](../CLAUDE.md#rastreabilidade-dor-e-dod).

| Fase | Estórias | TDDs |
|------|----------|------|
| [Fase 1 — Fundamentos Kubernetes](#fase-1--fundamentos-kubernetes) | — (setup de ambiente) | — |
| [Fase 2 — Aplicação](#fase-2--aplicação) | [US-001](stories/US-001-criar-produto.md), [US-002](stories/US-002-atualizar-produto.md), [US-003](stories/US-003-excluir-produto.md), [US-004](stories/US-004-consultar-produto.md), [US-005](stories/US-005-listar-produtos.md), [US-006](stories/US-006-registrar-log-consulta.md), [US-007](stories/US-007-consultar-logs.md), [US-008](stories/US-008-filtrar-logs-produto.md), [US-009](stories/US-009-containerizar-api.md) | [TDD-001](tdd/TDD-001-product-api.md), [TDD-002](tdd/TDD-002-containerizacao.md) |
| [Fase 3 — Deploy da Aplicação](#fase-3--deploy-da-aplicação) | [US-010](stories/US-010-infraestrutura-kubernetes-base.md) | [TDD-003](tdd/TDD-003-infraestrutura-kubernetes.md) |
| [Fase 4 — Persistência](#fase-4--persistência) | [US-011](stories/US-011-persistencia-statefulsets.md) | [TDD-004](tdd/TDD-004-persistencia.md) |
| [Fase 5 — Configuração](#fase-5--configuração) | [US-010](stories/US-010-infraestrutura-kubernetes-base.md) | [TDD-003](tdd/TDD-003-infraestrutura-kubernetes.md) |
| [Fase 6 — Confiabilidade](#fase-6--confiabilidade) | [US-012](stories/US-012-confiabilidade-escalabilidade.md) | [TDD-005](tdd/TDD-005-escalabilidade-confiabilidade.md) |
| [Fase 7 — Escalabilidade](#fase-7--escalabilidade) | [US-012](stories/US-012-confiabilidade-escalabilidade.md) | [TDD-005](tdd/TDD-005-escalabilidade-confiabilidade.md) |
| [Fase 8 — Exposição Externa](#fase-8--exposição-externa) | [US-010](stories/US-010-infraestrutura-kubernetes-base.md) | [TDD-003](tdd/TDD-003-infraestrutura-kubernetes.md) |
| [Fase 9 — Operação](#fase-9--operação) | [US-013](stories/US-013-workloads-operacionais.md) | [TDD-006](tdd/TDD-006-workloads-especiais.md) |
| [Fase 10 — Observabilidade Avançada](#fase-10--observabilidade-avançada) | [US-014](stories/US-014-observabilidade.md) | [TDD-007](tdd/TDD-007-observabilidade.md) |
| [Fase 11 — Segurança de Rede](#fase-11--segurança-de-rede) | [US-016](stories/US-016-networkpolicy.md) | a criar |
| [Fase 12 — Helm](#fase-12--helm) | [US-015](stories/US-015-helm-chart.md) | [TDD-008](tdd/TDD-008-helm.md) |
| [Fase 13 — Simulação de Incidentes](#fase-13--simulação-de-incidentes) | [US-017](stories/US-017-simulacao-incidentes.md) | a criar |
| [Fase 14 — Terraform *(opcional — pós-POC)*](#fase-14--terraform-opcional--pós-poc) | — (opcional, fora do escopo da POC) | — |

---

# 1. Visão Geral

## Objetivo

Desenvolver uma Prova de Conceito (POC) para estudo prático de Kubernetes utilizando Minikube, demonstrando os principais recursos da plataforma por meio de uma API REST desenvolvida em Java 21 e Spring Boot 4.

A aplicação utilizará simultaneamente:

* PostgreSQL 17 para armazenamento relacional.
* MongoDB para armazenamento NoSQL.
* Kubernetes como plataforma de orquestração.
* Docker como runtime de containers.

O objetivo principal não é a complexidade da regra de negócio, mas sim o aprendizado progressivo dos componentes fundamentais do Kubernetes encontrados em ambientes corporativos.

---

# 2. Objetivos de Aprendizagem

Ao concluir esta POC, o desenvolvedor deverá compreender:

## Kubernetes Fundamentals

* Cluster
* Node
* Pod
* Container
* Namespace
* Labels
* Selectors

## Workloads

* Deployment
* ReplicaSet
* StatefulSet
* DaemonSet
* Job
* CronJob

## Networking

* Service
* ClusterIP
* NodePort
* LoadBalancer (simulado)
* Ingress
* NetworkPolicy

## Storage

* Persistent Volume (PV)
* Persistent Volume Claim (PVC)
* Storage Class

## Configuration

* ConfigMap
* Secret

## Reliability

* Liveness Probe
* Readiness Probe
* Startup Probe
* Resource Requests e Limits

## Scaling

* Escalabilidade manual
* Horizontal Pod Autoscaler (HPA)

## Observability

* Logs
* Métricas
* Monitoramento
* Dashboards

## DevOps

* Docker
* Rolling Updates
* Rollbacks
* Estratégias de Deploy
* Helm

---

# 3. Escopo da Solução

A solução será composta por uma única aplicação denominada:

## Product API

Responsabilidades:

* CRUD de Produtos
* Consulta de Produtos
* Registro automático de Logs de Acesso
* Consulta de Logs de Acesso

---

# 4. Stack Tecnológica

Java 21, Spring Boot 4, PostgreSQL 17, MongoDB 7, Maven, Docker, Kubernetes/Minikube e Helm. Versões, papéis e ferramentas de observabilidade: [STACK.md](STACK.md).

---

# 5. Arquitetura Funcional

## Diagrama de Componentes

```
                        [ Cliente ]
                             │
                        HTTP/HTTPS
                             │
                    [ Ingress Controller ]
                    namespace: ingress-nginx
                             │
                    [ Service: NodePort ]
                             │
                    ┌────────┴────────┐
                    │   Product API   │
                    │   (Deployment)  │
                    │   port: 8080    │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
   [ Service: ClusterIP ]       [ Service: ClusterIP ]
   postgresql:5432               mongodb:27017
              │                             │
   ┌──────────┴──────────┐    ┌─────────────┴─────────────┐
   │     PostgreSQL       │    │          MongoDB           │
   │   (StatefulSet)      │    │       (StatefulSet)        │
   │    port: 5432        │    │        port: 27017         │
   └──────────┬──────────┘    └─────────────┬─────────────┘
              │                             │
           [ PVC ]                       [ PVC ]
   postgres-data-postgresql-0     mongodb-data-mongodb-0


[ Monitoring - namespace: monitoring ]

   [ Prometheus ] ←── scrape ──── [ Product API /actuator/prometheus ]
         │
   [ Grafana ] ←── datasource ─── [ Prometheus ]


[ DaemonSet: log-collector ]
   Executa em todos os nodes — coleta logs dos containers
```

---

## Fluxo de Cadastro

```
Cliente → Ingress → Service (NodePort) → Product API → PostgreSQL
```

---

## Fluxo de Consulta

```
Cliente → Ingress → Service (NodePort) → Product API → PostgreSQL
                                                     ↓
                                               MongoDB (LogAcesso)
```

---

## Fluxo de Consulta de Logs

```
Cliente → Ingress → Service (NodePort) → Product API → MongoDB
```

---

## Namespaces

Ver [ADR-005](adr/ADR-005-dual-namespace.md) (`learning-lab`, `monitoring` e `ingress-nginx`).

---

# 6. Requisitos Funcionais

Contrato detalhado (endpoints, validações, status HTTP) em [RULES.md](RULES.md); cada requisito tem uma estória.

| RF | Requisito | Estória |
|----|-----------|---------|
| [RF-001](RULES.md#rf-001--criar-produto) | Criar produto | [US-001](stories/US-001-criar-produto.md) |
| [RF-002](RULES.md#rf-002--atualizar-produto) | Atualizar produto | [US-002](stories/US-002-atualizar-produto.md) |
| [RF-003](RULES.md#rf-003--excluir-produto) | Excluir produto | [US-003](stories/US-003-excluir-produto.md) |
| [RF-004](RULES.md#rf-004--consultar-produto) | Consultar produto (gera log) | [US-004](stories/US-004-consultar-produto.md) |
| [RF-005](RULES.md#rf-005--listar-produtos) | Listar produtos (gera log) | [US-005](stories/US-005-listar-produtos.md) |
| [RF-006](RULES.md#rf-006--registrar-log-de-consulta) | Registrar log de consulta (interno) | [US-006](stories/US-006-registrar-log-consulta.md) |
| [RF-007](RULES.md#rf-007--consultar-logs) | Consultar logs | [US-007](stories/US-007-consultar-logs.md) |
| [RF-008](RULES.md#rf-008--filtrar-logs-por-produto) | Filtrar logs por produto | [US-008](stories/US-008-filtrar-logs-produto.md) |

---

# 7. Modelo de Dados

`Produto` (PostgreSQL, tabela `produtos`) e `LogAcesso` (MongoDB, collection `access_logs`): campos, constraints, índices e mapeamento Java em [MODEL.md](MODEL.md).

---

# 8. Arquitetura Kubernetes

## Componentes

### Product API

| Atributo | Valor |
|----------|-------|
| Tipo | Deployment |
| Réplicas iniciais | 1 |
| Porta | 8080 |
| Resources | requests: cpu=250m, memory=256Mi / limits: cpu=500m, memory=512Mi |
| Objetivo | Aplicação stateless escalável |

---

### PostgreSQL

| Atributo  | Valor                                                             |
|-----------|-------------------------------------------------------------------|
| Tipo      | StatefulSet                                                       |
| Réplicas  | 1                                                                 |
| Porta     | 5432                                                              |
| Resources | iguais aos da Product API |
| Objetivo  | Persistência relacional                                           |

---

### MongoDB

| Atributo  | Valor                                                             |
|-----------|-------------------------------------------------------------------|
| Tipo      | StatefulSet                                                       |
| Réplicas  | 1                                                                 |
| Porta     | 27017                                                             |
| Resources | iguais aos da Product API |
| Objetivo  | Persistência NoSQL                                                |

---

### Log Collector

| Atributo | Valor                                       |
|----------|---------------------------------------------|
| Tipo     | DaemonSet                                   |
| Objetivo | Coleta de logs em todos os nodes do cluster |

---

### Seed de Dados

| Atributo | Valor                                                    |
|----------|----------------------------------------------------------|
| Tipo     | Job                                                      |
| Objetivo | Popular PostgreSQL com dados iniciais para demonstrações |

---

### Limpeza de Logs

| Atributo | Valor                                             |
|----------|---------------------------------------------------|
| Tipo     | CronJob                                           |
| Schedule | `0 2 * * *` (diário às 02h)                       |
| Objetivo | Remover LogAcessos com mais de 30 dias do MongoDB |

---

# 9. Recursos Kubernetes a Serem Demonstrados

## Pods

Objetivos:

* Criação
* Inspeção
* Troubleshooting

---

## Deployments

Objetivos:

* Gerenciamento de versões
* Rolling Update
* Rollback

---

## ReplicaSets

Objetivos:

* Alta disponibilidade
* Recuperação automática

---

## StatefulSets

Objetivos:

* Persistência
* Identidade estável
* Bancos de dados

Aplicações:

* PostgreSQL
* MongoDB

---

## DaemonSet

Objetivos:

* Garantir execução em todos os nodes
* Padrão para agentes de infraestrutura (log collectors, monitoring agents)

Aplicação:

* Log Collector

---

## Jobs

Objetivos:

* Executar tarefas únicas até conclusão
* Padrão para migrations e seeds

Exemplos:

* Seed de dados iniciais no PostgreSQL
* Inicialização de ambiente

---

## CronJobs

Objetivos:

* Executar tarefas agendadas
* Padrão para rotinas de manutenção

Exemplos:

* Limpeza de logs antigos no MongoDB

---

## Services

### ClusterIP

Comunicação interna entre Pods (API → PostgreSQL, API → MongoDB).

---

### NodePort

Exposição da API externamente durante os estudos (antes do Ingress).

---

## Ingress

Objetivos:

* Entrada única para o cluster
* Roteamento HTTP por path ou host
* Substituir NodePort em ambientes corporativos

---

## Labels e Selectors

Objetivos:

* Compreender como Services selecionam Pods via `selector`
* Filtrar recursos com `kubectl get pods -l app=product-api`
* Entender o papel dos labels em Deployments, Services e HPA

---

## ConfigMaps

Objetivos:

Externalizar:

* URLs de banco de dados
* Configurações da aplicação
* Feature Flags

---

## Secrets

Objetivos:

Armazenar com segurança:

* Senha PostgreSQL
* Senha MongoDB
* Credenciais futuras

---

## Persistent Volumes

Objetivos:

* Compreender o ciclo de vida do armazenamento no Kubernetes
* Persistência dos bancos de dados

---

## Persistent Volume Claims

Objetivos:

* Consumo desacoplado de armazenamento
* Entender `StorageClass` e provisionamento dinâmico

---

## Resource Requests e Limits

Objetivos:

* Definir `resources.requests.cpu` e `resources.requests.memory` para todos os containers
* Compreender a diferença entre requests (scheduling) e limits (enforcement)
* Pré-requisito obrigatório para o HPA funcionar corretamente

---

## Health Checks

### Liveness Probe

Detectar travamentos — reinicia o container automaticamente.

### Readiness Probe

Controlar recebimento de tráfego — remove o Pod do Service enquanto não estiver pronto.

### Startup Probe

Controlar inicialização lenta — evita que Liveness mate um container ainda inicializando.

---

## Horizontal Pod Autoscaler

Objetivos:

* Escalabilidade automática baseada em uso de CPU
* Depende de `resources.requests.cpu` definidos
* Verificar com `kubectl get hpa`

---

## NetworkPolicy

Objetivos:

* Restringir tráfego entre Pods por namespace e labels
* Isolar PostgreSQL e MongoDB para aceitar conexões apenas da Product API
* Padrão de segurança em ambientes corporativos

---

# 10. PostgreSQL

## Objetivos de Aprendizagem

Demonstrar:

* StatefulSet
* PVC e StorageClass
* ConfigMap para URL e configurações
* Secret para credenciais
* Persistência relacional após reinicialização

---

## Dados Armazenados

Produtos.

---

## Validações

* Dados preservados após reinicialização do Pod
* Conexão com a Product API via ClusterIP
* Recuperação automática pelo StatefulSet

---

# 11. MongoDB

## Objetivos de Aprendizagem

Demonstrar:

* StatefulSet
* Persistência NoSQL
* Armazenamento orientado a documentos

---

## Collection

```text
access_logs
```

---

## Dados Armazenados

Logs de consulta aos produtos (LogAcesso).

---

## Validações

* Logs preservados após reinicialização do Pod
* Integração com a Product API via ClusterIP
* Recuperação automática pelo StatefulSet

---

# 12. Observabilidade

## Spring Boot Actuator

Endpoints habilitados:

* `/actuator/health` — Health Checks (Liveness e Readiness Probes)
* `/actuator/metrics` — Métricas da JVM e da aplicação
* `/actuator/prometheus` — Métricas no formato Prometheus (scrape target)
* `/actuator/info` — Informações da aplicação

---

## Logs

Objetivos:

* `kubectl logs <pod>` — visualização em tempo real
* `kubectl logs <pod> --previous` — logs do container anterior (troubleshooting)
* Structured logging em JSON para facilitar parsing

---

## Metrics Server

Objetivos:

Monitorar consumo de recursos no cluster:

```bash
kubectl top pod -n learning-lab
kubectl top node
```

Pré-requisito para HPA.

---

## Prometheus

Namespace: `monitoring`

Objetivos:

* Coletar métricas via scrape do `/actuator/prometheus`
* Armazenamento temporal de séries históricas
* Base para alertas futuros

---

## Grafana

Namespace: `monitoring`

Objetivos:

* Dashboards de métricas da aplicação e do cluster
* Datasource: Prometheus

Dashboards sugeridos:

* JVM Overview (heap, threads, GC)
* HTTP Request Rate e Latência
* CPU e Memória dos Pods

---

# 13. Cenários de Demonstração

## Cenário 1 — Deploy Completo da Plataforma

Ação: Aplicar todos os manifests do namespace `learning-lab`.

Validação:

* `kubectl get pods -n learning-lab` — todos os Pods em `Running`
* Product API respondendo na porta configurada
* PostgreSQL e MongoDB acessíveis pela API

---

## Cenário 2 — CRUD de Produtos

Ação: Executar criação, leitura, atualização e exclusão via API.

Validação:

* Persistência correta no PostgreSQL após cada operação
* Retorno dos status HTTP esperados (201, 200, 204, 404)

---

## Cenário 3 — Consulta com Auditoria

Ação: Consultar um produto via `GET /produtos/{id}`.

Validação:

* Produto retornado com status 200
* Log criado no MongoDB com `operacao: CONSULTA`

---

## Cenário 4 — Persistência PostgreSQL

Ação: Reiniciar o Pod do PostgreSQL com `kubectl delete pod`.

Validação:

* StatefulSet recria o Pod automaticamente
* Dados dos produtos preservados após a reinicialização

---

## Cenário 5 — Persistência MongoDB

Ação: Reiniciar o Pod do MongoDB com `kubectl delete pod`.

Validação:

* StatefulSet recria o Pod automaticamente
* Logs de acesso preservados após a reinicialização

---

## Cenário 6 — Recuperação Automática de Pod

Ação: Excluir o Pod da Product API manualmente com `kubectl delete pod`.

Validação:

* Deployment recria o Pod automaticamente
* API volta a responder sem intervenção manual

---

## Cenário 7 — Escalabilidade Manual

Ação: `kubectl scale deployment product-api --replicas=3 -n learning-lab`

Validação:

* 3 réplicas em execução (`kubectl get pods`)
* Balanceamento de carga distribuindo requisições entre os Pods

---

## Cenário 8 — Horizontal Pod Autoscaler

Ação: Gerar carga com `kubectl run -i --tty load-generator --image=busybox`.

Validação:

* HPA detecta CPU acima do threshold configurado
* Número de réplicas aumenta automaticamente (`kubectl get hpa -n learning-lab`)
* Réplicas reduzidas após cessação da carga

---

## Cenário 9 — Rolling Update

Ação: Atualizar a imagem Docker com `kubectl set image deployment/product-api`.

Validação:

* Deploy sem downtime — requisições contínuas durante a atualização
* `kubectl rollout status deployment/product-api` mostra progresso

---

## Cenário 10 — Rollback

Ação: Implantar versão com falha intencional e executar `kubectl rollout undo`.

Validação:

* Kubernetes reverte para a versão anterior automaticamente
* API volta a responder corretamente

---

## Cenário 11 — ConfigMap

Ação: Alterar um valor no ConfigMap e aplicar com `kubectl apply`.

Validação:

* Nova configuração propagada para os Pods
* Comportamento da aplicação reflete a mudança

---

## Cenário 12 — Secrets

Ação: Inspecionar o Secret criado para as credenciais do banco.

Validação:

* Valor armazenado em Base64, não em texto plano
* Credencial não exposta em variáveis de ambiente visíveis no Pod spec

---

## Cenário 13 — Observabilidade

Ação: Acessar Prometheus e Grafana no namespace `monitoring`.

Validação:

* Prometheus coletando métricas do endpoint `/actuator/prometheus`
* Grafana exibindo dashboards de JVM, HTTP e recursos do cluster

---

## Cenário 14 — Job (Seed de Dados)

Ação: Criar e executar um Job K8s que insere produtos iniciais no PostgreSQL.

Validação:

* `kubectl get job -n learning-lab` mostra o Job com status `Complete`
* Produtos visíveis via `GET /produtos`

---

## Cenário 15 — CronJob (Limpeza de Logs)

Ação: Disparar o CronJob manualmente com `kubectl create job --from=cronjob/log-cleanup`.

Validação:

* Job criado e executado com sucesso
* Logs com mais de 30 dias removidos do MongoDB

---

## Cenário 16 — DaemonSet (Log Collector)

Ação: Inspecionar o DaemonSet com `kubectl get daemonset -n learning-lab`.

Validação:

* Um Pod do DaemonSet em execução por node do cluster
* Logs dos containers coletados e disponíveis

---

## Cenário 17 — Labels e Selectors

Ação: Usar `kubectl get pods -l app=product-api -n learning-lab`.

Validação:

* Apenas os Pods da Product API retornados
* Demonstrar como o Service usa `selector` para rotear tráfego

---

## Cenário 18 — NetworkPolicy

Ação: Aplicar NetworkPolicy restringindo acesso ao PostgreSQL.

Validação:

* Product API consegue conectar ao PostgreSQL
* Qualquer outro Pod bloqueado de acessar a porta 5432
* MongoDB acessível apenas pela Product API e pelo CronJob `log-cleanup`
* Pré-requisito: cluster iniciado com `--cni=calico`

---

# 14. Estrutura Esperada do Repositório

```text
kubernetes-learning-lab/

docs/
├── PRD.md
├── STACK.md
├── RULES.md
├── MODEL.md
├── adr/
├── stories/
└── tdd/

application/
└── product-api/

docker/                    ← reservado (o Dockerfile fica em application/product-api/)

k8s/
├── namespace/
├── configmaps/
├── secrets/
├── deployments/
├── services/
├── ingress/
├── postgresql/
├── mongodb/
├── daemonsets/
├── jobs/
├── cronjobs/
├── hpa/
└── networkpolicies/

monitoring/
├── prometheus/
└── grafana/

helm/
└── kubernetes-learning-lab/
    ├── Chart.yaml
    ├── values.yaml
    └── templates/
```

---

# 15. Critérios de Sucesso

A POC será considerada concluída quando:

| Critério | Validação Mensurável |
|----------|---------------------|
| Product API executando | `kubectl get pods` mostra status `Running` |
| PostgreSQL persistindo produtos | Dados presentes após `kubectl delete pod` no StatefulSet |
| MongoDB persistindo logs | Logs presentes após `kubectl delete pod` no StatefulSet |
| Log automático em consultas | Cada `GET /produtos/{id}` gera documento no MongoDB |
| ConfigMaps funcionando | Alteração no ConfigMap refletida na aplicação sem rebuild |
| Secrets funcionando | Credenciais em Base64, não expostas em texto plano |
| StatefulSets funcionando | Pods recriados com mesmo nome e PVC após falha |
| Volumes persistentes funcionando | Dados sobrevivem ao ciclo de vida dos Pods |
| Ingress funcionando | Acesso via host/path sem NodePort direto |
| Rolling Update sem downtime | Requisições contínuas durante deploy sem erro |
| Rollback funcionando | `kubectl rollout undo` restaura versão anterior em < 60s |
| Health Checks funcionando | Pod removido do Service durante falha de Readiness |
| Escalabilidade manual | `kubectl scale` cria/remove réplicas conforme esperado |
| HPA funcionando | Escala de 1 para N réplicas com CPU > 50% por 30s |
| Prometheus coletando métricas | Target `product-api` com status `UP` no Prometheus UI |
| Grafana exibindo dashboards | Métricas de JVM e HTTP visíveis em tempo real |
| Job concluindo | Status `Complete` após seed de dados |
| CronJob executando | Limpeza executada e verificável no MongoDB |
| DaemonSet em todos os nodes | Um Pod por node em `kubectl get daemonset` |
| NetworkPolicy isolando bancos | Conexão ao PostgreSQL bloqueada de Pods não autorizados |

---

# 16. Roadmap de Implementação

## Fase 1 — Fundamentos Kubernetes

Objetivos:

* Instalar e configurar Minikube
* Instalar Docker e kubectl
* Criar e inspecionar Pods manualmente

Entregáveis:

* Ambiente operacional com Minikube rodando

---

## Fase 2 — Aplicação

Objetivos:

* Desenvolver Product API (Java 21 + Spring Boot 4)
* Dockerizar a aplicação
* Construir a imagem diretamente no daemon Docker do Minikube (`minikube docker-env`)

Entregáveis:

* Imagem Docker funcional acessível pelo cluster

---

## Fase 3 — Deploy da Aplicação

Objetivos:

* Criar Deployment para Product API
* Criar Service (NodePort)
* Verificar ReplicaSet criado automaticamente

Entregáveis:

* API executando no Kubernetes e acessível via NodePort

---

## Fase 4 — Persistência

Objetivos:

* PostgreSQL como StatefulSet com PV e PVC
* MongoDB como StatefulSet com PV e PVC
* Integração da API com ambos os bancos

Entregáveis:

* Bancos persistentes com dados sobrevivendo a reinicializações

---

## Fase 5 — Configuração

Objetivos:

* ConfigMaps para URLs e configurações
* Secrets para credenciais dos bancos
* Injeção via variáveis de ambiente nos containers

Entregáveis:

* Configuração externa ao container, sem rebuild para mudanças

---

## Fase 6 — Confiabilidade

Objetivos:

* Liveness Probe via `/actuator/health/liveness`
* Readiness Probe via `/actuator/health/readiness`
* Startup Probe para controlar inicialização lenta
* Resource Requests e Limits em todos os containers

Entregáveis:

* Aplicação resiliente com auto-recuperação e scheduling correto

---

## Fase 7 — Escalabilidade

Objetivos:

* Escalabilidade manual com `kubectl scale`
* HPA com target de 50% CPU (depende de Resource Requests)
* Metrics Server habilitado no Minikube

Entregáveis:

* Escalabilidade manual e automática operacionais

---

## Fase 8 — Exposição Externa

Objetivos:

* Instalar Ingress Controller (nginx) no Minikube
* Criar Ingress resource com roteamento por path
* Substituir NodePort pelo Ingress como ponto de entrada

Entregáveis:

* Acesso externo unificado via Ingress

---

## Fase 9 — Operação

Objetivos:

* Logs com `kubectl logs`
* Metrics Server (`kubectl top`)
* Job para seed de dados
* CronJob para limpeza de logs
* DaemonSet para log collector

Entregáveis:

* Monitoramento básico e tarefas agendadas operacionais

---

## Fase 10 — Observabilidade Avançada

Objetivos:

* Prometheus no namespace `monitoring`
* Grafana no namespace `monitoring`
* Dashboards de JVM, HTTP e recursos do cluster

Entregáveis:

* Stack de observabilidade completa com visualização em tempo real

---

## Fase 11 — Segurança de Rede

Objetivos:

* NetworkPolicy restringindo acesso ao PostgreSQL e MongoDB
* Apenas a Product API autorizada a conectar nos bancos — exceção: o CronJob `log-cleanup` também precisa acessar o MongoDB e deve ter regra própria (label `app: log-cleanup`)
* Minikube iniciado com `--cni=calico`; o CNI padrão não aplica NetworkPolicy
* Demonstrar isolamento entre namespaces

Entregáveis:

* Bancos isolados via NetworkPolicy, padrão de segurança corporativo

---

## Fase 12 — Helm

Objetivos:

* Empacotar todos os manifests em um Helm Chart
* Parametrizar via `values.yaml` (imagem, réplicas, recursos)
* Instalar e desinstalar a stack completa com um único comando

Entregáveis:

* Chart funcional para deploy reproduzível da plataforma completa

---

## Fase 13 — Simulação de Incidentes

Objetivos:

* Simular falhas de Pod e observar auto-recuperação
* Simular deploy com falha e executar Rollback
* Simular sobrecarga e observar HPA em ação
* Simular violação de NetworkPolicy
* Praticar troubleshooting com `kubectl describe`, `kubectl logs` e `kubectl events`

Entregáveis:

* Plataforma validada sob condições adversas, simulando operação em ambiente corporativo moderno.

---

## Fase 14 — Terraform *(opcional — pós-POC)*

> Esta fase está fora do escopo da POC. Serve como referência para evolução após a conclusão das fases 1–13, aplicando os conhecimentos de Kubernetes em infraestrutura cloud real.

Contexto:

As fases anteriores usam Minikube local, sem necessidade de provisionar infraestrutura. O Terraform entra quando o objetivo é criar o cluster Kubernetes em um provedor cloud (AWS, GCP, Azure) de forma reproduzível e versionada.

Objetivos de referência:

* Provisionar um cluster Kubernetes gerenciado (EKS, GKE ou AKS) via Terraform
* Gerenciar node groups, VPC, subnets e IAM/RBAC com código
* Instalar a plataforma no cluster cloud via Helm (reaproveitando o Chart da Fase 12)
* Comparar o comportamento do cluster cloud com o Minikube local

Diferença em relação às fases anteriores:

| Escopo | Ferramenta |
|--------|-----------|
| Workloads dentro do cluster | `kubectl` + Helm (Fases 1–13) |
| Infraestrutura que hospeda o cluster | Terraform (Fase 14) |

Referências para estudo posterior:

* [Terraform AWS EKS Module](https://registry.terraform.io/modules/terraform-aws-modules/eks/aws)
* [Terraform Google GKE Module](https://registry.terraform.io/modules/terraform-google-modules/kubernetes-engine/google)
* [Terraform AzureRM AKS](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/resources/kubernetes_cluster)