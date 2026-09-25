# Permission SaaS

SaaS de gerenciamento de permissões por projeto — um cliente se cadastra, assina um
plano (pagamento simulado) e recebe uma **ApiKey**. Sistemas externos usam essa
ApiKey para validar, em um único endpoint, se uma requisição pode acessar uma rota
com determinado cargo.

**Monolito modular** em Java 21 / Spring Boot 3, construído como projeto de longo
prazo ao longo da Pós-Graduação: cada disciplina evolui este mesmo código em vez de
começar um projeto do zero. O que cada uma acrescentou está em
[Evolução](#evolução).

**Stack:** Java 21 · Spring Boot 3 · Spring Data JPA · PostgreSQL 16 · Flyway · Spring Modulith · Docker Compose · Maven

---

## Como rodar

### Pré-requisitos

- Docker + Docker Compose (v2, comando `docker compose`)
- Para rodar fora do Docker: JDK 21. O Maven Wrapper (`./mvnw`) já está no repositório
  e baixa a versão certa do Maven sozinho — não precisa ter o Maven instalado.

### Variáveis de ambiente

```bash
cp .env.example .env
```

| Variável                                                 | Usada de fato? | Para quê                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| --------------------------------------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | ✅             | Conexão com o Postgres. Têm default em`application.yml` (`localhost:5432`, `saas`/`saas123`), então nem precisam estar no `.env` para rodar `./mvnw spring-boot:run` com `docker compose up -d postgres`.                                                                                                                                                                                                                                                                                                                                                |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`           | ❌             | `application.yml` referencia `${GOOGLE_CLIENT_ID}` para registrar o client OAuth2 do Google, mas `SecurityConfig` desabilita `oauth2Login` explicitamente (`.oauth2Login(oauth2 -> oauth2.disable())`) e libera todas as rotas (`anyRequest().permitAll()`). Login/JWT ainda não foram implementados (ver [Escopo](#escopo)). A variável só precisa existir com **qualquer valor não vazio** — sem isso o Spring falha ao resolver o placeholder e a aplicação nem sobe. Não há necessidade de criar credenciais reais no Google Cloud Console. |
| `JWT_SECRET`                                            | ❌             | Mesmo motivo acima — referenciada em`application.yml` (`app.jwt.secret`), mas nenhum código gera ou valida JWT ainda.                                                                                                                                                                                                                                                                                                                                                                                                                                               |

`docker compose up` lê o `.env` automaticamente e já sobrescreve as credenciais do
Postgres com os valores fixos do `docker-compose.yml` — o `.env` importa mesmo é
para `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`/`JWT_SECRET`, que não têm default.

Se for rodar a aplicação fora do Docker (`./mvnw spring-boot:run`), o `.env` **não**
é carregado automaticamente pelo Spring Boot — exporte as variáveis no shell antes:

```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

### Subir tudo (app + Postgres)

```bash
docker compose up -d --build
```

App em `http://localhost:8080`, Postgres em `localhost:5432`. As migrations do
Flyway rodam automaticamente na subida.

```bash
curl http://localhost:8080/ping
# pong
```

### Live reload com `docker compose watch`

Em vez de rebuildar a imagem manualmente a cada mudança, `docker compose watch`
observa `./src`, `./pom.xml` e `./.env` (configurado em `docker-compose.yml`) e
rebuilda o container automaticamente quando algum desses arquivos muda:

```bash
docker compose up -d --build   # sobe a stack uma vez
docker compose watch           # em outro terminal, fica observando e rebuildando
```

### Debug remoto do container

A imagem já sobe com o agente JDWP habilitado (`Dockerfile`) e a porta `5005`
exposta em `docker-compose.yml` — não precisa mudar nada para debugar. Basta
configurar a IDE para anexar (attach) um **Remote JVM Debug** em `localhost:5005`
e colocar os breakpoints normalmente; o processo já sobe com
`suspend=n`, ou seja, a aplicação não espera o debugger conectar para iniciar.

### Rodar só o banco (desenvolvimento local)

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

### Build e testes

```bash
./mvnw clean package -DskipTests   # build
./mvnw test                        # testes
./mvnw test -Dtest=ClassName       # uma classe específica
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
módulo. Essa fronteira é verificada pelo Spring Modulith: `./mvnw test` falha se
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
append-only, no banco e em arquivo texto. Nasce de um evento publicado pelo
`permission` e não devolve nada a ninguém.

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

### Limitação conhecida

Em `permission`, o `TokenValidationHandler` ainda é um stub documentado que sempre
concede: depende de um 2º fator de autenticação, fora do escopo até aqui. Os outros
dois handlers aplicam regra real — `ApiKeyValidationHandler` valida a ApiKey contra o
`billing` e `RoleRouteValidationHandler` verifica a concessão de rota no `project`.

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
histórico de concessão e revogação, e trilha de auditoria em banco e arquivo texto.

**Em desenvolvimento:** extração do `audit` para um serviço Spring Boot independente,
comunicação por OpenFeign, configuração centralizada, mensageria e processamento em
lote — o escopo da disciplina de microsserviços, descrito em
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
