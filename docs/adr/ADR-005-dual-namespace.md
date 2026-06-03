# ADR-005 — Dois Namespaces: `learning-lab` e `monitoring`

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

O projeto precisa decidir como organizar os recursos Kubernetes em namespaces. A opção mais simples seria um único namespace para tudo. Ambientes corporativos reais, entretanto, separam a stack de observabilidade da aplicação.

---

## Decisão

Usar **dois namespaces**:

| Namespace | Conteúdo |
|-----------|----------|
| `learning-lab` | Product API, PostgreSQL, MongoDB, DaemonSet, Job, CronJob |
| `monitoring` | Prometheus, Grafana |

Justificativas:
- Representa padrão corporativo real — times de platform/SRE gerenciam o namespace de monitoring separadamente
- Demonstra Service discovery cross-namespace (Prometheus scrapeando target em `learning-lab`)
- Isola ciclos de vida — stack de monitoring pode ser atualizada sem afetar a aplicação
- Demonstra o uso de NetworkPolicy considerando namespaces como boundary de segurança

---

## Consequências

- Prometheus precisa de permissão (ServiceAccount + ClusterRole) para scraping cross-namespace
- `kubectl` commands exigem flag `-n learning-lab` ou `-n monitoring` — reforça o aprendizado de namespaces
- Ingress no namespace `learning-lab` — Ingress Controller instalado no namespace padrão pelo Minikube addon
