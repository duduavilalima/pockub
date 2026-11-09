# ADR-005 — Dois Namespaces: `learning-lab` e `monitoring`

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

O projeto precisa decidir como organizar os recursos Kubernetes em namespaces. A opção mais simples seria um único namespace para tudo. Ambientes corporativos reais, entretanto, separam a stack de observabilidade da aplicação.

---

## Decisão

Usar **dois namespaces próprios** (mais o `ingress-nginx` do addon):

| Namespace | Conteúdo |
|-----------|----------|
| `learning-lab` | Product API, PostgreSQL, MongoDB, DaemonSet, Job, CronJob |
| `monitoring` | Prometheus, Grafana |
| `ingress-nginx` | Ingress Controller (criado pelo addon `ingress` do Minikube; não gerenciado por este projeto) |

Justificativas:
- Representa padrão corporativo real — times de platform/SRE gerenciam o namespace de monitoring separadamente
- Demonstra Service discovery cross-namespace (Prometheus scrapeando target em `learning-lab`)
- Isola ciclos de vida — stack de monitoring pode ser atualizada sem afetar a aplicação
- Demonstra o uso de NetworkPolicy considerando namespaces como boundary de segurança

---

## Consequências

- Prometheus precisa de permissão (ServiceAccount + ClusterRole) para descobrir Pods/Endpoints cross-namespace via `kubernetes_sd_configs`
- `kubectl` commands exigem flag `-n learning-lab` ou `-n monitoring` — reforça o aprendizado de namespaces
- O recurso Ingress fica no namespace `learning-lab`; o Ingress Controller (nginx) é instalado pelo addon do Minikube no namespace próprio `ingress-nginx`
