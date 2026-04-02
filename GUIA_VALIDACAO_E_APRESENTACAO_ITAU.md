# ✅ Guia de Validação + 🎤 Plano de Apresentação
## Case Técnico Itaú | Recuperação PJ

---

# PARTE 1 — VALIDAÇÃO DO PROJETO GERADO PELO CURSOR

> Execute cada passo na ordem. Se um passo falhar, corrija antes de avançar.

---

## PASSO 1 — Verificar Pré-requisitos da Máquina

```bash
# Verificar Java 17
java -version
# Esperado: openjdk 17.x.x

# Verificar Maven
mvn -version
# Esperado: Apache Maven 3.9.x

# Verificar Docker
docker --version
docker compose version
# Esperado: Docker version 24+, Compose v2+

# Verificar que as portas estão livres (macOS/Linux):
lsof -i :5432   # PostgreSQL
lsof -i :6379   # Redis
lsof -i :9092   # Kafka
lsof -i :8082   # renegociacao-service
# Se alguma estiver ocupada: sudo kill -9 $(lsof -t -i:PORTA)
# Windows (PowerShell/CMD): netstat -ano | findstr :5432
```

---

## PASSO 2 — Compilar o Projeto Completo

```bash
# Na raiz do repositório (pasta que contém pom.xml e este guia)
cd itau-recuperacao-pj

# Limpar e compilar todos os módulos sem rodar testes ainda
mvn clean compile -f pom.xml

# O que esperar:
# [INFO] BUILD SUCCESS
# Se houver erros de compilação, o Cursor gerou algo errado — anote o erro e peça correção
```

---

## PASSO 3 — Subir a Infraestrutura (Docker)

```bash
# Na raiz do projeto
docker compose up -d

# Aguardar ~30 segundos e verificar se todos os containers estão healthy
docker compose ps

# Você deve ver todos com status "Up" ou "healthy":
# ✅ zookeeper
# ✅ kafka
# ✅ postgres-renegociacao
# ✅ postgres-cobranca
# ✅ postgres-pagamento
# ✅ redis
# ✅ kafka-ui

# Verificar logs se algo não subiu:
docker compose logs kafka
docker compose logs postgres-renegociacao
docker compose logs redis

# Testar conexão PostgreSQL manualmente (banco sobe vazio; tabelas Flyway aparecem após subir o renegociacao-service):
docker exec -it $(docker ps -qf "name=postgres-renegociacao") \
  psql -U renegociacao -d renegociacao_db -c "\dt"
# Antes do primeiro run da aplicação: "Did not find any relations." — esperado.

# Stack completa com legado-acl + microserviços (mesma rede Docker):
# docker compose -f docker-compose.yml -f docker-compose-services.yml up -d

# Testar conexão Redis:
docker exec -it $(docker ps -qf "name=redis") redis-cli ping
# Deve retornar: PONG
```

---

## PASSO 4 — Rodar os Testes Unitários

```bash
# Rodar apenas testes unitários (rápido, sem Docker necessário)
cd renegociacao-service
mvn test -Dgroups="unit"

# Ou rodar por classe específica:
mvn test -Dtest=CalculoJurosSimplesTest
mvn test -Dtest=CalculoJurosCompostosTest
mvn test -Dtest=CalculoSemJurosTest
mvn test -Dtest=CriarPropostaCommandHandlerTest
mvn test -Dtest=RenegociacaoControllerTest

# Ver resultado no terminal:
# Tests run: X, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS ✅

# Ver relatório HTML de testes:
open target/surefire-reports/index.html   # Mac
xdg-open target/surefire-reports/index.html  # Linux
```

---

## PASSO 5 — Rodar os Testes de Integração (Testcontainers)

```bash
# Os testes de integração sobem PostgreSQL + Kafka + Redis via Docker automaticamente
# Docker precisa estar rodando

cd renegociacao-service
mvn verify -Dgroups="integration"

# Primeira execução é mais lenta (baixa imagens Docker)
# Esperar ~2-5 minutos

# O que vai acontecer nos bastidores:
# ✅ Testcontainers sobe PostgreSQL isolado
# ✅ Flyway roda as migrations
# ✅ Testcontainers sobe Kafka isolado
# ✅ Testcontainers sobe Redis isolado
# ✅ Testes rodam contra infra real
# ✅ Containers são destruídos ao final

# Ver cobertura de código (JaCoCo):
open target/site/jacoco/index.html
# Deve mostrar >= 80% nas camadas domain e application
```

