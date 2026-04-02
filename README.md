# Itau Recuperacao PJ — Modernizacao do Sistema de Renegociacao

## Sobre o Projeto

Sistema de modernizacao da plataforma de recuperacao de credito PJ do Itau, migrando de uma arquitetura monolitica mainframe para uma arquitetura de microsservicos baseada em eventos. O projeto aplica o padrao **Strangler Fig** para migrar gradualmente as funcionalidades do legado, garantindo coexistencia e rollback seguro.

O sistema permite que clientes PJ consultem dividas, simulem propostas de renegociacao, efetivem acordos e acompanhem pagamentos de forma digital, com integracao transparente ao mainframe existente via camada Anti-Corruption Layer (ACL).

## Arquitetura

```
┌─────────────────┐
│ Canais Digitais  │
│ (App/Web/API)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   API Gateway    │
└────────┬────────┘
         │
         ▼
┌────────────────────────────────────────────────────────────────┐
│                      Microsservicos                            │
│                                                                │
│  ┌──────────────────┐  ┌──────────────────┐                   │
│  │  renegociacao     │  │  cobranca        │                   │
│  │  service (8082)   │  │  service (8081)  │                   │
│  └────────┬─────────┘  └──────────────────┘                   │
│           │                                                    │
│  ┌────────┴─────────┐  ┌──────────────────┐                   │
│  │  pagamento        │  │  notificacao     │                   │
│  │  service (8083)   │  │  service (8084)  │                   │
│  └──────────────────┘  └──────────────────┘                   │
│                                                                │
└──────────┬─────────────────────┬──────────────────────────────┘
           │                     │
           ▼                     ▼
┌──────────────────┐    ┌──────────────────┐
│   Apache Kafka    │    │  legado-acl      │
│   (Event Bus)     │    │  service (8085)  │
└──────────────────┘    └────────┬─────────┘
                                 │
                                 ▼
                        ┌──────────────────┐
                        │   Mainframe       │
                        │   (Sistema Legado)│
                        └──────────────────┘

Armazenamento:
  renegociacao-service ──► PostgreSQL + Redis (cache)
  cobranca-service     ──► PostgreSQL
  pagamento-service    ──► PostgreSQL
```

## Pre-requisitos

- **Java 17+** (recomendado: Eclipse Temurin)
- **Maven 3.9+**
- **Docker** e **Docker Compose**

## Repositório no GitHub

