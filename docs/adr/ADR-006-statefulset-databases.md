# ADR-006 — StatefulSet para Bancos de Dados

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

Os bancos de dados PostgreSQL e MongoDB precisam ser implantados no Kubernetes de forma que demonstre persistência real. As opções são Deployment + PVC ou StatefulSet.

| Opção | Característica |
|-------|---------------|
| **Deployment + PVC** | Simples, sem identidade estável de Pod |
| **StatefulSet** | Identidade estável, PVC por Pod, ordem de criação/exclusão garantida |

---

## Decisão

Usar **StatefulSet** para PostgreSQL e MongoDB.

Justificativas:
- StatefulSet é o padrão K8s para workloads stateful — bancos de dados são o caso de uso canônico
- Demonstra dois workloads K8s distintos: Deployment (stateless) vs StatefulSet (stateful)
- PVC vinculado ao Pod pelo nome estável (`postgres-0`, `mongodb-0`) — o volume sobrevive ao Pod
- Comportamento de exclusão ordenada relevante para futuras extensões com replicação

---

## Consequências

- Nomes de Pod previsíveis: `postgresql-0`, `mongodb-0`
- PVC **não é excluído** automaticamente ao deletar o StatefulSet — requer `kubectl delete pvc` manual para limpeza
- Scaling de StatefulSet cria Pods em ordem (`-1`, `-2`...) — diferente de Deployment
- Para o escopo da POC, 1 réplica por banco é suficiente
