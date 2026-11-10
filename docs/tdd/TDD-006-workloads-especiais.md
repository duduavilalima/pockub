# TDD-006 — Workloads Especiais

**Status:** Done
**Versão:** 1.0
**Data:** Junho/2026
**Referências:** [PRD — Fase 9](../PRD.md#fase-9--operação) · [TDD-003](TDD-003-infraestrutura-kubernetes.md)
**Estórias:** [US-013](../stories/US-013-workloads-operacionais.md)
**PRD:** [Fase 9 — Operação](../PRD.md#fase-9--operação) · [Cenário 14 — Job (Seed de Dados)](../PRD.md#cenário-14--job-seed-de-dados) · [Cenário 15 — CronJob (Limpeza de Logs)](../PRD.md#cenário-15--cronjob-limpeza-de-logs) · [Cenário 16 — DaemonSet (Log Collector)](../PRD.md#cenário-16--daemonset-log-collector)

---

## 1. Visão Geral

Define os manifests e comportamentos de Job, CronJob e DaemonSet — workloads com ciclo de vida diferente de Deployments e StatefulSets. Cada um demonstra um padrão de uso corporativo distinto.

---

## 2. Job — Seed de Dados

### 2.1 Propósito

Popula o PostgreSQL com produtos iniciais para as demonstrações. Executa uma única vez e encerra com status `Complete`.

### 2.2 Manifest

```yaml
# k8s/jobs/seed-data-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: seed-data
  namespace: learning-lab
spec:
  backoffLimit: 3          # máximo de 3 tentativas em caso de falha
  activeDeadlineSeconds: 600    # aborta se a API não ficar pronta em 10 min
  # sem ttlSecondsAfterFinished: o Job permanece para `kubectl get job` / `kubectl logs`
  template:
    spec:
      restartPolicy: OnFailure
      initContainers:
        - name: wait-for-api
          image: busybox:1.36
          command:
            - sh
            - -c
            - |
              until wget -q -O- http://product-api/actuator/health/readiness; do
                echo "aguardando API..."; sleep 3
              done
          resources:
            requests: {cpu: "10m", memory: "16Mi"}
            limits: {cpu: "50m", memory: "32Mi"}
      containers:
        - name: seed
          image: curlimages/curl:8.6.0
          command:
            - sh
            - -c
            - |
              set -e
              # idempotente: não insere de novo se já existirem produtos
              if curl -sf http://product-api/produtos | grep -q '"id"'; then
                echo "Produtos já existem — seed ignorado."; exit 0
              fi

              curl -sf -X POST http://product-api/produtos \
                -H "Content-Type: application/json" \
                -d '{"nome":"Teclado Mecânico","descricao":"Cherry MX","preco":299.90,"quantidadeEstoque":15}'

              curl -sf -X POST http://product-api/produtos \
                -H "Content-Type: application/json" \
                -d '{"nome":"Mouse Gamer","descricao":"DPI ajustável","preco":199.90,"quantidadeEstoque":25}'

              curl -sf -X POST http://product-api/produtos \
                -H "Content-Type: application/json" \
                -d '{"nome":"Monitor 4K","descricao":"27 polegadas","preco":1499.90,"quantidadeEstoque":8}'

              echo "Seed concluído com sucesso."
          resources:
            requests: {cpu: "10m", memory: "16Mi"}
            limits: {cpu: "50m", memory: "32Mi"}
```

### 2.3 Execução e Verificação

```bash
# Executar
kubectl apply -f k8s/jobs/seed-data-job.yaml

# Acompanhar
kubectl get job seed-data -n learning-lab -w

# Verificar logs
kubectl logs job/seed-data -n learning-lab

# Confirmar produtos criados
curl http://product-api.local/produtos
```

### 2.4 Comportamento

| Estado | Descrição |
|--------|-----------|
| `Running` | Pod em execução |
| `Complete` | Executou com sucesso (exit code 0) |
| `Failed` | Falhou — reexecutado até `backoffLimit` |

---

## 3. CronJob — Limpeza de Logs

### 3.1 Propósito

Remove LogAcessos com mais de 30 dias do MongoDB. Executa diariamente às 02h, simulando rotina de manutenção corporativa.

### 3.2 Manifest

```yaml
# k8s/cronjobs/log-cleanup-cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: log-cleanup
  namespace: learning-lab
spec:
  schedule: "0 2 * * *"          # diário às 02:00
  concurrencyPolicy: Forbid      # não executa nova instância se a anterior ainda rodar
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 3
  jobTemplate:
    spec:
      backoffLimit: 2
      template:
        metadata:
          labels:
            app: log-cleanup      # usado pela NetworkPolicy que libera o acesso ao MongoDB
        spec:
          restartPolicy: OnFailure
          containers:
            - name: log-cleanup
              image: mongo:7
              command:
                - mongosh
                - mongodb://mongodb:27017/pockub
                - --eval
                - |
                  const limite = new Date();
                  limite.setDate(limite.getDate() - 30);
                  const result = db.access_logs.deleteMany({
                    timestamp: { $lt: limite }
                  });
                  print("Logs removidos:", result.deletedCount);
              resources:
                requests: {cpu: "50m", memory: "64Mi"}
                limits: {cpu: "200m", memory: "256Mi"}
```

### 3.3 Execução Manual (para demonstração)

```bash
# Disparar manualmente sem aguardar o schedule
kubectl create job log-cleanup-manual \
  --from=cronjob/log-cleanup \
  -n learning-lab

# Verificar
kubectl get job log-cleanup-manual -n learning-lab
kubectl logs job/log-cleanup-manual -n learning-lab

# Verificar histórico do CronJob
kubectl get cronjob log-cleanup -n learning-lab
```

### 3.4 Campos Importantes

| Campo | Valor | Descrição |
|-------|-------|-----------|
| `schedule` | `0 2 * * *` | Cron expression: às 02h diariamente |
| `concurrencyPolicy` | `Forbid` | Impede execuções paralelas |
| `successfulJobsHistoryLimit` | `3` | Mantém histórico dos 3 últimos Jobs bem-sucedidos |
| `failedJobsHistoryLimit` | `3` | Mantém histórico dos 3 últimos Jobs com falha |

---

## 4. DaemonSet — Log Collector

### 4.1 Propósito

Demonstra o padrão DaemonSet: garante exatamente 1 Pod em cada node do cluster. Representa agentes de infraestrutura (log shippers, monitoring agents) que precisam rodar em todo o cluster.

### 4.2 Manifest

```yaml
# k8s/daemonsets/log-collector-daemonset.yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: log-collector
  namespace: learning-lab
  labels:
    app: log-collector
spec:
  selector:
    matchLabels:
      app: log-collector
  template:
    metadata:
      labels:
        app: log-collector
    spec:
      tolerations:
        - key: node-role.kubernetes.io/control-plane
          operator: Exists
          effect: NoSchedule
      containers:
        - name: log-collector
          image: busybox:1.36
          command:
            - sh
            - -c
            - |
              echo "Log collector iniciado no node: $NODE_NAME"
              # coleta simulada: acompanha (tail) os logs dos containers da Product API no node
              exec tail -F /var/log/containers/product-api-*.log
          env:
            - name: NODE_NAME
              valueFrom:
                fieldRef:
                  fieldPath: spec.nodeName
          resources:
            requests:
              cpu: "50m"
              memory: "32Mi"
            limits:
              cpu: "100m"
              memory: "64Mi"
          volumeMounts:
            - name: varlog
              mountPath: /var/log
              readOnly: true
            # runtime Docker: /var/log/pods/.../0.log é symlink para /var/lib/docker/containers
            - name: docker-containers
              mountPath: /var/lib/docker/containers
              readOnly: true
      volumes:
        - name: varlog
          hostPath:
            path: /var/log
        - name: docker-containers
          hostPath:
            path: /var/lib/docker/containers
```

### 4.3 Verificação

```bash
# Ver DaemonSet
kubectl get daemonset -n learning-lab

# Esperado no Minikube (1 node = 1 Pod)
# NAME            DESIRED   CURRENT   READY   ...
# log-collector   1         1         1

# Verificar em qual node o Pod está rodando
kubectl get pods -n learning-lab -l app=log-collector -o wide

# Logs do collector
kubectl logs -l app=log-collector -n learning-lab -f
```

### 4.4 Comportamento no Minikube

O Minikube tem apenas 1 node — o DaemonSet cria exatamente 1 Pod. Em clusters com múltiplos nodes, seriam criados N Pods (1 por node), incluindo control-plane por causa da `toleration`. O "coletor" apenas faz `tail` dos logs dos containers da API (via `hostPath: /var/log`) para demonstrar o padrão; um agente real (Fluent Bit, Promtail) seguiria o mesmo desenho de DaemonSet + hostPath.

---

## 5. Comparativo dos Workloads

| Workload | Ciclo de vida | Quantidade | Caso de uso |
|----------|-------------|-----------|-------------|
| Deployment | Contínuo | N réplicas | API stateless escalável |
| StatefulSet | Contínuo | N com identidade | Bancos de dados |
| Job | Finito (run-to-completion) | 1 execução | Seed, migrations |
| CronJob | Periódico | 1 por disparo | Limpeza, relatórios |
| DaemonSet | Contínuo | 1 por node | Agentes de infraestrutura |

---

## 6. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Job idempotente | O script consulta `GET /produtos` e sai sem inserir se já houver produtos (note que essa consulta também gera um `LogAcesso`) |
| CronJob timezone | Por padrão usa UTC — ajustar `timeZone: "America/Sao_Paulo"` se necessário (K8s 1.27+) |
| DaemonSet em control-plane | A `toleration` só tem efeito se o node tiver o taint `control-plane`; no Minikube de 1 node normalmente não há taint, e ela fica como exemplo para clusters multi-node |
| NetworkPolicy (Fase 11) | O CronJob conecta direto no MongoDB; a policy de isolamento do banco deve permitir Pods com `app: log-cleanup` além da `product-api` |
| Symlinks de log (Docker) | **Divergência da implementação:** no Minikube com runtime Docker, `/var/log/containers/*.log` → `/var/log/pods/.../0.log` → `/var/lib/docker/containers/<id>/<id>-json.log`. Só com `/var/log` montado o `tail` falha (`No such file or directory`); por isso o DaemonSet monta também `/var/lib/docker/containers` (somente leitura) |
| `ttlSecondsAfterFinished` | Omitido de propósito no seed, para que o Job e seus logs continuem disponíveis nas verificações; em uso real, defina (ex.: `3600`) para limpar Jobs concluídos |


---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [x] Artefatos implementados conforme este documento (divergências registradas aqui)
- [x] Job `seed-data` com status `Complete` e produtos visíveis em `GET /produtos`
- [x] CronJob disparado manualmente com `kubectl create job --from=cronjob/log-cleanup`
- [x] DaemonSet com 1 Pod por node
- [x] Cenários 14, 15 e 16 do PRD executados
- [x] Arquivo `TDD-006-workloads-especiais_explain.md` gerado
- [x] Estórias vinculadas atualizadas (US-013)
