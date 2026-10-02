# Permission SaaS

SaaS de gerenciamento de permissões por projeto — um cliente se cadastra, assina um
plano (pagamento simulado) e recebe uma **ApiKey**. Sistemas externos usam essa
ApiKey para validar, em um único endpoint, se uma requisição pode acessar uma rota
com determinado cargo.

**Monolito modular** em Java 21 / Spring Boot 4.1.0, construído como projeto de longo
prazo ao longo da Pós-Graduação: cada disciplina evolui este mesmo código em vez de
começar um projeto do zero. O que cada uma acrescentou está em
[Evolução](#evolução).

**Stack:** Java 21 · Spring Boot 4.1.0 · Spring Data JPA · PostgreSQL 16 · Flyway · Spring Modulith · Docker Compose · Maven

---

## Como rodar

### Pré-requisitos

- Docker + Docker Compose (v2, comando `docker compose`)
- Para rodar fora do Docker: JDK 21. O Maven Wrapper (`./mvnw`) já está em cada projeto
  e baixa a versão certa do Maven sozinho — não precisa ter o Maven instalado.

### Estrutura do repositório

```
permission_saas/
├── permission-service/  aplicação principal (monolito modular) — porta 8080
├── audit-service/       trilha de auditoria extraída como serviço — porta 8081
├── docker-compose.yml   orquestra as aplicações e os bancos
├── docker/              script de inicialização do Postgres da aplicação principal
└── docs/                documentação do projeto e de cada disciplina
```

Cada aplicação é um projeto Maven independente, com seu próprio `pom.xml`, `mvnw` e
`Dockerfile`: os comandos `./mvnw` abaixo rodam **dentro** da pasta do projeto. A
decisão está no ADR-008 de [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Variáveis de ambiente

```bash
cp .env.example .env
```

| Variável                                                 | Usada de fato? | Para quê                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| --------------------------------------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | ✅             | Conexão com o Postgres. Têm default em`application.yml` (`localhost:5432`, `saas`/`saas123`), então nem precisam estar no `.env` para rodar `./mvnw spring-boot:run` com `docker compose up -d postgres`.                                                                                                                                                                                                                                                                                                                                                |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`           | ❌             | `application.yml` referencia `${GOOGLE_CLIENT_ID}` para registrar o client OAuth2 do Google, mas `SecurityConfig` desabilita `oauth2Login` explicitamente (`.oauth2Login(oauth2 -> oauth2.disable())`) e libera todas as rotas (`anyRequest().permitAll()`). Login/JWT ainda não foram implementados (ver [Escopo](#escopo)). A variável só precisa existir com **qualquer valor não vazio** — sem isso o Spring falha ao resolver o placeholder e a aplicação nem sobe. Não há necessidade de criar credenciais reais no Google Cloud Console. |
| `JWT_SECRET`                                            | ❌             | Mesmo motivo acima — referenciada em`application.yml` (`app.jwt.secret`), mas nenhum código gera ou valida JWT ainda.                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `AUDIT_SERVICE_URL`                                     | ✅             | Endereço do `audit-service`, usado pelo cliente OpenFeign da aplicação principal (`audit.service.url` no `application.yml`). Default `http://localhost:8081`, o serviço rodando na máquina. No Docker Compose, o próprio `docker-compose.yml` define `http://audit-service:8081`: o nome do serviço na rede interna, porque dentro de um container `localhost` é o próprio container. |
| `POSTGRES_HOST_PORT`                                    | ✅             | Porta da **sua máquina** em que o Compose publica o banco da aplicação principal. Default `5432`; use outra (ex.: `5434`) se um PostgreSQL instalado na máquina já ocupa a 5432. Só muda o acesso de fora: entre containers o banco continua em `postgres:5432`. |

`docker compose up` lê o `.env` automaticamente e já sobrescreve as credenciais do
Postgres com os valores fixos do `docker-compose.yml` — o `.env` importa mesmo é
para `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`/`JWT_SECRET`, que não têm default.

Se for rodar a aplicação fora do Docker (`./mvnw spring-boot:run`), o `.env` **não**
é carregado automaticamente pelo Spring Boot — exporte as variáveis no shell antes:

```bash
set -a && source .env && set +a
cd permission-service && ./mvnw spring-boot:run
```

### Subir tudo (aplicações + bancos)

```bash
docker compose up -d --build
```

Um comando sobe os quatro containers, na rede que o Compose cria para o projeto:

| Container            | Imagem                               | Porta na máquina | Fala com                        |
| -------------------- | ------------------------------------ | ---------------- | ------------------------------- |
| `permission-service` | `permission-service/Dockerfile`      | 8080             | `postgres:5432`, `audit-service:8081` |
| `audit-service`      | `audit-service/Dockerfile`           | 8081             | `audit-postgres:5432`           |
| `postgres`           | `postgres:16`                        | 5432 (`POSTGRES_HOST_PORT`) | —                    |
| `audit-postgres`     | `postgres:16`                        | 5433             | —                               |

Entre containers o endereço é o **nome do serviço** e a porta de dentro, nunca
`localhost`. As migrations do Flyway de cada aplicação rodam na subida, cada uma no
seu banco. Os dados ficam em volumes (`permission_saas_pgdata`, `audit_pgdata`), e o
arquivo `logs/audit-events.txt` do `audit-service` no volume `audit_logs`: sobrevivem
a `docker compose down` (só `down -v` apaga).

```bash
curl http://localhost:8080/ping                  # pong
curl http://localhost:8081/actuator/health       # {"status":"UP",...}
docker compose ps                                # os quatro como "healthy"
```

### Live reload com `docker compose watch`

Em vez de rebuildar a imagem manualmente a cada mudança, `docker compose watch`
observa o `src/` e o `pom.xml` de cada aplicação, e o `./.env` no caso da principal
(configurado em `docker-compose.yml`), e rebuilda só o container afetado:

```bash
docker compose up -d --build   # sobe a stack uma vez
docker compose watch           # em outro terminal, fica observando e rebuildando
```

### Debug remoto do container

O agente de debug (JDWP) **não** está dentro das imagens: o `Dockerfile` traz só o
necessário para rodar a aplicação. Quem liga o debug é o `docker-compose.yml`, pela
variável `JAVA_TOOL_OPTIONS`, que a JVM lê na partida. A aplicação principal escuta na
porta `5005` e o `audit-service` na `5006`. Na IDE, anexe (attach) um **Remote JVM
Debug** em `localhost:5005` ou `localhost:5006`. O processo sobe com `suspend=n`,
ou seja, não espera o debugger conectar para iniciar.

### Rodar só o banco (desenvolvimento local)

```bash
docker compose up -d postgres
cd permission-service && ./mvnw spring-boot:run
```

O `audit-service` tem banco próprio. Rode-o num terminal **sem** o `.env` da raiz
exportado — as variáveis `SPRING_DATASOURCE_*` de lá apontam para o banco da
aplicação principal e teriam precedência sobre o `application.yml` do serviço:

```bash
docker compose up -d audit-postgres
cd audit-service && ./mvnw spring-boot:run
```

Para testar a comunicação entre os dois, suba os dois serviços ao mesmo tempo, cada um
no seu terminal (ou cada um numa configuração de launch da IDE, a do `audit-service`
sem o `.env`). Ao depurar, um breakpoint parado no `audit-service` estoura o timeout de
2s do cliente Feign; para depurar com calma, suba a aplicação principal com
`--spring.cloud.openfeign.client.config.audit-service.read-timeout=600000`.

**Porta 5432 ocupada** por um PostgreSQL instalado na máquina. Com o Compose, basta
publicar o banco em outra porta. Para não repetir a variável a cada comando, ponha
`POSTGRES_HOST_PORT=5434` no `.env`:

```bash
POSTGRES_HOST_PORT=5434 docker compose up -d --build
```

Rodando a aplicação fora do Docker, suba o banco num container avulso em outra porta,
com o mesmo volume do Compose, e aponte a aplicação para ela:

```bash
docker run -d --rm --name permission-pg-5434 -e POSTGRES_DB=permissions_saas \
  -e POSTGRES_USER=saas -e POSTGRES_PASSWORD=saas123 \
  -v permission_saas_permission_saas_pgdata:/var/lib/postgresql/data -p 5434:5432 postgres:16
set -a && source .env && set +a
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/permissions_saas \
  SPRING_DATASOURCE_USERNAME=saas SPRING_DATASOURCE_PASSWORD=saas123
cd permission-service && ./mvnw spring-boot:run
```

`--rm` remove o container quando ele para (`docker stop permission-pg-5434`); os dados
ficam no volume.

### Build e testes

```bash
cd permission-service
./mvnw clean package -DskipTests   # build
./mvnw test                        # testes
./mvnw test -Dtest=ClassName       # uma classe específica
./mvnw verify                      # inclui os testes de integração (*IT)
```

---

## Fluxo de ponta a ponta

```bash
# 1. Cadastrar um cliente (o "id" da resposta é o clientId usado a seguir)
curl -X POST http://localhost:8080/clients/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Jairo Neto","email":"jairo@example.com","phone":"11999999999","rawPassword":"senha123"}'

# 2. Assinar um plano (planId de um plano já existente no banco) e receber a ApiKey
curl -X POST http://localhost:8080/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"clientId":"<uuid do passo 1>","planId":"<uuid do plano>"}'

# 3. Validar uma permissão com a ApiKey recebida
curl -X POST http://localhost:8080/validate-permission \
  -H "Content-Type: application/json" \
  -d '{"apiKey":"<apiKey do passo 2>","role":"admin","route":"/orders"}'
```

Detalhe de todos os endpoints, request/response e exemplos: [`docs/API.md`](docs/API.md).

---

## Arquitetura

Monolito modular: um único deploy, organizado por módulo de domínio, cada um com
`domain` / `application` / `infrastructure` / `api`.

| Módulo       | Responsabilidade                                                          | Status |
| ------------ | ------------------------------------------------------------------------- | ------ |
| `shared`     | Configuração global, `Mapper<I,O>`, tratamento de exceções, `PingController` | ✅     |
| `identity`   | Cadastro e consulta de `Client`                                           | ✅     |
| `billing`    | Plano, Assinatura e geração de ApiKey (pagamento simulado)                | ✅     |
| `permission` | 🔑 Núcleo — middleware de validação de permissão (Chain of Responsibility) | ✅     |
| `project`    | Projeto/Cargo/Rota e concessões de rota por cargo, respeitando o limite do plano | ✅     |
| `audit`      | Trilha de auditoria das validações, via Observer                          | ✅     |

Os módulos só conversam por use cases ou eventos — nunca pelo repositório de outro
módulo. Essa fronteira é verificada pelo Spring Modulith: `./mvnw test` (em `permission-service/`) falha se
alguém importar um pacote interno de outro módulo.

Detalhes de camadas, regras de comunicação entre módulos e ADRs:
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Módulos e responsabilidades

**`identity` — quem é o cliente.** Cadastra e consulta o `Client`, a empresa que
contrata o SaaS. É a porta de entrada: sem um cliente cadastrado não há assinatura,
e sem assinatura não há ApiKey. Não conhece plano, projeto nem permissão.

**`billing` — o que o cliente comprou.** Cuida de `Plan` e `Subscription` e é o único
lugar que emite e ativa uma `ApiKey`. Concentra também o pagamento, hoje simulado
atrás da porta `PaymentGateway`. Responde a uma pergunta que o resto do sistema faz o
tempo todo: *esta ApiKey existe e está ativa?*

**`project` — o que o cliente configurou.** Guarda o `Project` do cliente, seus
cargos (`Role`), suas rotas (`Route`) e, entre eles, o `RoleRoute` — a concessão que
diz quais rotas cada cargo alcança, com histórico de concessão e revogação. É aqui
que vive a regra de negócio que a validação consulta.

**`permission` — o núcleo.** Expõe o único endpoint que o cliente chama em produção
(`POST /validate-permission`) e decide se uma requisição passa. A decisão é uma
cadeia de responsabilidade: cada handler verifica uma preocupação e delega adiante.
Não tem tabela própria — pergunta aos outros módulos.

**`audit` — o que aconteceu.** Registra cada validação de permissão em uma trilha
append-only. Nasce de um evento publicado pelo `permission` e não devolve nada a
ninguém. Desde 30/09/2026 não guarda nada localmente: envia cada evento e repassa cada
consulta ao `audit-service` (porta 8081, banco próprio) via OpenFeign — se o serviço
cair, a validação de permissão segue funcionando e o evento se perde, com um aviso no
log, e a consulta responde `503`.

**`shared` — o que é de todos.** Configuração de segurança e Swagger, `Mapper<I,O>`,
`DomainException` e o `GlobalExceptionHandler` que centraliza o tratamento de erro.
Não tem regra de negócio.

### Dependências entre os módulos

Medido pelos imports entre pacotes de módulos diferentes:

```
identity   → shared
billing    → identity, shared
project    → shared
permission → billing, project, shared
audit      → permission (apenas o record do evento), shared
```

Duas dependências concretas, ambas no caminho crítico da validação de permissão:

**`permission` → `billing`.** Para aceitar uma requisição é preciso saber se a ApiKey
existe e está ativa. O `permission` não enxerga a tabela do `billing`: declara a porta
`ApiKeyValidator` no próprio domínio, e o adapter `BillingApiKeyValidator` chama o use
case `FindActiveApiKeyByPlainKeyUseCase`. Quem consome é o `ApiKeyValidationHandler`.

**`permission` → `project`.** Para decidir se o cargo alcança a rota é preciso
consultar as concessões do projeto. Mesmo desenho: porta `RouteAccessChecker` no
domínio, adapter `ProjectRouteAccessChecker` chamando `CheckRouteAccessUseCase`, e o
`RoleRouteValidationHandler` como consumidor.

As duas são **síncronas e acontecem dentro da requisição**: se qualquer uma falhar, a
validação não tem resposta para dar. É exatamente o que as separa do `audit`.

Uma terceira dependência, de natureza diferente: **`audit` → `permission`**. O
`ValidatePermissionUseCase` publica um `PermissionValidatedEvent` e segue seu caminho;
o `AuditLogListener` reage a ele. A seta aponta para dentro do `audit` e nada volta.

### Candidato a serviço independente: `audit`

> Análise da etapa 1. A extração foi feita na etapa 2: o `audit-service/` grava e
> consulta a trilha no próprio banco, e o módulo `audit` do monolito virou só um
> cliente dele (ADR-010 em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)).

**Responsabilidade.** Registrar a trilha de auditoria das validações de permissão —
projeto, rota, cargo, data, resultado e motivo — e permitir consultá-la depois.

**Quem depende dele hoje.** Nenhum módulo. É a resposta incômoda e é justamente o
argumento: `grep` por imports de `com.saas.permissions.audit` fora do próprio módulo
não retorna nada. O acoplamento existe na direção oposta e por evento — um único
publisher (`ValidatePermissionUseCase`) e um único consumidor (`AuditLogListener`),
sem valor de retorno. Nenhuma regra de negócio lê da auditoria para decidir algo.

**Por que poderia rodar separado.**

- A trilha é *append-only*: grava-se muito e lê-se raramente, para conferência.
- O acoplamento já é o mais fraco do projeto — um evento assíncrono por natureza,
  hoje entregue em processo.
- Os dados são próprios (`audit_events`) e **sem chave estrangeira** para as tabelas
  dos outros módulos, então separar o banco não quebra integridade referencial.
- Cresce por um motivo diferente do resto: cada validação de permissão gera um
  registro, enquanto o cadastro de projetos é esporádico. Escala independente.

**Por contraste, o que não deve sair.** Extrair `billing` seria o oposto: o
`ApiKeyValidationHandler` depende dele *dentro* da requisição, e a separação
transformaria uma chamada de método em ponto de falha no caminho crítico.

### Consultas Spring Data

Além do CRUD do `JpaRepository`, as consultas que o domínio pede:

- **`project` — consultas derivadas.** Projetos ativos por nome
  (`findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc`), por cliente
  (`findByClientIdAndDeletedAtIsNullOrderByCreatedAtAsc`) e a busca por id que
  ignora os excluídos (`findByIdAndDeletedAtIsNull`).
- **`audit` — JPQL com filtros opcionais** (desde 30/09/2026 no `audit-service`, para
  onde a trilha foi extraída). `search` filtra a trilha por tipo,
  projeto e período; `searchDenied` devolve só as validações negadas, por projeto e
  período, apoiada no índice parcial `idx_audit_events_denied`. Na disciplina anterior
  esses filtros rodavam em memória; como a trilha só cresce, desceram para o banco —
  decisão e detalhes no ADR-009 de [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Limitação conhecida

Em `permission`, o `TokenValidationHandler` ainda é um stub documentado que sempre
concede: depende de um 2º fator de autenticação, fora do escopo até aqui. Os outros
dois handlers aplicam regra real — `ApiKeyValidationHandler` valida a ApiKey contra o
`billing` e `RoleRouteValidationHandler` verifica a concessão de rota no `project`.

A validação da ApiKey tem duas lacunas registradas como trabalho futuro em
[`docs/DOMAIN.md`](docs/DOMAIN.md) → "Limitações conhecidas": a chave não é conferida
contra o dono do projeto, e a busca compara a chave com todas as chaves ativas por
bcrypt, o que fica mais lento a cada cliente.

---

## Serviço independente: `audit-service`

A extração da etapa 2. O porquê da escolha está em
[Candidato a serviço independente: `audit`](#candidato-a-serviço-independente-audit).

|                                         |                                                                                                                                                                                                                                                                                                                       |
| --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Nome**                                | `audit-service` — pasta `audit-service/`, porta 8081, banco próprio `audit_db` (porta 5433)                                                                                                                                                                                                                           |
| **Responsabilidade principal**          | Guardar a trilha de auditoria das validações de permissão e permitir consultá-la por tipo, projeto, período e resultado                                                                                                                                                                                              |
| **O que saiu da aplicação principal**   | A persistência da trilha: entidades JPA com herança `SINGLE_TABLE`, repositórios, consultas JPQL, o arquivo `logs/audit-events.txt` e a tabela `audit_events`, apagada pela migration `V10`. O módulo `audit` do monolito ficou só como cliente do serviço                                                       |
| **Motivo**                              | Nenhum módulo depende da auditoria para decidir algo: o `permission` avisa o que aconteceu e não espera resposta. Os dados não têm chave estrangeira para outras tabelas e crescem a cada validação, num ritmo próprio. E auditoria serve a qualquer sistema, não só a este — ver a [reflexão](#etapa-2--separação-do-audit-service) |

**API REST.** Contrato em DTOs próprios nos dois lados (`RegisterPermissionCheckRequest`,
`AuditEventResponse`); nenhuma entidade JPA atravessa a rede. Swagger em
`http://localhost:8081/swagger-ui/index.html`; detalhes em [`docs/API.md`](docs/API.md).

| Método | Caminho                           | O que faz                                                                     | Respostas      |
| ------ | --------------------------------- | ----------------------------------------------------------------------------- | -------------- |
| `POST` | `/audit-events/permission-checks` | Registra uma validação de permissão                                           | `201`, `400` |
| `GET`  | `/audit-events`                   | Consulta a trilha, com filtros opcionais `type`, `projectId`, `onlyDenied`, `from` e `to` | `200`, `400` |

**Comunicação.** A aplicação principal chama o serviço pelo cliente OpenFeign
`AuditClient`, sempre atrás da porta `AuditTrail` — nenhum controller conhece o Feign.
O endereço vem de `audit.service.url` no `application.yml`
(`${AUDIT_SERVICE_URL:http://localhost:8081}`), nunca do código Java.

```
Cliente HTTP
↓
permission-service (8080)
├── PermissionController → ValidatePermissionUseCase → publica PermissionValidatedEvent
│                                                          ↓
├── AuditLogListener (Observer) ───────────→ porta AuditTrail → AuditClient (@FeignClient)
└── AuditEventController → SearchAuditEventsUseCase ↗                  ↓ HTTP
                                                     audit-service (8081)
                                                     ├── AuditEventController
                                                     ├── RegisterPermissionCheckUseCase / SearchAuditEventsUseCase
                                                     └── AuditEventRepository → PostgreSQL audit_db
```

**Falha de comunicação.** O cliente desiste em 1s para conectar e 2s para ler. Cada
operação trata a falha de um jeito:

- **Gravação** — a validação de permissão responde normalmente; o `AuditLogListener`
  captura a falha e o evento se perde, com `WARN ... Audit event lost: ...` no log.
- **Consulta** — `GET /audit-events` responde `503` com
  `{"status":503,"error":"Service Unavailable","message":"Audit service is unavailable",...}`.
  O detalhe do Feign só vai para o log.

**Como testar** (coleção em [`docs/postman/`](docs/postman/)):

| Demonstração                         | Pasta do Postman                                     |
| ------------------------------------ | ---------------------------------------------------- |
| API do serviço isolada               | `audit-service (8081)`                               |
| Operação pela aplicação principal    | `Fluxo completo` (requisições 9 a 14) e `Audit`      |
| Serviço indisponível                 | `audit-service fora do ar` — o roteiro está na descrição da pasta |

---

## Reflexões arquiteturais

### Etapa 2 — separação do `audit-service`

**Qual funcionalidade foi separada da aplicação principal?** A trilha de auditoria:
registrar cada validação de permissão (projeto, rota, cargo, resultado e motivo) e
consultar esse histórico depois.

**Por que ela foi escolhida?** Era o módulo mais desacoplado do monolito: ninguém
depende dele, ele só recebe um evento e não devolve nada. Separar primeiro a parte mais
solta segue o *Strangler Fig*: tirar uma capacidade de cada vez do monolito, sem
reescrevê-lo inteiro. O `billing`, pelo contrário, está dentro da requisição de validação
e, se fosse separado, viraria um ponto de falha no caminho crítico.

**O que ficou mais complexo depois da separação?**

- A aplicação principal precisou ser reestruturada para atender o novo serviço. O código
  saiu da raiz para `permission-service/`. O módulo `audit` perdeu a persistência e virou
  cliente, com um `@FeignClient`, um adapter e DTOs que espelham o contrato do serviço.
  Esse contrato agora existe nos dois lados e precisa mudar junto.
- Passou a haver outro sistema para cuidar: dois projetos, dois bancos e duas aplicações
  para subir, depurar e manter.
- A aplicação principal precisa decidir o que fazer com o retorno ou a falha do serviço.
  Uma chamada de método virou chamada de rede, que pode demorar, falhar ou nem responder.
  Cada operação ganhou uma decisão própria: a gravação engole a falha, a consulta devolve
  `503`.
- Foi preciso configurar a aplicação principal para a indisponibilidade: URL externa,
  timeouts e tratamento de erro que não vaza detalhe interno.

**O que aconteceria com a funcionalidade principal caso o novo serviço ficasse
indisponível?** A validação de permissão, que é o produto, continua funcionando. Ela
responde normalmente, no máximo uns 3 segundos mais lenta por causa dos timeouts, mas o
evento daquela validação se perde. A consulta da trilha fica fora do ar e responde `503`.
A perda de eventos é a limitação aceita nesta etapa; a fila do RabbitMQ, na etapa 4,
existe para resolvê-la.

**A funcionalidade realmente precisa permanecer como um serviço independente?** Sim. Olhando
só para o Permission SaaS, o módulo `audit` dentro do monolito dava conta: funcionou assim
na disciplina anterior, e a separação trouxe os custos listados acima sem ganho funcional
para este sistema sozinho. O que justifica o serviço é ele servir de base para outros
projetos. Auditoria é útil para qualquer sistema: mostra o que acontece de certo e de errado
nas aplicações e apoia a conformidade com a LGPD, que pede o registro das operações de
tratamento de dados pessoais. Com esse horizonte, desacoplar a auditoria do projeto
principal foi uma escolha válida. A direção está registrada em
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) → "Direção futura".

---

## Padrões de projeto

| Padrão                 | Onde                                                                                   | Status |
| ----------------------- | -------------------------------------------------------------------------------------- | ------ |
| Factory Method          | `ApiKeyFactory` — centraliza a estratégia de geração da ApiKey                   | ✅     |
| Adapter                 | `FakePaymentGatewayAdapter` — adapta o gateway simulado à porta `PaymentGateway` | ✅     |
| Chain of Responsibility | Handlers de validação de permissão, um por preocupação                            | ✅     |
| Observer                | `AuditLogListener` — reage à validação de permissão sem acoplar os módulos       | ✅     |
| Builder                 | `ProjectBuilder` — descartado: `@Builder` do Lombok mais `addRole`/`addRoute` já cobrem o caso | ⛔     |

Onde cada padrão vive, por que foi escolhido e como estender:
[`docs/PATTERNS.md`](docs/PATTERNS.md). Mapeamento dos 5 princípios SOLID:
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Escopo

**Implementado:** cadastro de cliente, assinatura de plano com pagamento simulado e
geração de ApiKey, middleware de validação de permissão aplicando a regra real
(o cargo precisa de uma concessão ativa sobre a rota), CRUD de projeto/cargo/rota com
histórico de concessão e revogação, e trilha de auditoria em banco e arquivo texto —
desde a etapa 2 no [`audit-service`](#serviço-independente-audit-service), chamado por
OpenFeign.

**Em desenvolvimento:** configuração centralizada, mensageria e processamento em
lote — o restante do escopo da disciplina de microsserviços, descrito em
[Evolução](#evolução).

**Trabalho futuro:** gateway de pagamento real, autenticação/JWT com Spring Security,
exportação CSV/JSON, front-end, `userId` no evento de auditoria, uma aplicação
cliente de demonstração consumindo o `POST /validate-permission` e a renomeação desse
endpoint para `POST /permissions/validate`, que alinharia o recurso ao restante da API
(ver a nota de contrato em [`docs/API.md`](docs/API.md)).

---

## Evolução

O mesmo código atravessa as disciplinas da Pós-Graduação. Cada uma tem sua pasta em
`docs/`, com o enunciado do professor e o plano daquela matéria — o que permite
distinguir o que já existia do que foi construído em cada momento.

| Disciplina                                                                                                             | Período        | O que acrescentou                                                                                                                                            | Marcos                                           |
| ---------------------------------------------------------------------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------ |
| [Clean Code e Padrões de Projeto](docs/clean_code_e_padroes_de_projeto/PLAN.md)                                        | até 05/07/2026 | Módulos`shared`, `identity`, `billing` e `permission`; Factory Method, Adapter e Chain of Responsibility; fronteiras de módulo com Spring Modulith | —                                               |
| [Desenvolvimento de aplicações Java com Spring Boot](docs/desenvolvimento_de_aplicacoes_java_com_spring_boot/PLAN.md) | até 31/08/2026 | Módulos `project` e `audit`, CRUD REST completo, relacionamentos e herança JPA, leitura de arquivos texto | tags `etapa-1` … `etapa-4` |
| [Arquiteturas avançadas de software com microsserviços e Spring Framework](docs/arquiteturas_avancadas_de_software_com_microsservicos_e_spring_framework/PLAN.md) | até 05/10/2026 | `audit` extraído como serviço independente, OpenFeign, Config Server, banco por serviço, RabbitMQ e Spring Batch | tags `arq-etapa-1` … `arq-etapa-4` (em construção) |

As tags desta disciplina usam o prefixo `arq-` porque `etapa-1` … `etapa-4` já
apontam para a evidência da disciplina anterior e não podem ser movidas.

O relatório escrito da primeira disciplina foi entregue como PDF no Moodle e não está
versionado aqui.

---

## Documentação

A raiz de `docs/` guarda a documentação **do projeto como um todo** — cumulativa,
descrevendo o sistema como ele está hoje:

| Arquivo                                                   | Conteúdo                                                                              |
| --------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| [`docs/API.md`](docs/API.md)                             | Todos os endpoints REST implementados, com request/response e exemplos de curl         |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)           | Módulos, camadas, regras de comunicação, ADRs                                       |
| [`docs/DOMAIN.md`](docs/DOMAIN.md)                       | Glossário de entidades, value objects e invariantes de negócio                       |
| [`docs/PATTERNS.md`](docs/PATTERNS.md)                   | Cada padrão GoF: onde vive, por quê, como estender                                   |
| [`docs/TEST-ARCHITECTURE.md`](docs/TEST-ARCHITECTURE.md) | Convenções de teste (unit/slice/integration), exemplos e pirâmide de testes adotada |
| [`docs/DER.pdf`](docs/DER.pdf)                           | Diagrama entidade-relacionamento                                                       |

O que é específico de uma disciplina — enunciado e planejamento — fica na pasta dela,
listada em [Evolução](#evolução).

---

## Autor

Jairo Williams Guedes Lopes Neto
