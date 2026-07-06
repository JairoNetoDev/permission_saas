# Permission SaaS

SaaS de gerenciamento de permissões por projeto — um cliente se cadastra, assina um
plano (pagamento simulado) e recebe uma **ApiKey**. Sistemas externos usam essa
ApiKey para validar, em um único endpoint, se uma requisição pode acessar uma rota
com determinado cargo.

Projeto acadêmico da disciplina **Clean Code e Padrões de Projeto**, implementado
como um **monolito modular** em Java 21 / Spring Boot.

**Stack:** Java 21 · Spring Boot 3 · Spring Data JPA · PostgreSQL 16 · Flyway · Docker Compose · Maven

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

| Variável | Usada de fato? | Para quê |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | ✅ | Conexão com o Postgres. Têm default em `application.yml` (`localhost:5432`, `saas`/`saas123`), então nem precisam estar no `.env` para rodar `./mvnw spring-boot:run` com `docker compose up -d postgres`. |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | ❌ | `application.yml` referencia `${GOOGLE_CLIENT_ID}` para registrar o client OAuth2 do Google, mas `SecurityConfig` desabilita `oauth2Login` explicitamente (`.oauth2Login(oauth2 -> oauth2.disable())`) e libera todas as rotas (`anyRequest().permitAll()`). Login/JWT estão fora de escopo desta entrega (ver [`docs/PLAN.md`](docs/PLAN.md)). A variável só precisa existir com **qualquer valor não vazio** — sem isso o Spring falha ao resolver o placeholder e a aplicação nem sobe. Não há necessidade de criar credenciais reais no Google Cloud Console. |
| `JWT_SECRET` | ❌ | Mesmo motivo acima — referenciada em `application.yml` (`app.jwt.secret`), mas nenhum código gera ou valida JWT ainda. |

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

| Módulo | Responsabilidade | Status |
|---|---|---|
| `shared` | Configs globais, `Mapper<I,O>`, `PingController` | ✅ |
| `identity` | Cadastro e consulta de `Client` | ✅ |
| `billing` | Plano, Assinatura e geração de ApiKey (pagamento simulado) | ✅ |
| `permission` | 🔑 Núcleo — middleware de validação de permissão (Chain of Responsibility) | ✅ (validação de ApiKey real; ver limitação abaixo) |
| `project` | Projeto/Cargo/Rota respeitando limite do plano | ❌ fora de escopo |
| `audit` | Log de auditoria via Observer | ❌ fora de escopo |

Detalhes de camadas, regras de comunicação entre módulos e ADRs:
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

### Limitação conhecida

Em `permission`, só o `ApiKeyValidationHandler` aplica uma regra real (valida a
ApiKey contra `billing`). `TokenValidationHandler` e `RoleRouteValidationHandler`
sempre concedem nesta entrega, porque dependem do módulo `project` (Role/Rota) e de
um 2º fator de autenticação, nenhum dos dois implementado — decisão de escopo
documentada em [`docs/PLAN.md`](docs/PLAN.md).

---

## Padrões de projeto e SOLID

1 criacional + 1 estrutural + 1 comportamental implementados: **Factory Method**
(`ApiKeyFactory`), **Adapter** (`FakePaymentGatewayAdapter`) e **Chain of
Responsibility** (validação de permissão). Builder (`project`) e Observer (`audit`)
estão desenhados mas não implementados — trabalho futuro.

Onde cada padrão vive, por que foi escolhido e como estender:
[`docs/PATTERNS.md`](docs/PATTERNS.md). Mapeamento dos 5 princípios SOLID:
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) e [`docs/RELATORIO.md`](docs/RELATORIO.md).

---

## Documentação

| Arquivo | Conteúdo |
|---|---|
| [`docs/API.md`](docs/API.md) | Todos os endpoints REST implementados, com request/response e exemplos de curl |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Módulos, camadas, regras de comunicação, ADRs |
| [`docs/DOMAIN.md`](docs/DOMAIN.md) | Glossário de entidades, value objects e invariantes de negócio |
| [`docs/PATTERNS.md`](docs/PATTERNS.md) | Cada padrão GoF: onde vive, por quê, como estender |
| [`docs/PLAN.md`](docs/PLAN.md) | Planejamento dia a dia, decisões de escopo, checklist da rubrica |
| [`docs/RELATORIO.md`](docs/RELATORIO.md) | Relatório da disciplina (Clean Code, SOLID, GoF) |
| [`docs/TEST-ARCHITECTURE.md`](docs/TEST-ARCHITECTURE.md) | Convenções de teste (unit/slice/integration), exemplos e pirâmide de testes adotada |

---

## Escopo

**Implementado nesta entrega:** cadastro de cliente, assinatura de plano com
pagamento simulado e geração de ApiKey, middleware de validação de permissão.

**Fora de escopo (trabalho futuro):** criação de Projeto/Cargo/Rota respeitando
limite do plano, log de auditoria, gateway de pagamento real, autenticação/JWT
completos, exportação CSV/JSON, front-end. Detalhes e justificativa em
[`docs/PLAN.md`](docs/PLAN.md).

---

## Autor

Jairo Williams Guedes Lopes Neto — Clean Code e Padrões de Projeto
