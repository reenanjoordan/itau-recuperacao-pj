# Plano de apresentação — Itaú Recuperação PJ

Documento auxiliar para apresentar o projeto **itau-recuperacao-pj** a gestores e avaliadores técnicos do Itaú. Ajuste tempos conforme o slot (sugestão: **30 min** com demo; **45 min** com perguntas e relatório técnico).

**Repositório:** https://github.com/reenanjoordan/itau-recuperacao-pj  
**Entrada principal:** [README.md](../README.md) · **Anexo técnico:** [relatorio-tecnico.md](relatorio-tecnico.md)

---

## Objetivo da reunião

- Posicionar a solução como **referência de modernização** da recuperação de crédito PJ (saindo do monólito mainframe em direção a microsserviços e eventos).
- Demonstrar **reprodutibilidade** (`mvn clean verify`, Docker) e **clareza arquitetural** (ACL, Kafka, banco por serviço).
- Deixar explícito o que é **demonstrável hoje** versus **evolução planejada**.

---

## Audiência e tom

| Perfil | O que priorizar |
|--------|------------------|
| Negócio / produto | Jornada do cliente PJ, benefícios de canais digitais, coexistência com legado (Strangler Fig). |
| Arquitetura / enterprise | Bounded contexts, barramento de eventos, ACL, *database per service*, desacoplamento. |
| Engenharia / segurança | Build com Enforcer, SpotBugs, SBOM CycloneDX, testes e JaCoCo no serviço principal. |

**Tom:** objetivo, sem oversell; deixar claro que é um **portfólio técnico de referência**, alinhado a padrões corporativos.

---

## Mensagens-chave (elevator pitch)

1. **Problema:** legado monolítico dificulta escala, time-to-market e observabilidade nas jornadas de renegociação PJ.  
2. **Abordagem:** **Strangler Fig** + **Kafka** + **ACL** (`legado-acl-service`) para isolar o mainframe e evoluir por incrementos.  
3. **Prova:** API documentada (OpenAPI), stack containerizada, testes e portões de qualidade no Maven, relatório técnico para *deep dive*.

---

## Roteiro sugerido (30 minutos)

| Min | Bloco | Conteúdo |
|-----|--------|----------|
| 0–3 | Abertura | Nome do projeto, seu papel, objetivo da sessão. |
| 3–10 | Contexto e escopo | Recuperação PJ; tabela de microsserviços e portas (README). Destaque `renegociacao-service` (8082) como núcleo da jornada. |
| 10–18 | Arquitetura | Diagrama ASCII ou Mermaid do README: canais → serviços → Kafka → ACL → mainframe. Uma frase por serviço (cobrança, pagamento, notificação). |
| 18–25 | Demo rápida | Infra com Docker; Swagger em `8082`; opcional: Kafka UI (`8090`) mostrando tópicos. Health do Actuator. |
| 25–28 | Qualidade | `mvn clean verify` na raiz; Enforcer, SpotBugs, CycloneDX; JaCoCo no módulo de renegociação. |
| 28–30 | Fechamento | Apontar [relatorio-tecnico.md](relatorio-tecnico.md); convite a perguntas; próximos passos (se houver). |

---

## Roteiro estendido (45 minutos)

Inclua após o bloco de arquitetura:

- **10 min — Padrões:** CQRS, saga (visão), circuit breaker (Resilience4j) na chamada ao legado; *database per service* e Postgres no Compose.  
- **5 min — Segurança e compliance de software:** SBOM; política de falha de build com SpotBugs; rastreabilidade de dependências.

Use o [relatorio-tecnico.md](relatorio-tecnico.md) como **script de backup** para perguntas profundas.

---

## Demo — checklist antes da reunião

- [ ] `docker compose up -d` testado; Kafka e Postgres saudáveis.  
- [ ] `mvn clean verify` passando na máquina de apresentação (ou gravar terminal as a prova).  
- [ ] `renegociacao-service` sobe com perfil esperado; Swagger abre em `http://localhost:8082/swagger-ui.html`.  
- [ ] Navegador com abas: Swagger, Actuator health, Kafka UI.  
- [ ] (Opcional) Stack completa: `docker compose -f docker-compose.yml -f docker-compose-services.yml up -d --build` após `mvn package -DskipTests`.  
- [ ] Backup: prints ou vídeo curto se a rede da empresa bloquear Docker.

---

## Perguntas frequentes (preparação)

- **“Isso substitui o mainframe?”** — Não imediatamente; o desenho **coexiste** com o legado via ACL e migra como Strangler Fig.  
- **“Onde está a persistência?”** — Renegociação: PostgreSQL + Redis; demais serviços no repositório estão **orientados a Kafka**; Compose já separa bases por contexto.  
- **“E CI/CD?”** — Build e testes são **reproduzíveis localmente**; pipeline corporativo (Jenkins/Azure DevOps) seria o passo seguinte em um ambiente Itaú.  
- **“Cobertura de testes?”** — Meta explícita de **80%** com JaCoCo no `renegociacao-service`; demais módulos no reactor do `mvn verify`.

---

## Materiais para enviar após a apresentação

- Link do repositório e branch `main`.  
- [README.md](../README.md) como “capa” do projeto.  
- [relatorio-tecnico.md](relatorio-tecnico.md) em PDF ou Markdown anexo.  
- (Opcional) Resultado de `mvn clean verify` e caminho do relatório JaCoCo em `renegociacao-service/target/site/jacoco/`.

---

## Pós-apresentação

Registrar feedback, decisões (“piloto”, “POC estendida”, “arquivar”) e owners para próximos passos — facilita alinhamento com o banco e evita expectativas ambíguas.