### Windows + Docker Desktop — `BadRequestException (Status 400)` no Testcontainers

**Docker Engine 29+** (ex.: API **1.54**, como no `docker version`) exige cliente com API **≥ 1.44**. Versões antigas do docker-java usavam **1.32** por padrão e o daemon responde com **400** — sintoma: `Could not find a valid Docker environment` e JSON de `/info` “vazio” nos logs.

O projeto já trata isso com **Testcontainers 1.21.4+**, **`docker-java.properties`** (`api.version=1.54`) e **`api.version`** no Surefire (`renegociacao-service/pom.xml`). Se você atualizar o Docker e a API mudar, ajuste `docker.test.api.version` no POM e `api.version` no `docker-java.properties`.

Se ainda falhar após isso:

1. Confirme que o **engine** está ok: `docker info` deve preencher **Server Version**, **Operating System**, etc. Se vier vazio, reinicie o Docker Desktop e espere ficar verde.
2. **`DOCKER_HOST=npipe:////./pipe/docker_engine`** no Surefire e **`testcontainers.properties`** em `renegociacao-service/src/test/resources/`.
3. **Plano B (TCP):** no Docker Desktop → **Settings → General** → ative **“Expose daemon on tcp://localhost:2375 without TLS”** (só em máquina de dev). Depois rode:
   ```bash
   mvn verify -Dgroups=integration -Pdocker-tcp -pl renegociacao-service
   ```
4. **Plano C:** rode o Maven **dentro do WSL2** (Ubuntu) com integração Docker Desktop para WSL ativada — o cliente Java enxerga o socket Unix como no Linux.

---

## PASSO 6 — Rodar TODOS os Testes + Cobertura

```bash
# Na raiz — roda todos os módulos
mvn clean verify

# Resultado esperado:
# [INFO] renegociacao-service ............... SUCCESS
# [INFO] cobranca-service .................. SUCCESS
# [INFO] pagamento-service ................. SUCCESS
# [INFO] notificacao-service ............... SUCCESS
# [INFO] legado-acl-service ................ SUCCESS
# [INFO] BUILD SUCCESS ✅

# Se a cobertura estiver abaixo de 80%, o build FALHA — isso é intencional (JaCoCo)
```

---

## PASSO 7 — Subir o Serviço e Testar Manualmente

```bash
# Subir o renegociacao-service localmente
cd renegociacao-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Aguardar a mensagem no log:
# Started RenegociacaoApplication in X.XXX seconds

# Verificar health check:
curl http://localhost:8082/actuator/health | python3 -m json.tool
# Esperado:
# {
#   "status": "UP",
#   "components": {
#     "db": { "status": "UP" },
#     "redis": { "status": "UP" },
#     "kafka": { "status": "UP" }
#   }
# }

# Abrir Swagger UI no browser:
open http://localhost:8082/swagger-ui.html
```

---

## PASSO 8 — Testar os Endpoints via Swagger ou cURL

### 8.1 Buscar dívidas elegíveis
CNPJ no path aceita a barra (`/`) como parte do documento (`{*cpfCnpj}`); use URL-encoded em clientes que não enviam a barra literal.

```bash
curl -X GET "http://localhost:8082/api/v1/renegociacao/dividas/12.345.678%2F0001-99" \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Accept: application/json" | python3 -m json.tool

# Esperado: HTTP 200 com lista de dívidas
```

### 8.2 Simular proposta (3 opções de cálculo)
```bash
curl -X POST "http://localhost:8082/api/v1/renegociacao/simular" \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cpfCnpj": "12.345.678/0001-99",
    "contratosIds": ["CTR-001", "CTR-002"],
    "numerosParcelasDesejados": [3, 6, 12]
  }' | python3 -m json.tool

# Esperado: HTTP 200 com lista de SimulacaoResponse
# Cada item: { tipoAcordo, numeroParcelas, valorNegociado, valorParcela, percentualDesconto }
```

### 8.3 Criar proposta
```bash
curl -X POST "http://localhost:8082/api/v1/renegociacao/proposta" \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cpfCnpj": "12.345.678/0001-99",
    "contratosIds": ["CTR-001", "CTR-002"],
    "numeroParcelas": 12,
    "tipoAcordo": "JUROS_SIMPLES"
  }' | python3 -m json.tool

# Esperado: HTTP 201 Created
# Header Location: /api/v1/renegociacao/proposta/{uuid}
# Body: { id, cpfCnpj, status: "PENDENTE", valorTotal, valorNegociado, ... }

# Salve o ID retornado para os próximos passos
export PROPOSTA_ID="uuid-retornado-aqui"
```

