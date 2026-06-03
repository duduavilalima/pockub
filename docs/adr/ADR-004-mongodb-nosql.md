# ADR-004 — MongoDB para Logs de Acesso

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

A POC precisa de um segundo banco de dados para demonstrar StatefulSet com tecnologia diferente e justificar o uso de armazenamento NoSQL. O caso de uso escolhido é o registro de logs de acesso (auditoria).

Logs de acesso são um caso de uso natural para NoSQL:
- Schema flexível (campos podem variar por versão da aplicação)
- Alta taxa de escrita
- Consultas por range de tempo ou por produto

---

## Decisão

Usar **MongoDB** para armazenar os logs de acesso (collection `access_logs`).

Justificativas:
- Banco NoSQL orientado a documentos mais adotado no mercado
- Spring Data MongoDB com suporte nativo no ecossistema Spring Boot
- Demonstra StatefulSet com tecnologia diferente do PostgreSQL — dois padrões de persistência num único projeto
- Schema flexível adequado para logs — campos podem ser acrescentados sem migração
- Contraste pedagógico com o modelo relacional do PostgreSQL

---

## Consequências

- `produtoId` armazenado por valor (sem FK real) — consistência eventual entre os bancos
- `nomeProduto` é snapshot imutável — não reflete renomeações futuras (intencional para auditoria)
- Requer segundo StatefulSet, PV e PVC — mais manifests para gerenciar
- Sem autenticação configurada na POC (simplificação) — em produção exige autenticação obrigatória
