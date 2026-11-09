# TDD-010 — Simulação de Incidentes

**Status:** Ready
**Versão:** 1.0
**Data:** Novembro/2026
**Referências:** [TDD-003](TDD-003-infraestrutura-kubernetes.md) · [TDD-005](TDD-005-escalabilidade-confiabilidade.md) · [TDD-007](TDD-007-observabilidade.md) · [TDD-009](TDD-009-networkpolicy.md)
**Estórias:** [US-017](../stories/US-017-simulacao-incidentes.md)
**PRD:** [Fase 13 — Simulação de Incidentes](../PRD.md#fase-13--simulação-de-incidentes) · [Cenário 6 — Recuperação Automática de Pod](../PRD.md#cenário-6--recuperação-automática-de-pod) · [Cenário 8 — Horizontal Pod Autoscaler](../PRD.md#cenário-8--horizontal-pod-autoscaler) · [Cenário 9 — Rolling Update](../PRD.md#cenário-9--rolling-update) · [Cenário 10 — Rollback](../PRD.md#cenário-10--rollback) · [Cenário 18 — NetworkPolicy](../PRD.md#cenário-18--networkpolicy)

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
| 8 | Rolling Update com perda de requisições | Loop de `curl` via Ingress durante `kubectl set image deployment/product-api product-api=product-api:1.1.0` (ver §2.1) | Algumas requisições com timeout/502 mesmo com `maxUnavailable: 0` (observado: 3 em 290 no TDD-005) | Contar códigos HTTP do loop; `kubectl get endpoints product-api -w` durante o rollout | Adicionar `preStop: sleep 5` ao container e reaplicar; repetir o loop e esperar 0 falhas |


### 2.1 Incidente 8 — Rolling Update com perda de requisições

Origem: [TDD-005 §7 e `_explain` §4](TDD-005-escalabilidade-confiabilidade_explain.md) — no Cenário 9, 3 de 290 requisições deram timeout.

**Causa:** ao encerrar um Pod, o kubelet envia SIGTERM ao mesmo tempo em que o endpoint é removido do Service/Ingress. Até a remoção propagar, ainda chegam requisições a um Pod que já está terminando.

**Reproduzir (antes):**

```bash
IP=$(minikube ip)
while true; do curl -s -o /dev/null -m 2 -w "%{http_code}\n" -H "Host: product-api.local" $IP/produtos; sleep 0.3; done > codes.txt &
kubectl set image deployment/product-api product-api=product-api:1.1.0 -n learning-lab
kubectl rollout status deployment/product-api -n learning-lab
kill %1; sort codes.txt | uniq -c      # procure 000/502/503
```

**Correção** (no container do Deployment, `k8s/deployments/product-api.yaml`):

```yaml
spec:
  template:
    spec:
      terminationGracePeriodSeconds: 30
      containers:
        - name: product-api
          lifecycle:
            preStop:
              exec:
                command: ["sleep", "5"]   # dá tempo de o endpoint sair do Service/Ingress antes do SIGTERM
```

O `exec` exige o binário `sleep` na imagem; se ela não o tiver, use a ação nativa `preStop.sleep: {seconds: 5}` (K8s ≥ 1.30). **Validar (depois):** repetir o loop e esperar somente `200`. Divergência no manifest é registrada aqui e refletida no TDD-003/TDD-005.

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
- [ ] Incidentes 1 a 8 executados, com sintoma, diagnóstico e correção registrados
- [ ] Estado base restaurado após cada incidente
- [ ] Arquivo `TDD-010-simulacao-incidentes_explain.md` gerado
- [ ] Estórias vinculadas atualizadas (US-017)
