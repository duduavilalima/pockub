# ADR-001 — Minikube como Cluster Kubernetes Local

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

O projeto é uma POC de estudo individual ou em equipe pequena. É necessário um cluster Kubernetes funcional que rode localmente, sem dependência de infraestrutura cloud, custos de uso ou acesso à internet para provisionamento.

As alternativas avaliadas foram:

| Opção | Prós | Contras |
|-------|------|---------|
| **Minikube** | Instalação simples, addons prontos (Ingress, Metrics Server, Registry), maturidade | Single-node |
| kind | Leve, multi-node simulado via Docker | Addons menos plug-and-play |
| k3s | Próximo de produção, leve | Configuração mais manual |
| EKS/GKE/AKS | Ambiente real de produção | Custo, dependência de conta cloud |

---

## Decisão

Usar **Minikube** como cluster local.

Justificativas:
- Addons `ingress`, `metrics-server` e `registry` habilitáveis com um comando (`minikube addons enable`)
- Documentação extensa e comunidade ativa
- Foco do projeto é aprender K8s, não operar o cluster em si
- Sem custo e sem dependência de conta cloud

---

## Consequências

- Cluster single-node — não demonstra scheduling multi-node real
- DaemonSet terá apenas 1 Pod (1 node = 1 réplica)
- LoadBalancer requer `minikube tunnel` para funcionar localmente
- Ambiente não reflete limitações de rede de clusters corporativos
