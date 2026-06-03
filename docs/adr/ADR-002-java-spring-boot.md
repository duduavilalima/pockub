# ADR-002 — Java 21 e Spring Boot 4

**Status:** Aceito
**Data:** Junho/2026

---

## Contexto

A POC precisa de uma aplicação backend simples o suficiente para não desviar o foco do aprendizado de Kubernetes, mas representativa de um stack corporativo real. A linguagem e o framework devem ser familiares à equipe de estudos.

---

## Decisão

Usar **Java 21** com **Spring Boot 4**.

Justificativas:
- Stack amplamente adotado em ambientes corporativos brasileiros — alinhado ao objetivo de simular ambiente real
- Spring Boot 4 com Java 21 explora Virtual Threads (Project Loom), melhorando throughput sob carga (relevante para o cenário de HPA)
- Spring Actuator provê endpoints de health check prontos para Liveness, Readiness e Startup Probes sem código adicional
- Spring Data JPA e Spring Data MongoDB reduzem boilerplate, mantendo o foco no aprendizado de K8s

---

## Consequências

- Imagem Docker maior comparada a Go ou Node.js — exige configuração de resource limits adequados
- Tempo de startup da JVM requer `startupProbe` com tolerância maior (ex: 60s)
- Build da imagem mais lento — considerar multi-stage Dockerfile para otimizar tamanho
- Virtual Threads disponíveis a partir do Java 21 LTS
