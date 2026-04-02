# Diagrama de arquitetura — apresentação Itaú

## 1. Visão de contexto (C4 — nível de sistema)

Quem interage com a solução e qual o papel do legado:

```mermaid
flowchart LR
  subgraph externos [Atores externos]
    cliente[ClientePJEParceiros]
    canais[CanaisDigitais]
    mf[MainframeLegado]
  end

  subgraph solucao [Plataforma RecuperacaoPJ]
    boundary[SistemaModernizado]
  end

  cliente --> canais
  canais --> boundary
  boundary --> mf
```

---

## 2. Contêineres e portas

Microsserviços, barramento de eventos e **Anti-Corruption Layer** frente ao mainframe:

```mermaid
flowchart TB
  subgraph entrada [Entrada]
    gw[APIGateway]
  end

  subgraph servicos [Microsservicos_SpringBoot]
    reneg["renegociacao-service :8082"]
    cob["cobranca-service :8081"]
    pag["pagamento-service :8083"]
    notif["notificacao-service :8084"]
    acl["legado-acl-service :8085"]
  end

  kafka[ApacheKafka]

  gw --> reneg
  reneg --> kafka
  cob --> kafka
  pag --> kafka
  notif --> kafka
  reneg --> acl

  subgraph legado [Legado]
    mf[Mainframe]
  end

  acl --> mf
```

**Persistência e integração (resumo):**

| Contêiner | Armazenamento / barramento |
|-----------|----------------------------|
| renegociacao-service | PostgreSQL, Redis, Kafka |
| cobranca-service | Kafka |
| pagamento-service | Kafka |
| notificacao-service | Kafka (consumo) |
| legado-acl-service | HTTP de adaptação (sem banco no módulo) |

---

## 3. Infraestrutura local (Docker Compose — visão lógica)

Base de dados por contexto e mensageria (alinhado a *database per service* na infra):

```mermaid
flowchart TB
  subgraph compose [Docker_Compose]
    zk[Zookeeper]
    kfk[Kafka]
    pgRen[(PostgreSQL_Renegociacao)]
    pgCob[(PostgreSQL_Cobranca)]
    pgPag[(PostgreSQL_Pagamento)]
    rds[(Redis)]
    kui[KafkaUI]
  end

  subgraph apps [Aplicacoes]
    reneg[renegociacao-service]
    outr[cobranca_pagamento_notificacao]
    acl[legado-acl-service]
  end

  kfk --> zk
  reneg --> pgRen
  reneg --> rds
  reneg --> kfk
  outr --> kfk
  acl --> kfk
  kui --> kfk
```

---

## 4. Integração assíncrona (Kafka)

O núcleo de renegociação **publica** eventos de domínio; cobrança, pagamento e notificação **consomem** conforme configuração de cada serviço:

```mermaid
flowchart TB
  reneg[renegociacao-service]
  kafka[ApacheKafka]
  cob[cobranca-service]
  pag[pagamento-service]
  notif[notificacao-service]

  reneg -->|producao de eventos| kafka
  kafka -->|consumo| cob
  kafka -->|consumo| pag
  kafka -->|consumo| notif
```

**Tópicos** citados na documentação da infra (criação automática em ambiente local):

- `renegociacao.proposta.criada`
- `renegociacao.proposta.efetivada`
- `cobranca.acao.realizada`
- `pagamento.boleto.gerado`

---

## 5. Sequência simplificada — consulta via ACL

Leitura de dados sob responsabilidade do legado, com fronteira explícita:

```mermaid
sequenceDiagram
  participant Canal as CanaisDigitais
  participant GW as APIGateway
  participant Ren as renegociacao-service
  participant ACL as legado-acl-service
  participant MF as Mainframe

  Canal->>GW: HTTPS REST
  GW->>Ren: API v1 renegociacao
  Ren->>ACL: HTTP contrato moderno
  ACL->>MF: Adaptacao legado
  MF-->>ACL: Resposta legado
  ACL-->>Ren: DTO dominio
  Ren-->>GW: Resposta JSON
  GW-->>Canal: Resposta
```
