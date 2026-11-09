# TDD-010 — Simulação de Incidentes

**Status:** Ready
**Versão:** 1.0
**Data:** Novembro/2026
**Referências:** [TDD-003](TDD-003-infraestrutura-kubernetes.md) · [TDD-005](TDD-005-escalabilidade-confiabilidade.md) · [TDD-007](TDD-007-observabilidade.md) · [TDD-009](TDD-009-networkpolicy.md)
**Estórias:** [US-017](../stories/US-017-simulacao-incidentes.md)
**PRD:** [Fase 13 — Simulação de Incidentes](../PRD.md#fase-13--simulação-de-incidentes) · [Cenário 6 — Recuperação Automática de Pod](../PRD.md#cenário-6--recuperação-automática-de-pod) · [Cenário 8 — Horizontal Pod Autoscaler](../PRD.md#cenário-8--horizontal-pod-autoscaler) · [Cenário 10 — Rollback](../PRD.md#cenário-10--rollback) · [Cenário 18 — NetworkPolicy](../PRD.md#cenário-18--networkpolicy)

---

## 1. Visão Geral

Runbook de incidentes simulados sobre a plataforma já implantada. Não cria novos manifests: cada incidente reutiliza recursos dos TDDs anteriores e treina o ciclo **sintoma → diagnóstico → correção**. Registre o que observou em cada passo.

---

## 2. Incidentes

| # | Incidente | Como provocar | Sintoma esperado | Diagnóstico | Correção |
|---|-----------|---------------|------------------|-------------|----------|
| 1 | Queda de Pod da API | `kubectl delete pod -l app=product-api -n learning-lab` | Pod novo em `ContainerCreating` | `kubectl get pods -w`, `kubectl get events` | Nenhuma: o Deployment recria (Cenário 6) |
| 2 | Deploy defeituoso | `kubectl set image deployment/product-api product-api=product-api:1.0.0-broken -n learning-lab` | Pod novo em `CrashLoopBackOff`; Pods antigos seguem atendendo (`maxUnavailable: 0`) | `kubectl rollout status`, `kubectl logs <pod> --previous`, `kubectl describe pod` | `kubectl rollout undo deployment/product-api` (Cenário 10) |
| 3 | Sobrecarga | Load generator do [TDD-005 §4.4](TDD-005-escalabilidade-confiabilidade.md) | CPU acima de 50% dos `requests`; réplicas aumentam | `kubectl get hpa -w`, `kubectl top pods`, Grafana | Remover a carga; réplicas reduzem após a janela de estabilização (Cenário 8) |
| 4 | Config errada | Alterar `DB_URL` no ConfigMap para um host inexistente e `kubectl rollout restart` | `CrashLoopBackOff` no startup (Flyway não conecta) | `kubectl logs --previous`, `kubectl describe pod` | Corrigir o ConfigMap e novo `rollout restart` |
| 5 | Banco indisponível | `kubectl scale statefulset postgresql --replicas=0 -n learning-lab` | Readiness da API falha; `/produtos` sem resposta | `kubectl get endpoints product-api`, `kubectl logs` da API | `kubectl scale ... --replicas=1`; dados preservados (PVC) |
| 6 | Violação de NetworkPolicy | Teste de conexão bloqueado do [TDD-009 §5](TDD-009-networkpolicy.md) | Timeout na conexão | `kubectl describe networkpolicy`, `kubectl get pod --show-labels` | Ajustar labels/regras ou remover o cliente indevido (Cenário 18) |
| 7 | Memória estourada | Reduzir `limits.memory` do Deployment para `128Mi` | `OOMKilled` e reinícios | `kubectl describe pod` (Last State), `kubectl top pods` | Restaurar o limite; revisar `MaxRAMPercentage` |

---

## 3. Kit de troubleshooting

```bash
kubectl get pods -n learning-lab -o wide
kubectl describe pod <pod> -n learning-lab
kubectl logs <pod> -n learning-lab [--previous]
kubectl get events -n learning-lab --sort-by=.lastTimestamp
kubectl events -n learning-lab
kubectl rollout history deployment/product-api -n learning-lab
kubectl get endpoints product-api -n learning-lab
```

Fluxo sugerido: `get pods` (estado) → `describe` (eventos e motivo) → `logs`/`logs --previous` (causa na aplicação) → `get events` (linha do tempo).

---

## 4. Riscos e Considerações

| Item | Detalhe |
|------|---------|
| Ordem | Execute após Fases 3 a 12 estarem concluídas; o incidente 6 exige o TDD-009 |
| Restaurar o estado | Ao final de cada incidente volte ao estado base (`rollout undo`, `scale` e ConfigMap originais) |
| Incidente 5 | Escalar o PostgreSQL a zero não apaga dados, mas interrompe a API; não use `kubectl delete pvc` |
| Mudança de manifests | Alterações manuais com `kubectl set`/`edit` divergem do Git; reaplique os manifests ou use `helm upgrade` depois |

---

## Definition of Ready

- [x] Estórias vinculadas referenciadas
- [x] Fase/Cenário do PRD referenciados
- [x] ADRs relevantes e dependências de outros TDDs listados nas Referências
- [x] Riscos documentados

## Definition of Done

- [ ] Artefatos implementados conforme este documento (divergências registradas aqui)
- [ ] Incidentes 1 a 7 executados, com sintoma, diagnóstico e correção registrados
- [ ] Estado base restaurado após cada incidente
- [ ] Arquivo `TDD-010-simulacao-incidentes_explain.md` gerado
- [ ] Estórias vinculadas atualizadas (US-017)
