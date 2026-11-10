{{/*
Labels comuns aplicados a todos os recursos
*/}}
{{- define "learning-lab.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Selector labels para Deployments e Services.
Uso: {{ include "learning-lab.selectorLabels" (dict "name" "product-api" "root" .) }}
(os nomes dos recursos são fixos, iguais aos manifests de k8s/, por isso não há helper de fullname)
*/}}
{{- define "learning-lab.selectorLabels" -}}
app: {{ .name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
{{- end }}
