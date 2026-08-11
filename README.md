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

| Módulo        | Responsabilidade                                                                | Status                                                  |
| -------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------- |
| `shared`     | Configs globais,`Mapper<I,O>`, `PingController`                             | ✅                                                      |
| `identity`   | Cadastro e consulta de`Client`                                                | ✅                                                      |
| `billing`    | Plano, Assinatura e geração de ApiKey (pagamento simulado)                    | ✅                                                      |
| `permission` | 🔑 Núcleo — middleware de validação de permissão (Chain of Responsibility) | ✅ (validação de ApiKey real; ver limitação abaixo) |
| `project`    | Projeto/Cargo/Rota respeitando limite do plano                                  | 🚧 em desenvolvimento                                   |
| `audit`      | Trilha de auditoria das validações, via Observer                              | 🚧 em desenvolvimento                                   |

Os módulos só conversam por use cases ou eventos — nunca pelo repositório de outro
módulo. Essa fronteira é verificada pelo Spring Modulith: `./mvnw test` falha se
alguém importar um pacote interno de outro módulo.

Detalhes de camadas, regras de comunicação entre módulos e ADRs:
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Limitação conhecida

Em `permission`, só o `ApiKeyValidationHandler` aplica uma regra real (valida a
ApiKey contra `billing`). `TokenValidationHandler` e `RoleRouteValidationHandler`
sempre concedem: o primeiro depende de um 2º fator de autenticação, ainda não
previsto; o segundo depende do módulo `project`, que está sendo implementado agora
— quando `Role` e `Route` existirem, a regra real entra nesse handler sem tocar nos
demais.

---

## Padrões de projeto

| Padrão                 | Onde                                                                                   | Status |
| ----------------------- | -------------------------------------------------------------------------------------- | ------ |
| Factory Method          | `ApiKeyFactory` — centraliza a estratégia de geração da ApiKey                   | ✅     |
| Adapter                 | `FakePaymentGatewayAdapter` — adapta o gateway simulado à porta `PaymentGateway` | ✅     |
| Chain of Responsibility | Handlers de validação de permissão, um por preocupação                            | ✅     |
| Builder                 | `ProjectBuilder` — monta `Project` com cargos e rotas passo a passo               | 🚧     |
| Observer                | `AuditLogListener` — reage à validação de permissão sem acoplar os módulos     | 🚧     |

Onde cada padrão vive, por que foi escolhido e como estender:
[`docs/PATTERNS.md`](docs/PATTERNS.md). Mapeamento dos 5 princípios SOLID:
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## Escopo

**Implementado:** cadastro de cliente, assinatura de plano com pagamento simulado e
geração de ApiKey, middleware de validação de permissão.

**Em desenvolvimento:** módulos `project` (Projeto/Cargo/Rota respeitando o limite do
plano) e `audit` (trilha de auditoria de cada validação, com projeto, rota, cargo,
data, resultado e motivo), além de integração com API externa via OpenFeign.

**Trabalho futuro:** gateway de pagamento real, autenticação/JWT com Spring Security,
exportação CSV/JSON, front-end, processamento assíncrono da auditoria.

---

## Evolução

O mesmo código atravessa as disciplinas da Pós-Graduação. Cada uma tem sua pasta em
`docs/`, com o enunciado do professor e o plano daquela matéria — o que permite
distinguir o que já existia do que foi construído em cada momento.

| Disciplina                                                                                                             | Período        | O que acrescentou                                                                                                                                            | Marcos                                           |
| ---------------------------------------------------------------------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------ |
| [Clean Code e Padrões de Projeto](docs/clean_code_e_padroes_de_projeto/PLAN.md)                                        | até 05/07/2026 | Módulos`shared`, `identity`, `billing` e `permission`; Factory Method, Adapter e Chain of Responsibility; fronteiras de módulo com Spring Modulith | —                                               |
| [Desenvolvimento de aplicações Java com Spring Boot](docs/desenvolvimento_de_aplicacoes_java_com_spring_boot/PLAN.md) | até 24/08/2026 | Módulos`project` e `audit`, CRUD REST completo, relacionamentos e herança JPA, leitura de arquivos texto, OpenFeign                                    | tags`etapa-1` … `etapa-4` (em construção) |

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
