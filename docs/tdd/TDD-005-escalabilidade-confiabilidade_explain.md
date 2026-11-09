# TDD-005 — Escalabilidade e Confiabilidade: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

| Artefato | Recurso |
|----------|---------|
| `k8s/hpa/product-api-hpa.yaml` | HorizontalPodAutoscaler `product-api-hpa` (CPU 50%, 1–5 réplicas) |
| addon `metrics-server` do Minikube | Fonte de métricas de CPU/memória para HPA e `kubectl top` |

Probes (startup/liveness/readiness) e requests/limits **já estavam** no Deployment da API (TDD-003) e nos StatefulSets (TDD-004); nesta etapa foram apenas verificados com `kubectl describe pod`. O Rolling Update (`maxSurge: 1`, `maxUnavailable: 0`) também já vinha do TDD-003.

## 2. Por que cada decisão foi tomada

- **HPA `autoscaling/v2`**: API estável atual; permite várias métricas e `behavior`.
- **Alvo de 50% de CPU**: baixo o suficiente para a carga simples de estudo disparar o scale-up.
- **Percentual sobre `requests.cpu` (250m)**: o HPA divide o uso pelo request; por isso o request é pré-requisito. 154% significa ~385m por Pod.
- **maxReplicas 5**: cabe nos 4 CPUs / 6 GB do Minikube junto com os bancos.

## 3. Alternativas descartadas

- **Alvo de memória**: a JVM reserva heap e não devolve; a métrica quase não oscila.
- **`behavior` customizado**: mantido o default (scale-down com janela de 300s) para observar o comportamento padrão.
- **Ferramenta de carga (hey/k6)**: pods `busybox` com `wget` em loop bastam e não exigem instalar nada.

## 4. Pontos de atenção

- **`<unknown>` no início**: logo após criar o HPA (ou habilitar o metrics-server) o alvo aparece como `<unknown>/50%` por ~1 min.
- **HPA vs. `kubectl scale`**: com o HPA ativo ele passa a controlar `replicas`; o escalonamento manual só vale até a próxima avaliação (valores fora de `minReplicas..maxReplicas` ou fora do que a CPU justifica são corrigidos, com scale-down só após a janela de 300s).
- **Rolling Update não foi 100% sem falhas**: durante a troca de 1.0.0 → 1.1.0 com 3 réplicas, de 290 requisições via Ingress, 3 deram timeout (2s) e as demais 200. Causa provável: o Pod antigo recebe SIGTERM antes de o Ingress/kube-proxy removê-lo dos endpoints. Mitigação (não aplicada, fora do escopo): `preStop: sleep 5` e `terminationGracePeriodSeconds` adequado no Deployment.
- **Rollback**: a imagem `1.0.0-broken` não existe, o Pod novo ficou em `ImagePullBackOff` enquanto os 3 antigos continuavam atendendo (por causa do `maxUnavailable: 0`); `rollout undo` restaurou a revisão anterior.
- **Imagem `1.1.0`** foi criada só para o teste, com `minikube image tag product-api:1.0.0 product-api:1.1.0` (mesmo conteúdo). Após o teste o cluster foi realinhado com `kubectl apply -f k8s/deployments/product-api.yaml` (volta a 1.0.0).
- **Scale-down lento**: ~5 min após cessar a carga (janela de estabilização de 300s).

## 5. Como validar

```bash
minikube addons enable metrics-server
kubectl apply -f k8s/hpa/
kubectl get hpa -n learning-lab            # TARGETS deve mostrar cpu: N%/50%, não <unknown>
kubectl describe pod -n learning-lab -l app=product-api | grep -E "Startup|Liveness|Readiness|Limits|Requests"

# Cenário 6 — recuperação
kubectl delete pod -n learning-lab -l app=product-api
kubectl get pods -n learning-lab -w

# Cenário 7 — escala manual (desative o HPA se quiser fixar o número)
kubectl scale deployment product-api --replicas=3 -n learning-lab

# Cenário 8 — HPA
kubectl run load-generator -n learning-lab --image=busybox:1.36 --restart=Never -- \
  /bin/sh -c "while true; do wget -q -O- http://product-api/produtos >/dev/null; done"
kubectl get hpa -n learning-lab -w
kubectl delete pod load-generator -n learning-lab

# Cenários 9 e 10 — rolling update e rollback
minikube image tag product-api:1.0.0 product-api:1.1.0
kubectl set image deployment/product-api product-api=product-api:1.1.0 -n learning-lab
kubectl rollout status deployment/product-api -n learning-lab
kubectl set image deployment/product-api product-api=product-api:1.0.0-broken -n learning-lab
kubectl rollout undo deployment/product-api -n learning-lab
```

### Resultado da validação (execução real)

- Probes e requests/limits conferidos com `kubectl describe pod`.
- **C6**: Pod excluído e recriado pelo ReplicaSet em ~30s.
- **C7**: 3 réplicas, 3 endpoints no Service, 12/12 requisições 200.
- **C8**: com 2 Pods de carga, CPU foi a 154% e o HPA subiu 3→5 réplicas; após remover a carga, voltou a 1 réplica.
- **C9**: rollout 1.0.0→1.1.0 concluído; 287/290 requisições 200 (3 timeouts, ver §4).
- **C10**: `ImagePullBackOff` sem indisponibilidade; `rollout undo` restaurou a imagem anterior.
