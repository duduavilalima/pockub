# TDD-006 — Workloads Especiais: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

| Artefato | Recurso |
|----------|---------|
| `k8s/jobs/seed-data-job.yaml` | Job `seed-data`: `initContainer` espera a API ficar pronta; container `curl` insere 3 produtos via `POST /produtos` |
| `k8s/cronjobs/log-cleanup-cronjob.yaml` | CronJob `log-cleanup` (`0 2 * * *`): `mongosh` apaga `access_logs` com mais de 30 dias |
| `k8s/daemonsets/log-collector-daemonset.yaml` | DaemonSet `log-collector`: 1 Pod por node que faz `tail -F` dos logs da Product API via `hostPath` |

## 2. Por que cada decisão foi tomada

- **Job via API REST (e não SQL direto)**: o seed passa pelas mesmas validações da aplicação e não precisa de credenciais do banco.
- **`initContainer wait-for-api`**: o Job pode ser aplicado antes de a API estar pronta; o init evita gastar o `backoffLimit` com falhas de ordem de subida.
- **Idempotência (`GET /produtos` antes de inserir)**: reaplicar o Job não duplica dados. Efeito colateral: essa consulta gera um `LogAcesso`.
- **`restartPolicy: OnFailure` + `backoffLimit`**: Jobs não aceitam `Always`; falhas são reexecutadas até o limite.
- **Sem `ttlSecondsAfterFinished` no seed**: o Job e os logs ficam disponíveis para `kubectl get job` / `kubectl logs`.
- **`concurrencyPolicy: Forbid` no CronJob**: evita duas limpezas simultâneas no mesmo banco.
- **Label `app: log-cleanup` no Pod do CronJob**: será usada pela NetworkPolicy (TDD-009) para liberar o acesso ao MongoDB.
- **DaemonSet + `hostPath` somente leitura**: é o desenho real de agentes (Fluent Bit, Promtail); `tail` apenas simula a coleta.

## 3. Alternativas descartadas

- **Seed por script SQL / `psql`**: exigiria Secret do PostgreSQL no Job e ignoraria as regras da API.
- **Deployment com `sleep` em vez de Job**: não tem semântica run-to-completion.
- **Fluent Bit real no DaemonSet**: complexidade desnecessária para demonstrar o padrão.

## 4. Pontos de atenção

- **Divergência do TDD (symlinks do Docker)**: no Minikube com runtime Docker, `/var/log/containers/*.log` é symlink para `/var/log/pods/.../0.log`, que por sua vez é symlink para `/var/lib/docker/containers/<id>/<id>-json.log`. Montando só `/var/log`, o `tail` dava `No such file or directory`. Foi adicionado um segundo `hostPath` (`/var/lib/docker/containers`, read-only). Com `containerd` o segundo mount não seria necessário.
- O glob `product-api-*.log` também casa logs de **initContainers** e de Pods antigos (symlinks quebrados geram avisos `can't open`, que o `tail -F` ignora e segue tentando).
- A saída do collector é o JSON bruto do Docker (`{"log": ..., "stream": ...}`).
- **Seed na validação**: os produtos já existiam, então foi exercitado o caminho idempotente ("seed ignorado"); o caminho de inserção só roda com a tabela vazia.
- O CronJob usa UTC por padrão (`timeZone` opcional, K8s 1.27+).
- Jobs criados manualmente (`log-cleanup-manual`) não são removidos pelo histórico do CronJob; apague com `kubectl delete job`.

## 5. Como validar

```bash
kubectl apply -f k8s/jobs/ -f k8s/cronjobs/ -f k8s/daemonsets/
kubectl get job seed-data -n learning-lab            # COMPLETIONS 1/1
kubectl logs job/seed-data -n learning-lab

# CronJob: inserir log com 40 dias e disparar manualmente
kubectl exec mongodb-0 -n learning-lab -- mongosh --quiet mongodb://localhost:27017/pockub \
  --eval 'db.access_logs.insertOne({nomeProduto:"teste-antigo",timestamp:new Date(Date.now()-40*864e5)})'
kubectl create job log-cleanup-manual --from=cronjob/log-cleanup -n learning-lab
kubectl logs job/log-cleanup-manual -n learning-lab  # "Logs removidos: 1"

# DaemonSet: 1 Pod por node e logs da API
kubectl get daemonset,pods -n learning-lab -l app=log-collector -o wide
kubectl logs -l app=log-collector -n learning-lab --tail=20
```
