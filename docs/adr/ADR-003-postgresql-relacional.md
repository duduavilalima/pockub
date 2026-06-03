# ADR-003 — PostgreSQL para Persistência Relacional

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

A POC precisa de um banco de dados relacional para armazenar os Produtos. O banco deve ser implantado como StatefulSet no Kubernetes para demonstrar persistência com PV/PVC.

---

## Decisão

Usar **PostgreSQL 17** como banco relacional.

Justificativas:
- Banco open-source mais adotado em ambientes corporativos modernos
- Imagem oficial Docker bem mantida e estável para uso em StatefulSet
- Suporte nativo no Spring Data JPA via JDBC driver padrão
- Versão 17 com melhorias de performance e JSON support (útil para extensões futuras)
- Familiar à maioria dos desenvolvedores Java

---

## Consequências

- Requer PVC configurado corretamente — falha no PV resulta em perda de dados
- StatefulSet com 1 réplica — sem replicação master/standby (suficiente para POC)
- Senha gerenciada via Secret K8s (ver [ADR-006](ADR-006-statefulset-databases.md))
- Não demonstra clustering de banco de dados — fora do escopo desta POC