### 8.4 Consultar proposta (valida cache Redis)
```bash
# Primeira chamada → busca no banco
curl -X GET "http://localhost:8082/api/v1/renegociacao/proposta/$PROPOSTA_ID" \
  -H "Authorization: Bearer SEU_JWT_TOKEN" | python3 -m json.tool

# Segunda chamada → deve vir do cache (mais rápida)
curl -X GET "http://localhost:8082/api/v1/renegociacao/proposta/$PROPOSTA_ID" \
  -H "Authorization: Bearer SEU_JWT_TOKEN" | python3 -m json.tool

# Verificar no Redis que a chave foi criada:
docker exec -it $(docker ps -qf "name=redis") redis-cli
> KEYS proposta::*
> GET proposta::SEU_UUID
> TTL proposta::SEU_UUID  # deve ser ~300 (5 minutos)
```

### 8.5 Efetivar proposta (dispara Saga via Kafka)
```bash
curl -X POST "http://localhost:8082/api/v1/renegociacao/proposta/$PROPOSTA_ID/efetivar" \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "cpfCnpj": "12.345.678/0001-99" }' | python3 -m json.tool

# Esperado: HTTP 200
# Body: { ..., status: "EFETIVADA", efetivadaEm: "..." }

# Verificar o evento no Kafka:
open http://localhost:8090
# Kafka UI → Topics → renegociacao.proposta.efetivada → Messages
# Deve aparecer o evento PropostaEfetivadaEvent com o ID da proposta

# Tentar efetivar novamente (deve falhar com 409):
curl -X POST "http://localhost:8082/api/v1/renegociacao/proposta/$PROPOSTA_ID/efetivar" \
  -H "Authorization: Bearer SEU_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "cpfCnpj": "12.345.678/0001-99" }'
# Esperado: HTTP 409 Conflict
```

### 8.6 Validar Circuit Breaker (simular falha do legado)
```bash
# Parar o legado-acl-service (só existe com o segundo compose file)
docker compose -f docker-compose.yml -f docker-compose-services.yml stop legado-acl-service

# Tentar buscar dívidas (deve acionar Circuit Breaker)
curl -X GET "http://localhost:8082/api/v1/renegociacao/dividas/12.345.678%2F0001-99" \
  -H "Authorization: Bearer SEU_JWT_TOKEN"
# Esperado: HTTP 503 com mensagem de fallback (não um erro genérico)

# Verificar estado do Circuit Breaker:
curl http://localhost:8082/actuator/circuitbreakers | python3 -m json.tool
# Deve mostrar: "state": "OPEN" para o legado-client

# Subir o legado novamente:
docker compose -f docker-compose.yml -f docker-compose-services.yml start legado-acl-service
# Após ~30s o Circuit Breaker volta para HALF_OPEN e depois CLOSED
```

---

## PASSO 9 — Verificar Métricas e Observabilidade

```bash
# Métricas Prometheus:
curl http://localhost:8082/actuator/prometheus | grep propostas
# Deve aparecer:
# propostas_criadas_total X.0
# propostas_efetivadas_total X.0

# Circuit Breaker metrics:
curl http://localhost:8082/actuator/prometheus | grep resilience4j
# resilience4j_circuitbreaker_state{name="legado-client",...} 0.0

# Info do serviço:
curl http://localhost:8082/actuator/info | python3 -m json.tool
```

---

## CHECKLIST FINAL DE VALIDAÇÃO

