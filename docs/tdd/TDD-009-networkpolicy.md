# TDD-009 — NetworkPolicy

**Status:** Draft
**Versão:** 1.0
**Data:** Novembro/2026
**Referências:** [ADR-005](../adr/ADR-005-dual-namespace.md) · [TDD-004](TDD-004-persistencia.md) · [TDD-006](TDD-006-workloads-especiais.md)
**Estórias:** [US-016](../stories/US-016-networkpolicy.md)
**PRD:** [Fase 11 — Segurança de Rede](../PRD.md#fase-11--segurança-de-rede) · [Cenário 18 — NetworkPolicy](../PRD.md#cenário-18--networkpolicy)

---

## 1. Visão Geral

Isola o PostgreSQL e o MongoDB: só os Pods autorizados conseguem abrir conexão nas portas dos bancos. Usa `NetworkPolicy` com seleção por labels, sem alterar a aplicação.

---

## 2. Pré-requisito: CNI com suporte a NetworkPolicy

O objeto `NetworkPolicy` é aceito pela API mesmo quando o CNI não o aplica. O CNI padrão do Minikube não aplica, então a policy "funciona" sem bloquear nada. Inicie o cluster com Calico:

```bash
minikube start --memory=6144 --cpus=4 --cni=calico
```

---

## 3. Matriz de acesso

| Destino | Origem permitida | Label da origem | Porta |
|---------|------------------|-----------------|-------|
| PostgreSQL | Product API | `app: product-api` | 5432 |
| MongoDB | Product API | `app: product-api` | 27017 |
| MongoDB | CronJob de limpeza | `app: log-cleanup` ([TDD-006](TDD-006-workloads-especiais.md)) | 27017 |

O Job de seed fala apenas com a API (HTTP), por isso não precisa de regra para os bancos. Probes `exec` dos próprios bancos rodam dentro do container e não passam pela rede do Pod.

---

## 4. Manifests

```yaml
# k8s/networkpolicies/postgresql-allow-api.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: postgresql-allow-api
  namespace: learning-lab
spec:
  podSelector:
    matchLabels:
      app: postgresql
  policyTypes: [Ingress]
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: product-api
      ports:
        - protocol: TCP
          port: 5432
---
# k8s/networkpolicies/mongodb-allow-api-and-cleanup.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: mongodb-allow-api-and-cleanup
  namespace: learning-lab
spec:
  podSelector:
    matchLabels:
      app: mongodb
  policyTypes: [Ingress]
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: product-api
        - podSelector:
            matchLabels:
              app: log-cleanup
      ports:
        - protocol: TCP
          port: 27017
```

Sem `namespaceSelector`, o `podSelector` vale apenas para Pods do mesmo namespace (`learning-lab`): Pods de outros namespaces também ficam bloqueados.

---

## 5. Validação (Cenário 18)

```bash
kubectl apply -f k8s/networkpolicies/

# Permitido: a API continua funcionando
curl http://product-api.local/produtos

# Bloqueado: Pod sem label autorizado (esperado: timeout)
kubectl run np-test --rm -it --image=busybox:1.36 --restart=Never -n learning-lab \
  -- nc -zv -w 3 postgresql 5432

# Bloqueado: outro namespace
kubectl run np-test --rm -it --image=busybox:1.36 --restart=Never -n default \
  -- nc -zv -w 3 postgresql.learning-lab 5432

# Permitido: CronJob de limpeza
kubectl create job np-cleanup --from=cronjob/log-cleanup -n learning-lab
kubectl logs job/np-cleanup -n learning-lab
```

Para observar o efeito no diagnóstico: remova a policy (`kubectl delete -f k8s/networkpolicies/`) e repita o teste bloqueado — a conexão passa a funcionar.

---

## 6. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| CNI padrão | Sem `--cni=calico` nada é bloqueado e o cenário gera falso positivo |
| Novos clientes | Qualquer novo componente que fale direto com os bancos (ex.: backup) precisa de regra própria |
| Só ingress | As policies restringem apenas entrada; não há `default-deny` de egress, para manter o escopo do estudo |
| Helm | Os mesmos manifests entram no chart como `networkpolicies.yaml`, controlado por `networkPolicy.enabled` ([TDD-008](TDD-008-helm.md)) |

---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [ ] Artefatos implementados conforme este documento (divergências registradas aqui)
- [ ] Cluster iniciado com `--cni=calico` e policies aplicadas
- [ ] Cenário 18 do PRD executado: API conecta, CronJob conecta, Pod não autorizado é bloqueado
- [ ] Arquivo `TDD-009-networkpolicy_explain.md` gerado
- [ ] Estórias vinculadas atualizadas (US-016)
