# TDD-009 — NetworkPolicy: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

| Artefato | Recurso |
|----------|---------|
| `k8s/networkpolicies/postgresql-allow-api.yaml` | Libera a porta 5432 do PostgreSQL só para Pods `app: product-api` |
| `k8s/networkpolicies/mongodb-allow-api-and-cleanup.yaml` | Libera a porta 27017 do MongoDB para `app: product-api` e `app: log-cleanup` |

O cluster já estava com Calico (`minikube start --cni=calico`, ver CLAUDE.md).

## 2. Por que cada decisão foi tomada

- **Seleção por label (`podSelector`)**: as regras seguem o Pod, não o IP — sobrevivem a restart e a escala do HPA.
- **Regra própria para o CronJob (`app: log-cleanup`)**: ele fala direto com o MongoDB; sem a regra, a limpeza falharia silenciosamente com timeout.
- **Sem `namespaceSelector`**: o `podSelector` isolado só vale para o mesmo namespace, então Pods de outros namespaces também ficam bloqueados.
- **Só `Ingress`**: restringe quem entra nos bancos; um `default-deny` de egress exigiria liberar DNS, API server etc., fora do escopo.

## 3. Alternativas descartadas

- **`default-deny` no namespace inteiro**: exigiria liberar Ingress → API, Prometheus → API, Job de seed → API etc.
- **CNI padrão do Minikube**: aceita o objeto mas não bloqueia nada (falso positivo).
- **Liberar por CIDR**: IPs de Pods mudam.

## 4. Pontos de atenção

- Sem CNI com suporte, o `kubectl apply` funciona e **nada é bloqueado**. Por isso o teste foi feito antes e depois: sem as policies, o Pod sem label conectou (`postgresql (10.107.102.239:5432) open`); com elas, falhou.
- O Prometheus (namespace `monitoring`) só faz scrape da API (`app: product-api`), não dos bancos — por isso não precisa de regra.
- Probes `exec` dos bancos não passam pela rede do Pod; probes HTTP da API vêm do kubelet e não são afetadas (a regra só protege os bancos).
- Novo cliente direto dos bancos (backup, migração) precisa de uma regra própria.
- Logo após o `rollout restart` da API houve um 502 transitório do Ingress (janela do rolling update, ver incidente 8 do [TDD-010](TDD-010-simulacao-incidentes.md)); em seguida 200 estável.

## 5. Como validar

```bash
kubectl apply -f k8s/networkpolicies/

# Permitido: API
curl -H "Host: product-api.local" http://$(minikube ip)/produtos

# Bloqueado: Pod sem label (falha em ~3s)
kubectl run np-test --rm -i --image=busybox:1.36 --restart=Never -n learning-lab -- nc -zv -w 3 postgresql 5432
kubectl run np-test --rm -i --image=busybox:1.36 --restart=Never -n learning-lab -- nc -zv -w 3 mongodb 27017

# Bloqueado: outro namespace
kubectl run np-test --rm -i --image=busybox:1.36 --restart=Never -n default -- nc -zv -w 3 postgresql.learning-lab 5432

# Permitido: CronJob
kubectl create job np-cleanup --from=cronjob/log-cleanup -n learning-lab
kubectl logs job/np-cleanup -n learning-lab     # "Logs removidos: N"
```
