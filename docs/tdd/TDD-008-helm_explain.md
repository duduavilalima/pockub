# TDD-008 — Helm Chart: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

Chart `helm/kubernetes-learning-lab/` com `Chart.yaml`, `values.yaml` (+ `values-dev.yaml` e `values-prod.yaml` como exemplos de override) e templates para tudo que antes era aplicado com `kubectl apply`: Secrets, ConfigMaps, Deployment/Service/Ingress/HPA da API, StatefulSets e Services dos bancos, Job de seed, CronJob de limpeza, DaemonSet, NetworkPolicies e a stack `monitoring` (namespace, RBAC, Prometheus, Grafana). Os recursos têm os mesmos nomes dos manifests de `k8s/`, então os comandos dos outros TDDs continuam válidos.

## 2. Por que cada decisão foi tomada

- **Namespace via `--namespace` + `.Release.Namespace`**: o Helm cria o `learning-lab`; um template de Namespace entraria em conflito de ownership. O `monitoring` é criado por template porque é um segundo namespace e sai junto no `uninstall`.
- **Nomes fixos (sem `fullname`)**: a aplicação aponta para `postgresql`, `mongodb`, `product-api`; renomear quebraria URLs e NetworkPolicies.
- **`selectorLabels` com `app` + `instance`**: separa duas releases e mantém o label `app` usado pelas NetworkPolicies e pelo Prometheus.
- **`replicas` omitido quando `hpa.enabled`**: senão cada `helm upgrade` reseta as réplicas que o HPA escolheu.
- **Flags `enabled`**: cada workload opcional (seed, cleanup, collector, netpol, hpa, ingress, monitoring) liga/desliga por values.
- **`stringData` nos Secrets**: o Helm/K8s codificam; evita base64 manual no values.
- **Label de instância nos `volumeClaimTemplates`**: permite `kubectl delete pvc -l app.kubernetes.io/instance=<release>` após o uninstall.

- **Senhas fora do `values.yaml`**: `postgresql.credentials.password` e `monitoring.grafana.adminPassword` são vazias e os templates usam `required` (falha explícita em vez de Secret vazio). `helm/helm-env.sh` lê `helm/.env` e injeta com `--set-file` a partir de arquivos temporários, para a senha não aparecer na linha de comando (`ps`/histórico). O script não executa o `.env` (só lê `CHAVE=valor`); `GRAFANA_ADMIN_PASSWORD` é opcional quando `monitoring.enabled=false`.

## 3. Alternativas descartadas

- **`source .env` + `--set k=$VAR`**: expõe a senha no `ps`, e vírgulas/barras no valor quebram o `--set`.

- **Kustomize**: não ensina o ciclo de release (history/rollback) que o estudo quer.
- **Subcharts (um por componente)**: mais arquivos sem ganho de aprendizado.
- **Dashboards do Grafana no chart**: dependem do fluxo de import (`${DS_PROMETHEUS}`); ficam fora (ver TDD-007).

## 4. Pontos de atenção

- **Validado com Helm v4.3.1**: `lint` e `template` OK (defaults/dev/prod); `install` com `--wait`; dados dos bancos preservados (o produto criado antes da migração continuou lá); `upgrade --set productApi.replicas=2 --set hpa.enabled=false` (2 réplicas, HPA removido); `rollback 1` (volta a 1 réplica e recria o HPA); `uninstall` removeu tudo, inclusive o namespace `monitoring` e o ClusterRole, e **manteve os PVCs**.
- **Migrar de `kubectl apply` para Helm**: os recursos precisam ser apagados antes (selectors imutáveis e ownership). Os PVCs foram mantidos, então os dados sobreviveram.
- **PVCs antigos não ganham o label de instância** (o StatefulSet reaproveita pelo nome); nesses casos apague por nome.
- **Revisão 3 = rollback**: `helm rollback` cria uma nova revisão com o conteúdo da 1.
- O HPA aparece com `REPLICAS 0` e `<unknown>` por alguns segundos após ser recriado.
- **Job de seed é imutável**: se o template do Job mudar, `helm upgrade` falha; apague o Job antes (ou use hooks).
- Após `helm uninstall` o cluster foi **devolvido ao estado de manifests puros** (`kubectl apply`); os dashboards do Grafana precisam ser reimportados (Grafana sem PVC).

## 5. Como validar

```bash
helm/helm-env.sh lint helm/kubernetes-learning-lab/
helm/helm-env.sh template learning-lab helm/kubernetes-learning-lab/ -n learning-lab | kubectl apply --dry-run=client -f -

cp helm/.env.example helm/.env   # preencher as senhas
helm/helm-env.sh install learning-lab helm/kubernetes-learning-lab/ -n learning-lab --create-namespace --wait
helm/helm-env.sh upgrade learning-lab helm/kubernetes-learning-lab/ -n learning-lab --set productApi.replicas=2 --set hpa.enabled=false
helm history learning-lab -n learning-lab
helm rollback learning-lab 1 -n learning-lab
helm uninstall learning-lab -n learning-lab
kubectl delete pvc postgres-data-postgresql-0 mongodb-data-mongodb-0 -n learning-lab
```
