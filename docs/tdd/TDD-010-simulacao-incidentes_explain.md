# TDD-010 — Simulação de Incidentes: Explicação da Implementação

> Arquivo gerado conforme regra de workflow do CLAUDE.md.

---

## 1. O que foi implementado

O TDD-010 é um runbook: quase não há artefato novo. O que mudou:

| Artefato | Mudança |
|----------|---------|
| `k8s/deployments/product-api.yaml` | `terminationGracePeriodSeconds: 30` + `lifecycle.preStop` (`sleep 5`) no container `product-api` |
| `helm/kubernetes-learning-lab/templates/deployment.yaml` | Mesma mudança, para o chart não divergir dos manifests |
| `docs/tdd/TDD-010-simulacao-incidentes.md` | Seção 2.2 com o registro real de cada incidente e as divergências |
| TDD-003 / TDD-005, US-017, `docs/tdd/README.md` | Referências e status atualizados |

Os 8 incidentes foram executados no Minikube; o estado base foi restaurado ao fim de cada um.

## 2. Por que cada decisão foi tomada

- **`preStop: sleep 5`**: ao terminar um Pod, o kubelet manda SIGTERM e, em paralelo, o endpoint é removido do Service/Ingress. Essa remoção demora a propagar (EndpointSlice → kube-proxy → nginx). Nesse intervalo o Pod ainda recebe requisições. O `preStop` roda *antes* do SIGTERM e mantém o Pod servindo por 5 s, tempo de a remoção propagar.
- **`terminationGracePeriodSeconds: 30`**: explícito, para documentar que o `preStop` (5 s) + o shutdown do Spring cabem na janela (o padrão também é 30 s, mas o valor visível evita surpresa).
- **Variante `exec` com `sleep`**: a imagem tem `/bin/sleep` (verificado com `kubectl exec`), então não precisou da ação nativa `preStop.sleep` (K8s ≥ 1.30).
- **Imagem `1.0.0-broken` construída com `FROM product-api:1.0.0`**: reproduz `CrashLoopBackOff` de verdade sem alterar o código da API.

## 3. Alternativas descartadas

- **`preStop.sleep` nativo**: exige K8s ≥ 1.30; `exec` funciona em qualquer versão.
- **Aumentar `maxSurge`/réplicas para esconder o problema**: não corrige a causa (corrida entre SIGTERM e remoção do endpoint).
- **`readinessProbe` mais agressiva**: reduz, mas não elimina a janela.
- **Tag `1.0.0-broken` inexistente**: daria `ImagePullBackOff`, outro sintoma, que não treina o fluxo de `logs --previous`.

## 4. Pontos de atenção

- **O `preStop` só protege Pods que já o têm.** No primeiro rollout depois de aplicar o manifest, os Pods terminados são os antigos (sem `preStop`). Meça no rollout seguinte. Na primeira tentativa vieram 2 `000`, que na verdade ocorreram no início do loop, antes do `set image`; repetido com aquecimento, 0 falhas em 210.
- **Incidente 5**: a readiness da API *não* depende do banco; os Pods ficam `Ready` e o Service continua enviando tráfego, que falha. Para a readiness refletir o banco, seria preciso incluir `db` no `readiness` group do Actuator. Isso é uma decisão de design (um banco fora do ar derrubaria todos os Pods do Service).
- **Incidente 7**: `kubectl set resources --limits` sozinho é rejeitado se `requests.memory` ficar acima do limite; reduza os dois.
- **Incidente 3**: com o HPA em 5 réplicas, os incidentes seguintes rodam com várias réplicas; o scale-down leva ≥ 5 min.
- Mudanças via `kubectl set/patch` divergem do Git; restaure com `kubectl apply -f` (ou `helm upgrade`).

## 5. Como validar

```bash
# Manifest e chart
kubectl apply --dry-run=server -f k8s/deployments/product-api.yaml
helm lint helm/kubernetes-learning-lab/

# Incidente 8 (aplique o manifest, espere o rollout e meça no rollout seguinte)
kubectl apply -f k8s/deployments/product-api.yaml
kubectl rollout status deployment/product-api -n learning-lab
IP=$(minikube ip)
while true; do curl -s -o /dev/null -m 2 -w "%{http_code}\n" -H "Host: product-api.local" $IP/produtos; sleep 0.2; done > codes.txt &
kubectl set image deployment/product-api product-api=product-api:1.1.0 -n learning-lab
kubectl rollout status deployment/product-api -n learning-lab
kill %1; sort codes.txt | uniq -c      # esperado: só 200
```

Resultado obtido: antes do `preStop`, 3 falhas em 167; depois, 0 em 210.
