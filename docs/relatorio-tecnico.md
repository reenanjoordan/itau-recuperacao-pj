# Relatório Técnico — Modernização do Sistema de Recuperação de Crédito PJ

## Itaú Unibanco | Engenharia TI Pleno — Recuperação PJ

**Autor:** Engenharia de Software — Recuperação PJ  
**Data:** Abril/2026  
**Versão:** 1.0  
**Classificação:** Documento Interno  

---

## Sumário

1. [Introdução e Contexto](#1-introdução-e-contexto)
2. [Premissas Adotadas](#2-premissas-adotadas)
3. [Estratégia de Modernização — Strangler Fig Pattern](#3-estratégia-de-modernização--strangler-fig-pattern)
4. [Bounded Contexts e DDD](#4-bounded-contexts-e-ddd)
5. [Diagrama de Arquitetura](#5-diagrama-de-arquitetura)
6. [Design Patterns Aplicados](#6-design-patterns-aplicados)
7. [Detalhamento do renegociacao-service](#7-detalhamento-do-renegociacao-service)
8. [Estratégia de Testes](#8-estratégia-de-testes)
9. [Observabilidade](#9-observabilidade)
10. [Considerações de Segurança](#10-considerações-de-segurança)
11. [Perfil GitHub](#11-perfil-github)
12. [Referências](#12-referências)

---

## 1. Introdução e Contexto

### 1.1 Cenário Atual

O sistema de recuperação de crédito PJ do Itaú Unibanco opera sobre uma plataforma legada baseada em **mainframe IBM z/OS**, com aplicações desenvolvidas em **COBOL**, **Assembler**, persistência em **DB2** e **VSAM**, com ciclos de processamento **batch** diários e semanais. Este sistema possui entre **10 e 20 anos** de existência, acumulando débito técnico significativo e alto acoplamento entre módulos.

### 1.2 Problemas Identificados

- **Alto acoplamento:** Alterações em um módulo impactam diversos outros componentes do mainframe, elevando o risco de regressão e aumentando o tempo de entrega de novas funcionalidades.
- **Processamento batch:** O ciclo de processamento batch (diário/semanal) impede a oferta de serviços em tempo real, prejudicando a experiência do cliente e a competitividade do banco.
- **Escalabilidade limitada:** A arquitetura monolítica do mainframe não permite escalar componentes de forma independente, gerando gargalos em períodos de pico (fechamento fiscal, vencimento de impostos, sazonalidade de crédito PJ).
- **Escassez de profissionais:** A base de desenvolvedores COBOL está em declínio, tornando a manutenção evolutiva cada vez mais custosa.
- **Rigidez de integração:** Integrações com canais digitais (internet banking, app, portais de atendimento) exigem camadas de tradução complexas (MQ Series, CICS TG), com alta latência e baixa resiliência.

### 1.3 Objetivo da Modernização

Migrar gradualmente o sistema de recuperação de crédito PJ para uma **arquitetura de microsserviços**, utilizando tecnologias modernas (Java 17, Spring Boot 3.2, Apache Kafka, PostgreSQL, Redis), com processamento **online e em tempo real**, alta disponibilidade, escalabilidade horizontal e integração nativa com canais digitais.

### 1.4 Sazonalidade e Demanda

O sistema deve suportar **picos de demanda** associados a:
- Fechamento fiscal trimestral/anual de empresas
- Vencimento de impostos federais (DARF, DASN-SIMEI)
- Campanhas de renegociação com descontos sazonais (Feirão Limpa Nome PJ)
- Fim de períodos de carência de financiamentos PJ

A arquitetura proposta utiliza **auto-scaling** (AWS EKS) para responder dinamicamente a esses picos, mantendo SLAs de latência (p99 < 500ms) mesmo sob carga elevada.

---

## 2. Premissas Adotadas

As seguintes premissas guiaram todas as decisões de arquitetura e implementação:

| # | Premissa | Justificativa |
|---|----------|---------------|
| 1 | **Strangler Fig Pattern** | Migração gradual sem big-bang, permitindo operação paralela com o mainframe e rollback seguro a qualquer momento. |
| 2 | **Comunicação assíncrona (Kafka) + síncrona (REST)** | Kafka para eventos de domínio e integração entre serviços (desacoplamento temporal); REST para consultas síncronas e APIs externas. |
| 3 | **Anti-Corruption Layer (ACL) para o legado** | Isolamento do domínio moderno das complexidades do mainframe. O `legado-acl-service` traduz dados e protocolos, impedindo que conceitos legados contaminem os microsserviços. |
| 4 | **Segurança com JWT/OAuth2** | Autenticação e autorização via tokens JWT, integrado ao Identity Provider corporativo (OAuth2/OIDC), garantindo segurança zero-trust entre serviços. |
| 5 | **Cloud AWS (EKS, RDS, ElastiCache, MSK)** | Infraestrutura gerenciada com auto-scaling, alta disponibilidade multi-AZ, e serviços gerenciados para Kafka (MSK), PostgreSQL (RDS) e Redis (ElastiCache). |
| 6 | **Database per Service** | Cada microsserviço possui seu próprio banco de dados, garantindo independência de deployment, isolamento de falhas e liberdade de evolução de schema. |
| 7 | **Cobertura de testes mínima de 80%** | JaCoCo configurado para impedir builds com cobertura abaixo de 80% nas camadas de domínio e aplicação, garantindo qualidade e confiança para deploy contínuo. |

---

## 3. Estratégia de Modernização — Strangler Fig Pattern

### 3.1 Conceito

O **Strangler Fig Pattern** (Martin Fowler, 2004) é inspirado na figueira estranguladora que cresce ao redor de uma árvore hospedeira, gradualmente substituindo-a. Aplicado a software, significa construir o novo sistema ao redor do legado, migrando funcionalidades incrementalmente até que o mainframe possa ser descomissionado.

### 3.2 Fases de Migração

```
Fase 1 — Coexistência           Fase 2 — Migração Progressiva      Fase 3 — Descomissionamento
┌─────────────────────┐         ┌─────────────────────┐             ┌─────────────────────┐
│  Canais Digitais    │         │  Canais Digitais    │             │  Canais Digitais    │
│        │            │         │        │            │             │        │            │
│   API Gateway       │         │   API Gateway       │             │   API Gateway       │
│    /     \          │         │    /  |  \          │             │    /  |  \          │
│   MS    ACL→MF      │         │   MS  MS  ACL→MF   │             │   MS  MS  MS        │
│         (100%)      │         │        (30%)        │             │   (100% moderno)    │
└─────────────────────┘         └─────────────────────┘             └─────────────────────┘
```

**Fase 1 — Coexistência (atual):** Os microsserviços modernos operam em paralelo com o mainframe. O `legado-acl-service` traduz requisições entre os dois mundos. O roteamento no API Gateway direciona tráfego para o sistema correto.

**Fase 2 — Migração Progressiva:** Funcionalidades são migradas uma a uma. O tráfego é gradualmente desviado do mainframe para os microsserviços (canary/blue-green). Cada migração é validada com métricas de negócio.

**Fase 3 — Descomissionamento:** Após todas as funcionalidades migradas e validadas em produção, o mainframe é descomissionado. O `legado-acl-service` é removido.

### 3.3 Capacidade de Rollback

A qualquer momento durante as Fases 1 e 2, é possível reverter o tráfego para o mainframe via configuração do API Gateway, sem downtime. Esta é uma garantia fundamental para operações críticas de um banco de grande porte.

---

## 4. Bounded Contexts e DDD

A decomposição dos microsserviços segue os princípios do **Domain-Driven Design (DDD)**, com cada serviço representando um **Bounded Context** distinto:

| Bounded Context | Serviço | Porta | Responsabilidades |
|----------------|---------|-------|-------------------|
| **Renegociação** | `renegociacao-service` | 8082 | Simulação de acordos, criação de propostas, efetivação de renegociações, gestão do ciclo de vida da proposta. Serviço core do domínio. |
| **Cobrança** | `cobranca-service` | 8081 | Gestão de ações de cobrança (ligação, e-mail, SMS, carta, visita), régua de cobrança, acompanhamento de contatos com devedores. |
| **Pagamento** | `pagamento-service` | 8083 | Geração de boletos, processamento de pagamentos (boleto/débito em conta), conciliação financeira. |
| **Notificação** | `notificacao-service` | 8084 | Envio multicanal de notificações (e-mail, SMS, carta), gestão de templates, tracking de entregas. |
| **Integração Legado** | `legado-acl-service` | 8085 | Anti-Corruption Layer — tradução de dados do mainframe, consulta de dívidas e clientes no sistema legado. |

### 4.1 Comunicação entre Bounded Contexts

- **Eventos de domínio (Kafka):** Proposta criada → Notificação; Proposta efetivada → Cobrança, Pagamento, Notificação
- **Chamadas síncronas (REST):** Renegociação → Legado ACL (consulta de dívidas/clientes)
- **Princípio:** Cada contexto é autônomo e se comunica apenas via contratos bem definidos (eventos ou APIs)

---

## 5. Diagrama de Arquitetura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CANAIS DIGITAIS                                   │
│              Internet Banking  │  App Mobile  │  Portal Atendimento         │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │    API GATEWAY       │
                        │  (Spring Cloud GW)   │
                        │  JWT Validation      │
                        │  Rate Limiting       │
                        │  Load Balancing      │
                        └──────────┬──────────┘
                                   │
            ┌──────────┬───────────┼───────────┬──────────────┐
            │          │           │           │              │
            ▼          ▼           ▼           ▼              ▼
     ┌──────────┐┌──────────┐┌──────────┐┌──────────┐┌────────────┐
     │ cobranca ││renegocia-││pagamento ││notifica- ││ legado-acl │
     │ service  ││cao-serv. ││ service  ││cao-serv. ││  service   │
     │  :8081   ││  :8082   ││  :8083   ││  :8084   ││   :8085    │
     └────┬─────┘└──┬───┬───┘└────┬─────┘└────┬─────┘└─────┬──────┘
          │         │   │         │            │            │
          │         │   │         │            │            ▼
          │         │   │         │            │     ┌──────────────┐
          │         │   │         │            │     │  MAINFRAME   │
          │         │   │         │            │     │  (COBOL/DB2  │
          │         │   │         │            │     │   /VSAM)     │
          │         │   │         │            │     └──────────────┘
          │         │   │         │            │
          ▼         ▼   │         ▼            ▼
     ┌─────────────────────────────────────────────┐
     │              APACHE KAFKA (MSK)              │
     │                                              │
     │  Topics:                                     │
     │  ├── renegociacao.proposta.criada            │
     │  ├── renegociacao.proposta.efetivada         │
     │  ├── cobranca.acao.realizada                 │
     │  └── pagamento.boleto.gerado                │
     └─────────────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
          ▼              ▼              ▼
   ┌────────────┐ ┌────────────┐ ┌────────────┐
   │ PostgreSQL │ │   Redis    │ │ Prometheus  │
   │  (RDS)     │ │(ElastiCache│ │ + Grafana   │
   │            │ │   )        │ │             │
   │ renegocia- │ │ Cache:     │ │ Métricas    │
   │ cao_db     │ │ simulações │ │ Alertas     │
   └────────────┘ │ propostas  │ │ Dashboards  │
                  └────────────┘ └────────────┘
```

### 5.1 Fluxo Principal

1. O cliente acessa o portal/app e realiza uma simulação de renegociação
2. O **API Gateway** valida o token JWT e roteia para o `renegociacao-service`
3. O `renegociacao-service` consulta o `legado-acl-service` para obter dívidas do mainframe
4. Com os dados, calcula opções de parcelamento e retorna ao cliente
5. Ao aceitar, uma **Proposta** é criada e um evento `proposta.criada` é publicado no Kafka
6. O `notificacao-service` consome o evento e envia confirmação por e-mail/SMS
7. Na efetivação, o evento `proposta.efetivada` dispara a **Saga Coreografada**:
   - `pagamento-service` gera boletos
   - `cobranca-service` agenda ações de acompanhamento
   - `notificacao-service` envia confirmação final

---

## 6. Design Patterns Aplicados

| Pattern | Onde é Aplicado | Justificativa |
|---------|----------------|---------------|
| **Strangler Fig** | Arquitetura geral | Migração gradual do mainframe sem big-bang, operação paralela com rollback seguro |
| **Anti-Corruption Layer (ACL)** | `legado-acl-service` | Isolamento total entre domínio moderno e mainframe legado; tradução de modelos de dados e protocolos |
| **CQRS** | `renegociacao-service` | Separação de comandos (criar/efetivar proposta) de consultas (listar propostas/simulações), permitindo otimização independente de cada caminho |
| **Event Sourcing** | Eventos de domínio (Kafka) | Rastreabilidade completa de todas as mudanças de estado; auditoria nativa; replay de eventos para reconstrução de estado |
| **Saga (Choreography)** | Efetivação de proposta | Coordenação distribuída sem orquestrador central — cada serviço reage a eventos e publica seus próprios eventos, garantindo consistência eventual |
| **Circuit Breaker** | `renegociacao-service` → `legado-acl-service` | Proteção contra falhas em cascata quando o mainframe está indisponível; fallback com dados em cache |
| **Strategy** | `renegociacao-service` (cálculo de parcelas), `notificacao-service` (canais) | Algoritmos de cálculo intercambiáveis (juros simples, sem juros); canais de notificação plugáveis |
| **Factory** | `notificacao-service` (`NotificacaoChannelFactory`) | Criação dinâmica do canal correto de notificação baseado no tipo solicitado, com registro automático via Spring |
| **Builder** | Todos os modelos de domínio (Lombok `@Builder`) | Construção fluente de objetos complexos com validação em tempo de compilação |
| **Repository** | `renegociacao-service` (JPA Repositories) | Abstração de acesso a dados que permite trocar a implementação de persistência sem impactar o domínio |
| **Database per Service** | Todos os serviços | Cada microsserviço possui banco exclusivo, garantindo independência de deploy, isolamento de falhas e liberdade de schema |

---

## 7. Detalhamento do renegociacao-service

O `renegociacao-service` é o serviço core do domínio, responsável por todo o ciclo de vida de uma renegociação de dívida PJ.

### 7.1 Fluxo de Simulação

```
Cliente → POST /api/v1/renegociacao/simulacoes
         Body: { cpfCnpj, valorDivida, numeroParcelas, tipoAcordo }

1. Controller recebe request e valida campos (@Valid)
2. Service verifica cache Redis (chave: "simulacao:{cpfCnpj}:{valorDivida}:{parcelas}:{tipo}")
   ├── Cache HIT → retorna resultado imediato (latência ~5ms)
   └── Cache MISS → prossegue cálculo
3. Service seleciona CalculoParcelaStrategy via tipo de acordo:
   ├── PARCELAMENTO → CalculoJurosSimples (1.5% a.m.)
   └── DESCONTO_AVISTA → CalculoSemJuros (30% desconto)
4. Strategy calcula parcelas com valores, datas e totais
5. Resultado é salvo no Redis com TTL de 30 minutos
6. Response retornada ao cliente com opções de parcelamento
```

### 7.2 Fluxo de Criação de Proposta

```
Cliente → POST /api/v1/renegociacao/propostas
         Body: { cpfCnpj, valorDivida, numeroParcelas, tipoAcordo }

1. Controller recebe e valida request
2. Service consulta legado-acl-service via REST (com Circuit Breaker):
   GET http://legado-acl-service:8085/api/v1/legado/dividas/{cpfCnpj}
3. Valida elegibilidade da dívida:
   ├── diasAtraso >= 30 → elegível
   ├── valorAtualizado > 100 → elegível
   └── Caso contrário → DividaInelegivelException (HTTP 422)
4. Cria entidade Proposta com status CRIADA
5. Persiste no PostgreSQL via JPA Repository
6. Publica evento PropostaCriadaEvent no Kafka (topic: renegociacao.proposta.criada)
7. Retorna PropostaResponse com ID e detalhes
```

### 7.3 Fluxo de Efetivação — Saga Choreography

A efetivação de uma proposta dispara uma **Saga Coreografada** envolvendo 4 microsserviços:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        SAGA: EFETIVAÇÃO DE PROPOSTA                         │
│                                                                              │
│  Passo 1: renegociacao-service                                              │
│  ├── Recebe PUT /api/v1/renegociacao/propostas/{id}/efetivar                │
│  ├── Valida proposta (status = CRIADA)                                      │
│  ├── Atualiza status → EFETIVADA                                            │
│  ├── Persiste no PostgreSQL                                                 │
│  └── Publica evento: renegociacao.proposta.efetivada                        │
│           │                                                                  │
│  ─────────┼──────────────────────────────────────────────────────            │
│           │                                                                  │
│  Passo 2: pagamento-service (consome evento)                                │
│  ├── Recebe evento renegociacao.proposta.efetivada                          │
│  ├── Gera boleto com código de barras                                       │
│  ├── Persiste boleto em memória                                             │
│  └── Publica evento: pagamento.boleto.gerado                               │
│           │                                                                  │
│  Passo 3: cobranca-service (consome evento)                                 │
│  ├── Recebe evento renegociacao.proposta.efetivada                          │
│  ├── Cria ações de cobrança (e-mail + SMS automáticos)                      │
│  └── Publica evento: cobranca.acao.realizada                               │
│           │                                                                  │
│  Passo 4: notificacao-service (consome evento)                              │
│  ├── Recebe evento renegociacao.proposta.efetivada                          │
│  ├── Envia e-mail de confirmação da renegociação                            │
│  └── Envia SMS com resumo e link para boleto                               │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**Característica da Saga Coreografada:** Não há orquestrador central. Cada serviço conhece apenas seus eventos de entrada e saída. Isso garante baixo acoplamento, mas exige monitoramento robusto para detectar sagas incompletas.

### 7.4 CQRS (Command Query Responsibility Segregation)

O `renegociacao-service` implementa separação clara entre comandos e consultas:

**Comandos (escrita):**
- `POST /simulacoes` → Criar simulação
- `POST /propostas` → Criar proposta
- `PUT /propostas/{id}/efetivar` → Efetivar proposta

**Consultas (leitura):**
- `GET /propostas/{id}` → Buscar proposta por ID
- `GET /propostas?cpfCnpj=xxx` → Listar propostas por CPF/CNPJ

**Benefícios:**
- Comandos podem ser otimizados para escrita (validações, eventos, transações)
- Consultas podem ser otimizadas para leitura (cache, projeções, índices)
- Escalabilidade independente (mais réplicas de leitura sob demanda)

### 7.5 Cache Redis

#### Estratégia de Cache

| Dado | Chave Redis | TTL | Estratégia de Invalidação |
|------|-------------|-----|---------------------------|
| Simulações | `simulacao:{cpfCnpj}:{valor}:{parcelas}:{tipo}` | 30 min | TTL automático; invalidação manual em mudança de regras |
| Propostas (leitura) | `proposta:{id}` | 15 min | Invalidação on-write (ao efetivar/cancelar) |
| Dívidas do legado | `dividas:{cpfCnpj}` | 10 min | TTL automático; Circuit Breaker fallback |
| Dados do cliente | `cliente:{cpfCnpj}` | 60 min | TTL automático |

#### Padrão Cache-Aside

```
1. Consulta chega → verifica Redis
2. Cache HIT → retorna dado cacheado (latência ~2-5ms)
3. Cache MISS → consulta fonte (DB/legado) → salva no Redis → retorna
4. Escrita/Atualização → atualiza fonte → invalida/atualiza Redis
```

#### Métricas de Cache

- `cache.hit.ratio` — taxa de acerto (meta: > 70%)
- `cache.miss.count` — total de misses
- `cache.eviction.count` — evicções por TTL ou memória

### 7.6 Circuit Breaker (Resilience4j)

#### Configuração

```yaml
resilience4j:
  circuitbreaker:
    instances:
      legadoService:
        sliding-window-size: 10          # Janela de 10 chamadas
        failure-rate-threshold: 50        # Abre com 50% de falhas
        wait-duration-in-open-state: 30s  # Espera 30s antes de half-open
        permitted-number-of-calls-in-half-open-state: 3  # 3 tentativas em half-open
        record-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
          - org.springframework.web.client.ResourceAccessException
  retry:
    instances:
      legadoService:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
```

#### Estados do Circuit Breaker

```
CLOSED (normal) ──[50% falhas]──► OPEN (bloqueado)
                                      │
                                  [30s timeout]
                                      │
                                      ▼
                                 HALF-OPEN (teste)
                                   /         \
                          [sucesso]           [falha]
                              │                  │
                              ▼                  ▼
                           CLOSED              OPEN
```

#### Fallback

Quando o Circuit Breaker está OPEN (mainframe indisponível):
1. Retorna dados do cache Redis (se disponíveis)
2. Se cache vazio, retorna resposta degradada com mensagem informativa
3. Registra métrica `circuitbreaker.legadoService.state` para alertas

#### Métricas Exportadas

- `resilience4j.circuitbreaker.state` — estado atual (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j.circuitbreaker.calls` — chamadas por resultado (success/error/ignored)
- `resilience4j.circuitbreaker.failure.rate` — taxa de falha atual

---

## 8. Estratégia de Testes

### 8.1 Pirâmide de Testes

A estratégia de testes segue a **Pirâmide de Testes** clássica, com foco em testes unitários para máxima velocidade e confiança:

```
          ╱  ╲
         ╱ E2E╲         10% — Testes End-to-End
        ╱  10%  ╲       Testcontainers + RestAssured
       ╱─────────╲      Fluxos completos com Kafka + PostgreSQL
      ╱Integration╲     
     ╱    20%      ╲    20% — Testes de Integração
    ╱───────────────╲   @SpringBootTest + Testcontainers
   ╱    Unit Tests   ╲  Repositórios, Controllers, Kafka
  ╱       70%         ╲ 
 ╱─────────────────────╲ 70% — Testes Unitários
╱    Mockito + JUnit 5   ╲ Services, Strategies, Factories
╱─────────────────────────╲
```

| Tipo | Proporção | Tecnologias | Escopo |
|------|-----------|-------------|--------|
| **Unitário** | 70% | JUnit 5, Mockito, AssertJ | Services, strategies, factories, validators, domain models |
| **Integração** | 20% | @SpringBootTest, Testcontainers (PostgreSQL, Kafka), @WebMvcTest | Repositórios JPA, controllers REST, consumers/producers Kafka |
| **E2E** | 10% | Testcontainers, RestAssured, Docker Compose | Fluxos completos de simulação → proposta → efetivação com todos os serviços |

### 8.2 Cobertura de Código (JaCoCo)

**Configuração:** O plugin JaCoCo está configurado em todos os `pom.xml` para garantir cobertura mínima.

```xml
<configuration>
    <rules>
        <rule>
            <element>BUNDLE</element>
            <limits>
                <limit>
                    <counter>INSTRUCTION</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum>
                </limit>
            </limits>
        </rule>
    </rules>
</configuration>
```

**Meta de cobertura por camada:**

| Camada | Cobertura Mínima | Justificativa |
|--------|-----------------|---------------|
| `domain/` | 90% | Lógica de negócio crítica — zero tolerância a bugs |
| `application/` | 85% | Orquestração de casos de uso — alta criticidade |
| `api/` | 75% | Controllers REST — cobertos por testes de integração |
| `infrastructure/` | 70% | Configurações e integrações — cobertos por testes E2E |

**Build Gate:** O build Maven **falha** se a cobertura total cair abaixo de 80%, impedindo merge no branch principal.

---

## 9. Observabilidade

### 9.1 Stack de Observabilidade

A solução utiliza a tríade de observabilidade: **métricas, logs e traces**.

| Pilar | Tecnologia | Função |
|-------|-----------|--------|
| **Métricas** | Micrometer + Prometheus + Grafana | Coleta e visualização de métricas de negócio e técnicas |
| **Logs** | SLF4J + Logback + ELK Stack | Logs estruturados com correlação via traceId |
| **Traces** | Spring Actuator + MDC (traceId) | Rastreamento distribuído de requisições entre serviços |

### 9.2 Métricas Customizadas (Micrometer)

```java
// Métricas de negócio
Counter propostas_criadas = Counter.builder("propostas.criadas")
    .tag("tipo", tipoAcordo.name())
    .register(meterRegistry);

Timer simulacao_timer = Timer.builder("simulacao.duracao")
    .tag("tipo", tipoAcordo.name())
    .register(meterRegistry);

Gauge.builder("propostas.pendentes", propostaRepository, repo -> repo.countByStatus("PENDENTE"))
    .register(meterRegistry);
```

**Métricas expostas:**

| Métrica | Tipo | Descrição |
|---------|------|-----------|
| `propostas.criadas` | Counter | Total de propostas criadas por tipo de acordo |
| `propostas.efetivadas` | Counter | Total de propostas efetivadas |
| `simulacao.duracao` | Timer | Tempo de processamento de simulações |
| `propostas.pendentes` | Gauge | Número atual de propostas em status PENDENTE |
| `legado.consulta.duracao` | Timer | Latência de consulta ao mainframe |
| `kafka.mensagens.publicadas` | Counter | Total de eventos publicados por tópico |
| `cache.hit.ratio` | Gauge | Taxa de acerto do cache Redis |

### 9.3 Actuator Endpoints

Todos os serviços expõem endpoints do Spring Boot Actuator:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

- `/actuator/health` — Health check (liveness + readiness)
- `/actuator/info` — Informações do build e versão
- `/actuator/metrics` — Métricas Micrometer
- `/actuator/prometheus` — Métricas no formato Prometheus

### 9.4 Correlação de Logs (MDC traceId)

Cada requisição recebe um `traceId` único que é propagado via MDC (Mapped Diagnostic Context) entre todos os serviços, permitindo rastrear o fluxo completo de uma operação:

```
2026-04-01 10:15:30 [traceId=abc123] INFO  renegociacao-service — Simulação criada para CNPJ: ***456
2026-04-01 10:15:30 [traceId=abc123] INFO  legado-acl-service   — Consultando dívidas no mainframe
2026-04-01 10:15:31 [traceId=abc123] INFO  renegociacao-service — Proposta criada: PRO-789
2026-04-01 10:15:31 [traceId=abc123] INFO  notificacao-service  — E-mail enviado para ***@empresa.com
```

---

## 10. Considerações de Segurança

### 10.1 Autenticação e Autorização

| Mecanismo | Implementação |
|-----------|---------------|
| **Autenticação** | JWT (JSON Web Token) emitido pelo Identity Provider corporativo (OAuth2/OIDC) |
| **Autorização** | Claims no JWT com roles (`ROLE_OPERADOR`, `ROLE_GESTOR`, `ROLE_ADMIN`) |
| **Validação** | Cada microsserviço valida o JWT localmente (chave pública RSA) — sem chamada ao IdP |
| **Refresh** | Access Token com TTL de 15 minutos; Refresh Token com TTL de 8 horas |

### 10.2 Comunicação Segura

- **HTTPS/TLS 1.3** em todas as comunicações externas (cliente → API Gateway)
- **mTLS** (mutual TLS) entre microsserviços dentro do cluster EKS
- **Kafka SSL** para comunicação com o broker MSK

### 10.3 Gestão de Secrets

- **AWS Secrets Manager** para credenciais de banco de dados, chaves API e certificados
- **Rotação automática** de secrets a cada 90 dias
- Nenhuma credencial em código fonte, variáveis de ambiente ou arquivos de configuração

### 10.4 Proteção de Dados Sensíveis

```java
// Mascaramento de CPF/CNPJ em logs
private String mascararCpfCnpj(String cpfCnpj) {
    if (cpfCnpj.length() == 11) {
        return "***" + cpfCnpj.substring(3, 6) + "***";  // ***456***
    }
    return "***" + cpfCnpj.substring(3, 7) + "***";       // ***4567***
}
```

- CPF/CNPJ **nunca** aparece completo em logs
- Dados bancários (conta, agência) são mascarados
- Conformidade com LGPD (Lei Geral de Proteção de Dados)
- Logs de auditoria para acesso a dados sensíveis

### 10.5 Proteção contra Ataques

| Ameaça | Proteção |
|--------|----------|
| SQL Injection | JPA/Hibernate com Prepared Statements |
| XSS | Spring Security headers + Content Security Policy |
| CSRF | Desabilitado (API stateless com JWT) |
| DDoS | Rate Limiting no API Gateway (100 req/min por IP) |
| Brute Force | Account lockout após 5 tentativas |

---

## 11. Perfil GitHub

Repositório do projeto:

**URL:** [https://github.com/reenanjoordan/itau-recuperacao-pj](https://github.com/reenanjoordan/itau-recuperacao-pj)

### Estrutura do Repositório

```
itau-recuperacao-pj/
├── renegociacao-service/        # Serviço core de renegociação (porta 8082)
├── cobranca-service/            # Serviço de cobrança (porta 8081)
├── pagamento-service/           # Serviço de pagamento (porta 8083)
├── notificacao-service/         # Serviço de notificação (porta 8084)
├── legado-acl-service/          # Anti-Corruption Layer (porta 8085)
├── docs/                        # Documentação (relatório técnico, guia de validação)
├── docker-compose.yml           # Orquestração local (infra)
├── docker-compose-services.yml  # Microsserviços (sobrepor ao compose de infra)
├── pom.xml                      # POM pai (Maven multi-module)
└── README.md                    # Documentação geral
```

---

## 12. Referências

### Livros

1. **VERNON, Vaughn.** *Implementing Domain-Driven Design.* Addison-Wesley, 2013. — Referência principal para modelagem de domínio e Bounded Contexts.

2. **NEWMAN, Sam.** *Building Microservices: Designing Fine-Grained Systems.* O'Reilly, 2nd Edition, 2021. — Arquitetura de microsserviços, decomposição e comunicação.

3. **RICHARDSON, Chris.** *Microservices Patterns: With Examples in Java.* Manning, 2018. — Patterns de microsserviços (Saga, CQRS, Event Sourcing, Database per Service).

4. **FOWLER, Martin.** *Patterns of Enterprise Application Architecture.* Addison-Wesley, 2002. — Padrões fundamentais (Repository, Unit of Work, Domain Model).

5. **KLEPPMANN, Martin.** *Designing Data-Intensive Applications.* O'Reilly, 2017. — Sistemas distribuídos, consistência eventual, streaming de eventos.

6. **GAMMA, Erich et al.** *Design Patterns: Elements of Reusable Object-Oriented Software.* Addison-Wesley, 1994. — Padrões GoF (Strategy, Factory, Builder, Observer).

### Documentação Técnica

7. **Spring Boot Reference Documentation.** https://docs.spring.io/spring-boot/docs/3.2.x/reference/ — Framework base da solução.

8. **Apache Kafka Documentation.** https://kafka.apache.org/documentation/ — Plataforma de streaming de eventos.

9. **Resilience4j User Guide.** https://resilience4j.readme.io/docs — Circuit Breaker e padrões de resiliência.

10. **AWS Well-Architected Framework.** https://aws.amazon.com/architecture/well-architected/ — Boas práticas de arquitetura cloud.

11. **Martin Fowler — Strangler Fig Application.** https://martinfowler.com/bliki/StranglerFigApplication.html — Padrão de migração gradual.

12. **Testcontainers Documentation.** https://testcontainers.com/guides/ — Testes de integração com containers.

### Normativos e Compliance

13. **LGPD — Lei nº 13.709/2018.** Lei Geral de Proteção de Dados Pessoais — Regulamentação de tratamento de dados pessoais.

14. **Resolução CMN nº 4.893/2021.** Política de segurança cibernética para instituições financeiras.

---

*Documento gerado pela Engenharia de Software — Recuperação PJ | Itaú Unibanco*  
*Classificação: Documento Interno | Atualizado em Abril/2026*