Código-fonte e CI estão em: [https://github.com/reenanjoordan/itau-recuperacao-pj](https://github.com/reenanjoordan/itau-recuperacao-pj)

Clone e trabalhe **sempre** na pasta do projeto (`itau-recuperacao-pj`), não no diretório do usuário onde possa existir outro `.git` sem relação com este repositório.

```bash
git clone https://github.com/reenanjoordan/itau-recuperacao-pj.git
cd itau-recuperacao-pj
```

Branch principal: `main`.

## Documentação adicional

| Documento | Descrição |
|-----------|-----------|
| [docs/relatorio-tecnico.md](docs/relatorio-tecnico.md) | Relatório técnico (arquitetura, padrões, segurança) |
| [docs/guia-validacao-e-apresentacao-itau.md](docs/guia-validacao-e-apresentacao-itau.md) | Checklist de validação local e roteiro de apresentação |

## Como Executar

### 1. Subir a infraestrutura (Kafka, PostgreSQL, Redis)

```bash
docker compose up -d
```

### 2. Compilar e executar os testes

```bash
mvn clean verify
```

### 3. Executar o servico de renegociacao localmente

```bash
cd renegociacao-service
mvn spring-boot:run
```

### 4. Subir todos os servicos via Docker

```bash
# Primeiro, compilar os JARs de todos os modulos
mvn clean package -DskipTests

# Infraestrutura + microsserviços na mesma stack Compose (rede compartilhada)
docker compose -f docker-compose.yml -f docker-compose-services.yml up -d --build
```

## URLs Uteis

| Recurso | URL |
|---------|-----|
| Swagger UI (Renegociacao) | http://localhost:8082/swagger-ui.html |
| API Docs (OpenAPI) | http://localhost:8082/v3/api-docs |
| Kafka UI | http://localhost:8090 |
| Actuator Health | http://localhost:8082/actuator/health |
| Actuator Metrics | http://localhost:8082/actuator/metrics |

## Endpoints Principais

### Renegociacao Service (porta 8082)

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `GET` | `/api/v1/renegociacao/dividas/{cpfCnpj}` | Consultar dividas de um cliente PJ |
| `POST` | `/api/v1/renegociacao/simular` | Simular proposta de renegociacao |
| `POST` | `/api/v1/renegociacao/proposta` | Criar proposta de renegociacao |
| `POST` | `/api/v1/renegociacao/proposta/{id}/efetivar` | Efetivar proposta de renegociacao |
| `GET` | `/api/v1/renegociacao/proposta/{id}` | Consultar proposta por ID |
| `DELETE` | `/api/v1/renegociacao/proposta/{id}` | Cancelar proposta |

## Cobertura de Testes

O projeto utiliza **JaCoCo** para analise de cobertura de codigo com um minimo de **80%** de cobertura de instrucoes configurado no `renegociacao-service`.

Para gerar o relatorio de cobertura:

```bash
cd renegociacao-service
mvn clean verify
```

O relatorio HTML sera gerado em `renegociacao-service/target/site/jacoco/index.html`.

## Design Patterns Utilizados

| Padrao | Aplicacao |
|--------|-----------|
| **Strangler Fig** | Migracao gradual do monolito mainframe para microsservicos |
| **Anti-Corruption Layer (ACL)** | `legado-acl-service` traduz entre dominio moderno e legado |
| **CQRS** | Separacao de comandos (escrita) e queries (leitura) no servico de renegociacao |
| **Event Sourcing** | Eventos de dominio publicados via Kafka para rastreabilidade |
| **Saga** | Orquestracao de transacoes distribuidas entre servicos |
| **Circuit Breaker** | Resilience4j protege chamadas ao legado contra falhas em cascata |
| **Strategy** | Diferentes estrategias de calculo de desconto e parcelamento |
| **Factory** | Criacao de propostas e acordos com regras de negocio encapsuladas |
| **Builder** | Construcao de objetos complexos (propostas, simulacoes) |
| **Repository** | Abstracao de acesso a dados com Spring Data JPA |
| **Database per Service** | Cada microsservico possui seu proprio banco de dados |

## Estrutura do Projeto

```
itau-recuperacao-pj/
├── pom.xml                          # POM pai (multi-modulo)
├── docker-compose.yml               # Infraestrutura (Kafka, PostgreSQL, Redis)
├── docker-compose-services.yml      # Microsservicos
├── .github/workflows/ci.yml         # Pipeline CI/CD
├── docs/                            # Relatorio tecnico e guia de validacao
├── renegociacao-service/            # Servico principal de renegociacao
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── cobranca-service/                # Servico de cobranca
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── pagamento-service/               # Servico de pagamento
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── notificacao-service/             # Servico de notificacao
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
└── legado-acl-service/              # Anti-Corruption Layer
    ├── pom.xml
    ├── Dockerfile
    └── src/
```

## Tecnologias

- **Java 17** com **Spring Boot 3.2.3**
- **Spring Data JPA** + **PostgreSQL 15**
- **Spring Data Redis** para cache
- **Apache Kafka** para comunicacao assincrona entre servicos
- **Resilience4j** para circuit breaker e retry
- **Flyway** para versionamento de schema do banco
- **MapStruct** para mapeamento entre DTOs e entidades
- **Lombok** para reducao de boilerplate
- **SpringDoc OpenAPI** para documentacao automatica da API
- **JaCoCo** para cobertura de testes
- **Testcontainers** para testes de integracao
- **Docker** e **Docker Compose** para containerizacao

## Autor

Equipe de Modernizacao — Itau Recuperacao PJ