```
✅ mvn clean verify — raiz: `pom.xml` + `renegociacao-service/pom.xml` (JaCoCo verify); integração Testcontainers ignora-se sem Docker (`AbstractIntegrationTest` / `disabledWithoutDocker`)
✅ docker compose ps — Postgres/Redis/Kafka com healthcheck em `docker-compose.yml`; zookeeper/kafka-ui sem health = status Up
✅ GET /actuator/health — `application.yml` + `KafkaHealthIndicator.java` (kafka); db/redis conforme perfil local
✅ GET /dividas/{cpfCnpj} — `RenegociacaoController.java` (`{*cpfCnpj}` + normalização); teste `RenegociacaoControllerTest`
✅ POST /simular — `SimulacaoService` + controller; três tipos SEM_JUROS / JUROS_SIMPLES / JUROS_COMPOSTOS
✅ POST /proposta — 201 + Location; `RenegociacaoController.criarProposta`
✅ GET /proposta/{id} — cache `PropostaCacheService` / invalidação na efetivação (validar manualmente 1ª vs 2ª chamada + logs)
✅ POST .../efetivar — 200 + `efetivadaEm` em `PropostaResponseMapper`
✅ Segunda efetivação — 409 via `GlobalExceptionHandler` + `IllegalStateException`
✅ Kafka — tópicos em `KafkaTopicNames.java` + consumidores; UI `http://localhost:8090`
✅ Redis — prefixo `proposta::` + TTL 5 min em `PropostaCacheService.java`
✅ Circuit Breaker — `LegadoAclClient` + Resilience4j; validação do PASSO 8 com `docker compose -f docker-compose.yml -f docker-compose-services.yml` (serviço `legado-acl-service`)
✅ JaCoCo — regra ≥80% instruções em `domain/**` + `application/**` (`renegociacao-service/pom.xml`)
✅ Swagger — `application.yml` springdoc `swagger-ui.path: /swagger-ui.html`
```

---
---

# PARTE 2 — PLANO DE APRESENTAÇÃO PARA O ITAÚ

> **Duração estimada:** 30-45 minutos  
> **Formato sugerido:** Compartilhe a tela com o código aberto + browser

---

## 🎯 MENTALIDADE ANTES DE ENTRAR

O avaliador **não quer ver código perfeito**. Ele quer entender:
1. Como você **pensa** diante de um problema complexo
2. Se você **toma decisões conscientes** (não só copia padrões)
3. Se você **comunica bem** escolhas técnicas para times
4. Se você tem **maturidade de sênior** — sabe o que não sabe e sabe o que priorizar

---

## 🗂️ ESTRUTURA DA APRESENTAÇÃO (30-45 min)

---

### BLOCO 1 — Abertura e Framing (3-4 min)

**O que falar:**

> *"Antes de mostrar o código, quero contextualizar a decisão mais importante que tomei: escolhi não resolver tudo de uma vez."*

> *"O sistema atual tem 10-20 anos de COBOL. Uma migração Big Bang seria arriscada demais — qualquer falha derrubaria a cobrança de milhares de clientes PJ. Então, apliquei o Strangler Fig Pattern: os novos microserviços convivem com o legado, que continua funcionando, enquanto as funcionalidades são migradas gradualmente e validadas em produção."*

> *"Focei o desenvolvimento na funcionalidade de Criação de Propostas de Renegociação — exatamente o que a squad de Recuperação PJ faz hoje. Isso não foi por acaso."*

**Por que isso impressiona:** Você demonstra visão de negócio e maturidade. Engenheiro júnior migra tudo. Engenheiro sênior migra com segurança.

---

### BLOCO 2 — Arquitetura (7-8 min)

**Mostre o diagrama de arquitetura e navegue por ele.**

Fale sobre cada peça na seguinte ordem:

**1. Canais digitais → API Gateway**
> *"O cliente no app ou internet banking chega pelo API Gateway. Autenticação JWT já resolvida ali — os serviços não precisam gerenciar sessão. Stateless por design."*

**2. Os 4 microserviços**
> *"Dividi em 4 bounded contexts seguindo DDD: Renegociação, Cobrança, Pagamento e Notificação. Cada um tem seu próprio banco PostgreSQL — Database per Service — então uma falha no Pagamento não derruba a Renegociação."*

**3. Kafka no centro**
> *"A comunicação entre serviços é assíncrona via Kafka para operações de escrita. Isso desacopla temporalmente os serviços: o Renegociação não precisa esperar o Pagamento terminar de gerar o boleto para confirmar o acordo ao cliente."*

**4. Redis para leitura**
> *"Consultas de proposta são cacheadas no Redis. O cliente vai consultar o status do acordo muitas vezes — não faz sentido bater no banco toda vez. TTL de 5 minutos, invalidado na efetivação."*

**5. Anti-Corruption Layer**
> *"O legado-acl-service é o adaptador entre o mundo novo e o mainframe. Ele traduz o modelo COBOL para o modelo de domínio moderno. Se amanhã migramos o mainframe, só muda aqui — os outros serviços não sabem da existência do legado."*

---

### BLOCO 3 — Design Patterns (5-6 min)

**Não liste todos — escolha os 4 mais impactantes e conte a história de cada um:**

**Strategy — Cálculo de Parcelas**
> *"O banco tem múltiplas regras de parcelamento: sem juros, juros simples, juros compostos. Se eu hardcodar o cálculo no serviço, qualquer nova modalidade exige mexer no core. Com Strategy, cada cálculo é uma peça independente. A Factory devolve a estratégia certa pelo tipo de acordo. Adicionar uma nova modalidade é criar uma nova classe e registrá-la — zero impacto nas existentes."*

**CQRS — Separação de Comandos e Consultas**
> *"Simulação é leitura pura e intensiva — o cliente simula 10 vezes antes de fechar. Efetivação é escrita com consistência forte — não pode errar. Se uso a mesma camada para os dois, o modelo fica comprometido. Com CQRS, leitura usa cache Redis, escrita usa transação com PostgreSQL. Cargas diferentes, soluções diferentes."*

**Saga Choreography — Efetivação do Acordo**
> *"Quando o cliente efetiva o acordo, três coisas precisam acontecer: gerar boleto, enviar notificação e registrar o acordo. Se uso uma transação distribuída (2PC), qualquer serviço fora do ar trava tudo. Com Saga via eventos Kafka, cada serviço reage ao evento anterior de forma independente. Se o Pagamento falhar, publica um evento de compensação que cancela o acordo no Renegociação."*

**Circuit Breaker — Resiliência ao Legado**
> *"O mainframe tem janelas de manutenção, lentidão em picos. Se o novo serviço chama o legado diretamente e ele fica lento, toda a thread pool do novo serviço trava em timeouts. Com Circuit Breaker do Resilience4j: após 50% de falhas em 10 requisições, o circuito abre por 30 segundos e o fallback devolve uma resposta degradada. O serviço continua respondendo."*

---

### BLOCO 4 — Demo do Código (10-12 min)

**Abra o editor. Navegue nesta ordem:**

#### 4.1 Mostrar a entidade de domínio `Proposta.java`
> *"Repare que a entidade não é anêmica — ela tem comportamento. O método `efetivar()` encapsula a regra de negócio: só pode efetivar se estiver PENDENTE, senão lança exceção. Isso é DDD: o domínio protege suas próprias invariantes."*

#### 4.2 Mostrar `CalculoJurosSimples.java` e `CalculoSemJuros.java`
> *"São implementações do Strategy. Cada uma encapsula sua fórmula. A interface define o contrato — `calcularValorParcela` e `calcularValorNegociado`. O CommandHandler não sabe qual estratégia está usando."*

#### 4.3 Mostrar `CriarPropostaCommandHandler.java`
> *"Aqui está o fluxo completo de criação: busca dívidas no legado via ACL com Circuit Breaker, valida elegibilidade, obtém a estratégia pela Factory, chama `Proposta.criar()` que aplica o Builder internamente, persiste e publica o evento. Cada etapa é testável isoladamente."*

#### 4.4 Mostrar `PropostaEventProducer.java`
> *"Publicação no Kafka com log estruturado usando MDC — o traceId aparece em todos os logs desse contexto. Em produção, isso significa rastrear uma requisição do API Gateway até o consumidor Kafka em segundos."*

#### 4.5 Mostrar `LegadoAclClient.java`
> *"Aqui está o Circuit Breaker em ação com a annotation `@CircuitBreaker`. O método `fallbackDividas` é chamado automaticamente quando o circuito está aberto. O cliente recebe uma resposta degradada, não um erro 500."*

#### 4.6 Abrir o browser — Swagger UI
> *"Vou mostrar funcionando."*

Execute ao vivo:
1. `GET /dividas/{cpfCnpj}` → mostra dívidas
2. `POST /simular` → mostra as 3 opções de cálculo
3. `POST /proposta` → cria com status PENDENTE
4. `GET /proposta/{id}` → consulta (1ª vez banco)
5. `GET /proposta/{id}` → consulta (2ª vez cache — mostrar log "cache hit")
6. `POST /proposta/{id}/efetivar` → efetiva, mostra EFETIVADA
7. Abrir Kafka UI → mostrar evento no tópico

---

### BLOCO 5 — Testes (4-5 min)

**Rodar ao vivo:**
```bash
mvn test -pl renegociacao-service
```

**Mostre o relatório JaCoCo no browser:**
> *"Cobertura de 80%+ nas camadas de domínio e aplicação. O build falha automaticamente se cair abaixo disso — é uma proteção configurada no JaCoCo. Testcontainers sobe infraestrutura real nos testes de integração — não tem mock de Kafka ou banco, é a coisa real rodando em container."*

**Destaque um teste:**

Abra `CriarPropostaCommandHandlerTest.java`:
> *"Repare no `@DisplayName` em português — o relatório de testes é legível para qualquer pessoa do time, não só engenheiros. E os mocks seguem o padrão Given/When/Then — fácil de entender o que está sendo testado e qual é o comportamento esperado."*

---

### BLOCO 6 — O Que Ficou de Fora e Por Quê (3-4 min)

**Este bloco é diferencial de senioridade. Fale sobre o que você não implementou:**

> *"Há itens que priorizei não implementar neste exercício, mas que seriam necessários em produção:"*

> *"**Autenticação própria** — assumi que o API Gateway já valida JWT. Em produção, configuraria o Spring Security com chave pública do Identity Provider do banco."*

> *"**Outbox Pattern** — para garantir que o evento Kafka e a escrita no banco aconteçam na mesma transação atômica. Sem isso, existe uma janela onde o banco persiste mas o Kafka falha. Em produção, implementaria com Debezium ou uma tabela de outbox."*

> *"**Deploy na AWS** — descrevi o ambiente (EKS, RDS, MSK, ElastiCache) mas não configurei Terraform ou Helm Charts. O foco foi na aplicação."*

> *"**Observabilidade completa** — instrumentei métricas com Micrometer, mas não subi Grafana. Em produção, os dashboards seriam essenciais."*

> *"Essas decisões foram conscientes — dado o tempo disponível, priorizei o que demonstra mais sobre design de software e raciocínio arquitetural."*

---

### BLOCO 7 — Fechamento (2-3 min)

> *"Resumindo: recebi um problema de modernização de mainframe e escolhi uma abordagem que minimiza risco e entrega valor incremental — Strangler Fig com microserviços desacoplados via Kafka, CQRS para separar cargas, Strategy para extensibilidade de regras de negócio e Circuit Breaker para resiliência."*

> *"O serviço que codifiquei — Renegociação — é exatamente o que a squad de Recuperação PJ opera hoje. Não foi coincidência."*

> *"Estou aberto a perguntas sobre qualquer decisão arquitetural ou de implementação."*

---

## ❓ PERGUNTAS ESPERADAS E SUAS RESPOSTAS

### "Por que Kafka e não chamada REST entre os serviços?"

> *"Três razões: desacoplamento temporal (o Renegociação não fica bloqueado esperando o Pagamento), resiliência (se o Pagamento cair, o evento fica no tópico e é consumido quando ele volta) e rastreabilidade (todo evento tem timestamp, offset, chave — auditoria natural). REST síncrono entre serviços cria dependência direta — uma cadeia de 3 serviços lentos vira um timeout em cascata."*

---

### "Como você garantiria consistência dos dados entre os serviços?"

> *"Com a Saga de Coreografia. Cada serviço reage a eventos e publica seus próprios eventos de sucesso ou compensação. Se o Pagamento falhar ao gerar o boleto, ele publica `BoletoFalhouEvent`, o Renegociação consome e cancela o acordo publicando `PropostaCanceladaEvent`. Não tem 2PC — cada serviço tem consistência eventual, mas a Saga garante que o sistema converge para um estado consistente."*

---

### "E se o mesmo evento Kafka for processado duas vezes?"

> *"Idempotência. Cada consumer verifica se o eventId já foi processado antes de agir. Uso um campo `eventoProcessado` na tabela ou uma tabela de eventos processados. Kafka garante at-least-once delivery — a idempotência no consumer garante exactly-once semantics no negócio."*

---

### "Como você migraria o mainframe sem downtime?"

> *"Strangler Fig em fases: Fase 1 — o novo serviço consome o legado via ACL (hoje). Fase 2 — o novo serviço passa a ser a fonte de verdade para novos contratos; legado fica como fallback. Fase 3 — migração de dados históricos em batch para o PostgreSQL. Fase 4 — ACL desligada, legado aposentado para Renegociação. Em cada fase, é possível fazer rollback. Nunca há um 'corte' único."*

---

### "Por que Java 17 e não a versão mais recente?"

> *"Java 17 é LTS (Long Term Support) — garante suporte e patches de segurança por anos. O Spring Boot 3.2 exige Java 17+ e traz virtual threads (Project Loom), que melhoram throughput em aplicações I/O-bound como esta. Estabilidade em produção tem mais valor do que adotar a versão mais recente sem LTS."*

---

### "Como você testaria o Circuit Breaker em produção?"

> *"Chaos Engineering — injeção controlada de falhas. Ferramentas como Chaos Monkey ou AWS Fault Injection Simulator simulam falhas do legado em produção de forma controlada. Os dashboards Prometheus/Grafana mostrariam o estado do Circuit Breaker em tempo real. Antes de produção, os testes de integração com Testcontainers já validam o comportamento do fallback."*

---

### "O que você faria diferente com mais tempo?"

> *"Implementaria o Outbox Pattern para garantir atomicidade entre banco e Kafka. Adicionaria contract testing com Pact — cada consumer define o contrato que espera do producer, e o CI valida isso automaticamente. Configuraria o ambiente completo na AWS com Terraform. E adicionaria rastreamento distribuído com OpenTelemetry + AWS X-Ray para correlacionar traces entre os microserviços."*

---

### "Você conhece IA no desenvolvimento? Stackspot, Copilot?"

> *"Sim — usei ferramentas de IA como assistência no desenvolvimento deste case. Na prática, ferramentas como Stackspot e GitHub Copilot aceleram a criação de boilerplate e documentação, mas a decisão arquitetural e o design de domínio precisam vir do engenheiro. IA não sabe que temos um mainframe legado ou que a squad foca em Recuperação PJ — esse contexto é humano."*

---

## 🧠 DICAS DE POSTURA NA APRESENTAÇÃO

| Situação | Como agir |
|---|---|
| Pergunta que você não sabe responder | *"Não tenho certeza, mas meu raciocínio seria... Você pode me ajudar a pensar nisso?"* |
| Algo no código que não funcionou | *"Identifico o problema — seria [explicação]. A correção seria [solução]."* |
| Avaliador discorda da sua escolha | *"Faz sentido. Na minha análise priorizei X por causa de Y, mas consigo ver como Z resolveria melhor se [condição]. Qual é a abordagem que vocês usam aqui?"* |
| Silêncio após pergunta difícil | Pense em voz alta: *"Deixa eu raciocinar... o problema aqui seria..."* |
| Pergunta sobre times e colaboração | Conecte com o projeto: *"Neste projeto estruturei os serviços de forma que times independentes possam evoluir cada um — essa autonomia é fundamental para squads ágeis."* |

---

## ⏱️ CRONOGRAMA SUGERIDO

| Tempo | Bloco |
|---|---|
| 0-4 min | Abertura: contexto e decisão estratégica |
| 4-12 min | Arquitetura: diagrama + cada peça |
| 12-18 min | Design Patterns: os 4 principais |
| 18-30 min | Demo ao vivo: Swagger + Kafka UI + logs |
| 30-35 min | Testes: rodar ao vivo + JaCoCo |
| 35-38 min | O que ficou de fora e por quê |
| 38-40 min | Fechamento |
| 40-45 min | Perguntas |

---

## 📋 CHECKLIST PRÉ-APRESENTAÇÃO (noite anterior)

```
□ Projeto compila: mvn clean verify ✅
□ docker compose up -d → todos healthy ✅
□ Swagger UI abre em http://localhost:8082/swagger-ui.html ✅
□ Kafka UI abre em http://localhost:8090 ✅
□ Todos os endpoints testados manualmente (checklist do PASSO 8) ✅
□ JaCoCo mostra >= 80% de cobertura ✅
□ GitHub: código commitado e público ✅
□ README.md completo com instruções de execução ✅
□ Relatório técnico finalizado em docs/relatorio-tecnico.md ✅
□ Diagrama de arquitetura salvo como imagem ✅
□ Ensaiou a apresentação em voz alta pelo menos uma vez ✅
□ Preparou as respostas para as perguntas esperadas ✅
□ Deixou o ambiente de demo aberto e pronto ✅
```

---

*Boa apresentação. A solução é sólida — agora é só comunicá-la com a mesma clareza com que foi construída.*
