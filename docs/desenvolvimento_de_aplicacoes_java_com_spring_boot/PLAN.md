# 📋 Planejamento — Desenvolvimento de aplicações Java com Spring Boot

**Aluno:** Jairo Williams Guedes Lopes Neto
**Disciplina:** Desenvolvimento de aplicações Java com Spring Boot
**Prazo:** 31/08/2026 23:59 (entrega única no Moodle — prazo confirmado com o professor em 26/08)
**Disponibilidade:** mínimo 2h/dia; 4h nos dias 29 e 30/08 (fim de semana)
**Base:** projeto Permission SaaS, aprovado pelo professor para continuidade

> ⚠️ Este arquivo é o plano **desta** disciplina. Cada matéria tem sua própria pasta em
> `docs/`, com o enunciado do professor e o plano correspondente. O plano da disciplina
> anterior está em `docs/clean_code_e_padroes_de_projeto/PLAN.md` e **não deve ser
> alterado** — é evidência do escopo já entregue em 05/07/2026.

---

## Contexto

O professor revisou o repositório e aprovou reusar o Permission SaaS em vez de começar um
projeto novo: *"mais interessante evoluir um projeto que já possui uma base consistente"*.
A única exigência é que a entrega desta disciplina apresente **evolução real e documentada,
claramente separada do que já estava pronto**.

A entrega é única, mas o desenvolvimento é dividido em quatro etapas marcadas por tags git
(`etapa-1` … `etapa-4`), usadas pelo professor como evidência de cada competência.

### Lacunas medidas contra a rubrica

| Item da rubrica | Estado em 10/08/2026 |
|---|---|
| 2 — 4+ classes com `@OneToMany` e `extends` | ❌ nenhum relacionamento JPA (entidades se ligam por UUID), nenhuma herança |
| 4, 5 — classes loader lendo arquivos texto | ❌ inexistente |
| 7, 8 — `Map` simulando banco + camada de serviço gerindo o Map | ❌ inexistente |
| 9, 10 — endpoints REST por contexto, testados e documentados | ⚠️ parcial (sem PUT/DELETE, sem coleção Postman) |
| 11, 12 — front-end consumindo a API | ❌ fora do escopo escolhido (ver "Não incluído") |
| 13, 14, 15 — entidades JPA, repositories, serviços injetando repositories | ✅ já atendido por `identity` e `billing` |
| 16 — OpenFeign | ❌ inexistente |
| Tags `etapa-1` … `etapa-4` | ❌ o repositório não tem nenhuma tag |

**Objetivo desta disciplina:** implementar os módulos `project` (Project/Role/Route) e `audit`
(trilha de auditoria das validações de permissão) numa sequência que produz naturalmente as
quatro tags, fecha as lacunas acima e completa o `RoleRouteValidationHandler` — a primeira
sugestão do professor.

---

## Decisões

1. **Política de IA (🟡):** a disciplina reserva ao aluno a modelagem, as camadas da aplicação,
   as APIs e a persistência. A IA atua em plano, explicação de conceitos, configuração de
   frameworks, depuração, testes, documentação e revisão de código.
2. **Escopo do domínio novo:** `project` + `audit` juntos. O `audit` sozinho registraria
   projeto/rota/cargo vazios, e é o `project` que fornece o relacionamento 1-N exigido.
3. **Arquitetura:** mantém o estilo hexagonal do resto do projeto
   (`domain` model → porta → adapter → `JpaEntity`).
4. **Extras inclusos:** loaders de arquivo texto, coleção Postman, OpenFeign.

### Por que o hexagonal favorece as tags

A porta `ProjectRepository`, declarada no `domain`, permite trocar a implementação sem tocar em
nada acima dela:

- **etapa-2:** `InMemoryProjectRepository` — um `Map<UUID, Project>` encapsulado no adapter,
  consumido pelos use cases. Forma: `Runner → UseCase → Map`.
- **etapa-4:** `ProjectRepositoryAdapter` (JPA) substitui o in-memory. Forma:
  `Controller → UseCase → Repository → Banco`.

É exatamente a evolução que as Etapas 2 e 4 pedem, com DIP real. Essa correspondência precisa
estar registrada em `docs/ARCHITECTURE.md` para o avaliador localizar o `Map` do item 7.

---

## Modelo de domínio novo

```
Project  1 ──── N  Role        (um projeto tem vários cargos)
Project  1 ──── N  Route       (um projeto tem várias rotas)
Project  1 ──── N  AuditEvent

AuditEvent (abstrato)
   ├── PermissionCheckEvent    (projectId, route, role, granted, reason, ipAddress, country)
   └── ProjectLifecycleEvent   (projectId, action: CREATED | UPDATED | DELETED)
```

A herança é coerente com o domínio: a trilha de auditoria registra tipos heterogêneos de evento,
cada um com campos próprios — não é herança criada só para atender ao requisito. Mapeamento JPA:
`SINGLE_TABLE` com `@DiscriminatorColumn`.

Tipos de atributo exigidos pela Etapa 1, distribuídos pelo modelo: texto (`name`, `path`,
`reason`), inteiro (`maxRoles`), real (`BigDecimal`), booleano (`active`, `granted`) e data
(`OffsetDateTime createdAt`, `occurredAt`). Todas as classes com `toString()`; o `toString()` de
`Project` deve listar roles e routes (rubrica item 6).

### Estrutura de pacotes

```
project/
  domain/         Project, Role, Route, ProjectBuilder, ProjectRepository (porta),
                  exception/ProjectNotFoundException, PlanLimitExceededException
  application/    CreateProjectUseCase, UpdateProjectUseCase, DeleteProjectUseCase,
                  FindProjectByIdUseCase, FindAllProjectsUseCase,
                  AddRoleToProjectUseCase, AddRouteToProjectUseCase, command/…
                  + package-info.java com @NamedInterface("application")
  infrastructure/ InMemoryProjectRepository (etapas 2-3), ProjectFileLoader,
                  ProjectJpaEntity, RoleJpaEntity, RouteJpaEntity,
                  ProjectJpaRepository, ProjectRepositoryAdapter (etapa 4)
  api/            ProjectController, dto/, mapper/

audit/
  domain/         AuditEvent (abstrato), PermissionCheckEvent, ProjectLifecycleEvent,
                  AuditEventRepository (porta), AuditQuery (filtro)
  application/    AuditLogListener (Observer), FindAuditEventsUseCase
  infrastructure/ InMemoryAuditEventRepository (etapas 2-3), AuditEventFileWriter,
                  AuditEventFileLoader, AuditEventJpaEntity + subclasses,
                  AuditEventJpaRepository, AuditEventRepositoryAdapter,
                  GeoLocationClient (OpenFeign)
  api/            AuditEventController, dto/, mapper/
```

### Observer e comunicação entre módulos

Usar **eventos de aplicação do Spring Modulith**, e não uma porta artesanal:

- `permission/domain/event/PermissionValidatedEvent`, com `package-info.java` anotado
  `@NamedInterface("events")` — mesmo padrão já usado em
  `billing/application/subscription/package-info.java`.
- `ValidatePermissionUseCase` publica o evento via `ApplicationEventPublisher` após
  `chain.handle(request)`.
- `audit/application/AuditLogListener` consome com `@ApplicationModuleListener`.

Isso mantém `ApplicationModulesIntegrationTests.verifiesModularStructure()` passando e é o
caminho natural para a "mensageria e processamento assíncrono" citada pelo professor.

### Arquivos texto (itens 4, 5 e 7)

- **Leitura (seed):** `src/main/resources/data/projects.txt`, `roles.txt` e `routes.txt`. As
  linhas de `roles.txt` e `routes.txt` referenciam o projeto pai — é isso que o item 5 pede
  ("atualizar os arquivos texto e as classes loader para contemplar o oneToMany").
- **Escrita:** `AuditEventFileWriter` grava cada validação em `logs/audit-events.txt`, e
  `AuditEventFileLoader` relê o arquivo reconstruindo os objetos. Assim o loader serve a uma
  necessidade real do domínio em vez de ser decorativo.

---

## Cronograma

### Executado (11/08 – 17/08, ~1h/dia)

| Dia | Data | Entrega |
|---|---|---|
| 1 | Seg 11/08 | ✅ `Project`, `Role`, `Route` no `domain` — atributos, comportamentos, `toString()` *(feito em 12/08)* |
| 2 | Ter 12/08 | ✅ `AuditEvent` abstrato + 2 subclasses; arquivos `.txt` de seed *(o `ProjectFileLoader` escorregou para o dia 3)* |
| 3 | Qua 13/08 | ✅ `ProjectFileLoader` *(feito em 14/08)*; runners de demo *(feitos em 17/08)*; **tag `etapa-1` criada em 17/08** |
| 4 | Qui 14/08 | ⚠️ `ProjectRepository` + `InMemoryProjectRepository` + use cases de leitura/criação ✅ *(feito em 17/08)*; falta a porta `AuditEventRepository` e os use cases de update/delete |

Entre 18/08 e 25/08 não houve trabalho no projeto (ver "Situação em 26/08/2026").

### Replanejado (26/08 – 31/08, mínimo 2h/dia)

| Dia | Data | Horas | Entrega |
|---|---|---|---|
| 5 | Qua 26/08 | 2h | ✅ *(feito em 29/08)* Fechar o dia 4 e a etapa 2: `UpdateProjectUseCase`, `DeleteProjectUseCase`, `AddRoleToProjectUseCase`, `AddRouteToProjectUseCase`; streams/lambdas (filtrar rotas por método, buscar por `path`, ordenar por data); porta `AuditEventRepository` + `InMemoryAuditEventRepository` → **tag `etapa-2`** |
| 6 | Qui 27/08 | 2h | ✅ *(feito em 29/08)* `ProjectController` CRUD completo (GET/POST/PUT/DELETE, 200/201/204/400/404) + DTOs + mappers + Bean Validation nos DTOs *(o antigo dia 10 foi fundido aqui: mexe nos mesmos arquivos)* |
| 7 | Sex 28/08 | 2h | ✅ *(feito em 29/08)* `AuditEventController` com filtros + anotações Swagger nos dois controllers + coleção Postman versionada → **tag `etapa-3`** |
| 8 | Sáb 29/08 | 4h | Persistência: `ProjectJpaEntity`/`RoleJpaEntity`/`RouteJpaEntity` com `@OneToMany`/`@ManyToOne`, herança `SINGLE_TABLE` no `audit`, migrations Flyway `V5`–`V7`, `JpaRepository` + adapters, remoção dos `InMemory*`, serialização sem referência circular |
| 9 | Dom 30/08 | 4h | Observer (`PermissionValidatedEvent` + `AuditLogListener` gravando banco e `.txt`), **`RoleRouteValidationHandler` com a regra real**, OpenFeign (`GeoLocationClient`), documentação (`DOMAIN`, `API`, `ARCHITECTURE`, `PATTERNS`, este arquivo) → **tag `etapa-4`** |
| 10 | Seg 31/08 | 2h | Buffer: rodar a coleção Postman inteira, `./mvnw test`, `docker compose up --build`, README de execução, PDF e postagem no Moodle |

Total planejado: **16h em 6 dias**. O caminho crítico agora é o dia 8 (persistência): sem ele
caem os itens 2, 13, 14 e 15 da rubrica de uma vez.

### Ordem de corte, se atrasar

Cortar de baixo para cima, nunca as tags:

1. **OpenFeign** (dia 9) — 1 item de rubrica (16), o mais isolado do resto.
2. **`AuditEventFileWriter`/`AuditEventFileLoader`** — os itens 4 e 5 já estão cobertos pelo
   `ProjectFileLoader`; a escrita em `.txt` é reforço, não requisito.
3. **Filtros do `AuditEventController`** — entregar só o `GET` sem query params.

Se em algum dia a etapa do dia não fechar, o dia 31/08 deixa de ser buffer e vira dia de
execução — a postagem no Moodle passa a ser a última hora do dia 31.

### Situação em 15/08/2026

O dia 11/08 não foi trabalhado; os dias 1 e 2 foram feitos juntos em 12/08. O dia 13/08 também
não foi trabalhado, e o dia 14/08 foi usado para concluir o dia 3.

**O cronograma está com cerca de dois dias de atraso:** o dia 3 ainda não fechou (falta o runner
de demo e a tag), e os dias 4 e 5 não começaram. O buffer do dia 14 (24/08) absorve isso, mas os
dias 1 a 7 são o caminho crítico — não há folga a gastar antes da `etapa-3`.

**Concluído:**
- `project/domain/` — `Project`, `Role`, `Route`, com invariantes (`maxRoles`, duplicidade
  case-insensitive de cargo e de `httpMethod`+`path`, soft delete via `deletedAt`) e seis
  exceções de domínio herdando `BusinessRuleException`.
- `audit/domain/` — `AuditEvent` abstrata + `PermissionCheckEvent` + `ProjectLifecycleEvent` +
  enum `LifecycleAction`. Herança via `@SuperBuilder`.
- Seed em `src/main/resources/data/` — `projects.txt` (3), `roles.txt` (8), `routes.txt` (10),
  separador `;`, filhos referenciando o pai por UUID.
- `project/infrastructure/ProjectFileLoader` — lê os três `.txt` do classpath e monta o grafo
  1-N chamando `addRole`/`addRoute`, de modo que o seed passa pelos invariantes do domínio.
  Campos obrigatórios validados antes do parse; `SeedFileException` reporta arquivo, linha e
  contagem de campos. Verificado: carrega 3 projetos, 8 cargos e 10 rotas.
- `ProjectNotFoundException` em `project/domain/project/exception/`, estendendo
  `ResourceNotFoundException`.
- Documentação: `DOMAIN.md` (entidades + invariantes), `ARCHITECTURE.md` (ADR-002, ADR-003 e a
  subseção sobre o risco de `equals`/`hashCode` ao reverter o ADR-003 na etapa 4).

**Pendente para fechar a `etapa-1`:**
1. `git tag etapa-1`.

### Situação em 17/08/2026

O dia 16/08 não foi trabalhado. O dia 3 foi fechado em 17/08 com os runners de demo; restam
os dias 4 a 7 (portas, `Map`, use cases, controllers) e **7 dias até o prazo** — o atraso subiu
para cerca de quatro dias e as etapas 2, 3 e 4 seguem abertas.

**Concluído em 17/08:**
- `project/infrastructure/ProjectDemoRunner` (`@Profile("demo")`, `@Order(1)`) — carrega o seed
  pelo `ProjectFileLoader` e imprime o grafo 1-N no console. Verificado: 3 projetos, 8 cargos,
  10 rotas.
- `audit/infrastructure/AuditDemoRunner` (`@Profile("demo")`, `@Order(2)`) — monta um
  `List<AuditEvent>` com um `ProjectLifecycleEvent` e dois `PermissionCheckEvent` e imprime
  polimorficamente, expondo a herança e o atributo real (`durationMs`) no console, que o runner
  de `project` sozinho não cobria. Fica no módulo `audit` porque `audit.domain` não é
  `@NamedInterface`: importá-lo de `project` quebraria o `verifiesModularStructure()`. Os eventos
  referenciam o projeto do seed por `UUID`, sem acoplamento de código entre os módulos.
- `project/domain/project/ProjectRepository` — porta com `save`, `findById`, `findAll` e
  `existsById`. Sem `deleteById`: o soft delete é comportamento do agregado (`Project.delete()`),
  e expor remoção física na porta permitiria contorná-lo por fora do domínio.
- `project/infrastructure/InMemoryProjectRepository` — `ConcurrentHashMap<UUID, Project>`
  populado num `@PostConstruct` pelo `ProjectFileLoader`, ligando os itens 5 e 7 da rubrica
  (os arquivos texto alimentam o banco simulado). Guardas `ensureProject`/`ensureId` porque
  `ConcurrentHashMap` rejeita chave nula e o NPE cru não indicaria a origem.
- `project/application/` — `CreateProjectUseCase` (com `CreateProjectCommand`),
  `FindProjectByIdUseCase` e `FindAllProjectsUseCase`. O filtro de `deletedAt` e a ordenação
  por `createdAt` ficam no use case, não na porta: mantém o adapter burro e antecipa o
  requisito de streams do Dia 5. Projeto com soft delete responde como inexistente também no
  `findById`, para não divergir do `findAll`.
- `ProjectDemoRunner` passou a injetar `FindAllProjectsUseCase` em vez de chamar o loader
  direto — a demo agora percorre `Runner → UseCase → Map`, a forma prevista para a etapa-2.
  Verificado: 3 projetos, 8 cargos, 10 rotas.
- Correção: `PermissionCheckEvent` e `ProjectLifecycleEvent` estavam com `@Data`, cujo
  `toString()` gerado sobrescrevia o de `AuditEvent` e omitia `id`, `projectId` e `occurredAt` —
  o `describe()` nunca era chamado. Trocado por `@Getter`/`@Setter`; confirmado no bytecode que a
  subclasse não gera mais `toString()`. `describe()` de `PermissionCheckEvent` passou a incluir
  a duração, para o número real aparecer na saída do console.

### Situação em 26/08/2026

O professor estendeu o prazo para **31/08/2026**. Entre 18/08 e 25/08 não houve trabalho no
projeto — o último commit é de 17/08 (`985ff94`). Restam **6 dias**, e a disponibilidade subiu
de ~1h para no mínimo 2h/dia.

**Estado verificado no repositório:**
- `etapa-1` ✅ tagueada em 17/08, apontando para `fbb99f3`.
- `project/domain/` e `audit/domain/` completos; `ProjectFileLoader`, `InMemoryProjectRepository`,
  os dois demo runners e três use cases (`CreateProject`, `FindProjectById`, `FindAllProjects`).
- `project/api/` e `audit/api/`, `audit/application/` e `audit/infrastructure` (fora do runner)
  ainda vazios — nenhum endpoint REST do escopo novo existe.
- Nenhuma entidade JPA, migration ou adapter dos módulos novos.
- `etapa-2`, `etapa-3` e `etapa-4` ❌ abertas.

**O que mudou no plano:** o cronograma de 14 dias × 1h foi comprimido para 6 dias × 2–4h. As
fusões feitas para caber:
- Bean Validation (antigo dia 10) foi para o dia do `ProjectController` — mesmos arquivos.
- Entidades JPA, adapters e serialização (antigos dias 8, 9 e parte do 10) viraram um único
  bloco de 4h no sábado.
- Observer, `RoleRouteValidationHandler`, OpenFeign e documentação (antigos dias 11, 12 e 13)
  viraram um único bloco de 4h no domingo, com o OpenFeign como primeiro item cortável.

---

## Reaproveitamento (não reinventar)

- `shared/domain/Mapper.java` — interface `Mapper<I,O>` usada por todos os mappers de API.
- `shared/api/GlobalExceptionHandler.java` — já mapeia `ResourceNotFoundException` → 404,
  `BusinessRuleException` → 409 e `MethodArgumentNotValidException` → 400. As exceções novas
  devem estender `ResourceNotFoundException`/`BusinessRuleException` para herdar o status correto.
- `identity/infrastructure/ClientRepositoryAdapter.java` — modelo de adapter domain ↔ JPA.
- `billing/application/subscription/package-info.java` — modelo de `@NamedInterface`.

---

## Não incluído

**Front-end (itens 11 e 12 da rubrica).** São 2 dos 16 itens (~12,5%). O Swagger UI não
substitui: a rubrica pede um projeto front-end que consome os endpoints e apresenta os dados.
Se sobrar tempo no dia 31/08, uma página estática única
(`src/main/resources/static/index.html` com `fetch` para `/projects` e `/audit-events`) atende
aos dois itens em cerca de 2h, sem exigir build separado.

**Spring Security / autenticação real.** Sugerida pelo professor, mas sem item de rubrica
correspondente; consumiria sozinha todo o tempo restante.

**Encapsulamento real das entidades de domínio (trocar `@Data` por `@Getter` + coleções
imutáveis).** Identificado durante o Dia 1 desta disciplina: o `@Data` gera setters públicos
que permitem furar os invariantes do domínio (ex.: `project.getRoles().add(role)` ignora a
validação de `maxRoles`). É um refactor transversal aos cinco módulos, sem item de rubrica
correspondente. Decisão e plano de execução registrados em `docs/ARCHITECTURE.md`, ADR-002.

---

## Verificação

Ao final de cada dia:

```bash
./mvnw test                       # inclui verifiesModularStructure()
./mvnw clean package -DskipTests
```

Nas etapas 1 e 2 (ainda sem REST nem banco):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Nas etapas 3 e 4:

```bash
docker compose up --build -d
curl http://localhost:8080/ping
# CRUD completo de Project, conferindo 201 / 200 / 204 / 400 / 404
curl -X POST http://localhost:8080/validate-permission -H 'Content-Type: application/json' -d '{...}'
curl http://localhost:8080/audit-events    # deve conter o evento gerado pela chamada acima
```

Antes da tag `etapa-4`: rodar a coleção Postman inteira, conferir que `GET /audit-events` não
estoura referência circular e que `logs/audit-events.txt` foi escrito.

```bash
git tag -l    # deve listar etapa-1 etapa-2 etapa-3 etapa-4
```

### Situação em 29/08/2026

Os dias 26, 27 e 28/08 não foram trabalhados. Em 29/08 os dias 5, 6 e 7 foram executados juntos:
as **etapas 2 e 3 fecharam no mesmo dia**, e restam os dias 8 (persistência JPA) e 9 (Observer,
`RoleRouteValidationHandler`, OpenFeign) mais o dia 10 de fechamento — com **2 dias até o prazo**.

O trabalho deste dia foi feito com apoio de IA (Claude) na escrita do código das camadas, o que
extrapola o modo de trabalho combinado no início da disciplina ("Claude guia, Jairo codifica").
Registrado aqui para constar na citação de fontes exigida pelo enunciado.

**Concluído — etapa 2 (tag `etapa-2` em `e9da117`):**
- `Project.update()` com validação de campo em branco e de `maxRoles` menor que os cargos já
  cadastrados; `InvalidProjectDataException`.
- `UpdateProjectUseCase`, `DeleteProjectUseCase` (soft delete pelo agregado, sem remoção na porta),
  `AddRoleToProjectUseCase`, `AddRouteToProjectUseCase`.
- `SearchProjectsUseCase` (filtro por trecho de nome + situação, ordenação alfabética) e
  `FindProjectRoutesUseCase` (filtro por método HTTP, ordenação por `path`) — os exemplos de
  Collections/lambdas/Streams exigidos pela Etapa 2.
- `ProjectDemoRunner` passou a imprimir resumo (`map`/`reduce`), busca e filtro de rotas.
- Decisão: a porta `AuditEventRepository` **não** entrou na etapa 2 como estava previsto — os itens
  7 e 8 da rubrica já estão cobertos pelo `Map` de `InMemoryProjectRepository` e pelos use cases.
  Ela acabou entrando junto com o `audit` na etapa 3, que é onde passou a ter uso real.

**Concluído — etapa 3:**
- `ProjectController`: 8 endpoints (CRUD de projeto + sub-recursos `roles` e `routes`), com
  201 + `Location`, 204, 400, 404 e 409, Bean Validation nos DTOs e anotações OpenAPI.
- Mappers de API: a montagem dos commands saiu do controller. `CreateProjectMapper`,
  `SearchProjectsMapper` e os mappers de resposta implementam `Mapper<I,O>`; `UpdateProjectMapper`,
  `AddRoleToProjectMapper` e `AddRouteToProjectMapper` ficam fora da interface porque o command
  junta o `projectId` do caminho com o corpo da requisição — decisão documentada no javadoc.
- Serialização do 1-N: o pai embute os filhos e cada filho referencia o pai por `projectId`,
  eliminando a referência circular sem `@JsonIgnore` (antecipa o requisito 6 da Etapa 4).
- `audit` completo para leitura: porta `AuditEventRepository`, `InMemoryAuditEventRepository`
  (que atribui o `id` no `save`, respeitando o ADR-001), `FindAuditEventsUseCase` com filtros
  `type`/`projectId`/`onlyDenied`, `AuditEventController` (`GET /audit-events`) e um único
  `AuditEventResponse` para toda a hierarquia, via `type()` e `describe()`.
- **Observer antecipado do dia 9:** `PermissionValidatedEvent` (pacote `permission/domain/event`
  com `@NamedInterface("events")`), publicado por `ValidatePermissionUseCase` e consumido por
  `AuditLogListener`. Usa `@EventListener` e não `@ApplicationModuleListener` porque este último
  implica `AFTER_COMMIT` e o fluxo ainda não é transacional — trocar na etapa 4.
- Coleção Postman versionada em `docs/postman/permission-saas.postman_collection.json`:
  26 requisições em 4 pastas, encadeadas por variáveis, verificadas com `newman` (30 asserções,
  0 falhas, re-executável).
- Documentação atualizada: `API.md` (todos os endpoints novos + coleção), `PATTERNS.md` (Observer
  movido para "implementados"), `ARCHITECTURE.md` (mapa de módulos, fluxo de negócio, estrutura de
  pacotes).

**Achados registrados durante os testes da API:**
1. `POST /plans` devolve **200** em vez de 201, divergindo dos demais endpoints de criação.
2. `POST /plans` com nome repetido devolve **500**: a constraint `uq_plans_name` estoura sem
   exceção de domínio correspondente. O correto seria 409 via `PlanNameAlreadyInUseException`.
3. A descrição do evento de auditoria mostra `httpMethod = null`, porque
   `ValidatePermissionRequest` só carrega `route`. Fecha na etapa 4 junto com a regra real do
   `RoleRouteValidationHandler`.

**Pendente:** dias 8, 9 e 10 — persistência JPA (itens 2, 13, 14 e 15 da rubrica), regra real do
`RoleRouteValidationHandler`, OpenFeign (item 16, primeiro cortável) e o front-end estático
(itens 11 e 12), se sobrar tempo no dia 31.

### Situação em 30/08/2026

O dia 8 (persistência) não tinha sido executado em 29/08 — o repositório fechou o dia 29 com as
etapas 2 e 3 prontas, mas sem nenhuma `@Entity` nos módulos novos e com as migrations paradas na
`V4`. O dia 8 passou para 30/08, e o dia 9 (Observer restante, `RoleRouteValidationHandler`,
OpenFeign, documentação) mais o dia 10 de fechamento ficam para 31/08, o próprio dia do prazo.

**Concluído — schema (primeiro item do dia 8):**
- Migrations `V5`–`V8`: `projects`, `roles`, `routes` e `audit_events`. O plano previa `V5`–`V7`,
  mas são quatro tabelas — `audit_events` ganhou arquivo próprio, seguindo a convenção de uma
  tabela por migration já usada em `V1`–`V4`.
- `roles` e `routes` com FK para `projects` e `ON DELETE CASCADE`, acompanhando o
  `cascade = ALL` / `orphanRemoval = true` previsto para o `@OneToMany` (item 3 do checklist do
  ADR-003). `projects.client_id` e `audit_events.project_id` ficaram **sem** FK — decisão e
  motivos em `docs/ARCHITECTURE.md`, ADR-004.
- `audit_events` em `SINGLE_TABLE`, discriminada por `event_type`, com os valores iguais aos de
  `AuditEvent.type()`. As colunas de subclasse são nullable (limitação do SINGLE_TABLE) e a
  obrigatoriedade volta como `CHECK` condicionado ao discriminador.
- Índices únicos case-insensitive espelhando os invariantes de `addRole`/`addRoute`, e `CHECK`
  espelhando as Bean Validations de `AddRouteRequest` e `CreateProjectRequest`.
- Verificado num Postgres 16 limpo: as oito migrations aplicam em sequência, o seed dos três
  `.txt` entra íntegro (3 projetos, 8 cargos, 10 rotas) e 16 casos de constraint se comportam
  como o domínio (duplicidade de cargo/rota, método HTTP inválido, `max_roles` zero, evento de
  auditoria sem `granted`, discriminador desconhecido, cargo órfão).

**Decisão pendente antes de escrever a `ProjectJpaEntity`:** qual das três opções do ADR-003
(`Persistable`, chave natural ou `equals` null-safe) resolve o conflito `@Builder.Default` do
`id` × `SimpleJpaRepository.save()`. O schema não força nenhuma das três — as PKs têm
`DEFAULT gen_random_uuid()`, que funciona tanto com `id` vindo do domínio quanto gerado pelo
banco.

**Próximo no dia 8:** entidades JPA (`ProjectJpaEntity` com `@OneToMany`, `Role`/`Route` com
`@ManyToOne`, `AuditEventJpaEntity` + subclasses), `JpaRepository` com pelo menos uma consulta
derivada (item 4 da Etapa 4), adapters implementando as portas e remoção dos `InMemory*`.

### Situação em 31/08/2026 — dia da entrega

Os dias 8, 9 e 10 foram executados em 31/08, no próprio dia do prazo. A **etapa 4 fechou**; o
OpenFeign foi cortado, seguindo a "ordem de corte" definida neste plano.

Como em 29/08, o código das camadas deste dia foi escrito com apoio de IA (Claude), o que extrapola o
modo de trabalho combinado no início da disciplina ("Claude guia, Jairo codifica"). Registrado aqui
para constar na citação de fontes exigida pelo enunciado.

**Concluído — dia 8 (persistência JPA):**
- `ProjectJpaEntity` com `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)` para `RoleJpaEntity`
  e `RouteJpaEntity`, ambas com `@ManyToOne` de volta.
- `AuditEventJpaEntity` abstrata em `SINGLE_TABLE`, discriminada por `event_type`, com
  `PermissionCheckEventJpaEntity` e `ProjectLifecycleEventJpaEntity` como `@DiscriminatorValue`.
- `JpaProjectRepository` e `JpaAuditEventRepository` estendendo `JpaRepository`, com consultas
  derivadas: `findByDeletedAtIsNullOrderByCreatedAtAsc`,
  `findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc`,
  `findByClientIdAndDeletedAtIsNullOrderByCreatedAtAsc`, `findByIdAndDeletedAtIsNull`,
  `findAllByOrderByOccurredAtDesc`, `findByProjectIdOrderByOccurredAtDesc`.
- `ProjectRepositoryAdapter` e `AuditEventRepositoryAdapter` implementando as portas. O adapter de
  projeto lê antes de gravar e sincroniza as coleções filhas **na instância gerenciada**, que é o que
  o `orphanRemoval` exige.
- `InMemoryProjectRepository` e `InMemoryAuditEventRepository` removidos (autorizado pelo enunciado da
  Etapa 4; preservados na tag `etapa-3`).
- `PurgeProjectUseCase` + `DELETE /projects/{id}/purge` — é o `deleteById` que a Etapa 4 pede, num uso
  real, com cascata para cargos e rotas.
- A decisão pendente do ADR-003 foi resolvida: **nenhuma das três opções foi necessária**. Como a
  entidade JPA é classe separada da entidade de domínio, usa `@Id` atribuído (sem `@GeneratedValue`) e
  não tem `@Version`, o conflito com o ADR-001 não se materializa. `Persistable` chegou a ser
  implementado e foi removido — ver "Resolução na etapa 4" no ADR-003.

**Concluído — dia 9:**
- **`RoleRouteValidationHandler` com a regra real.** Nova porta `permission/domain/RouteAccessChecker`,
  adapter `permission/infrastructure/ProjectRouteAccessChecker` e
  `project/application/project/CheckRouteAccessUseCase`. Mesma forma de integração já usada com o
  `billing`. Cinco motivos distintos de negativa, em vez de um `allow()` fixo.
- `ValidatePermissionRequest` ganhou `projectId` (`@NotNull`) e `httpMethod` (`@Pattern` com os cinco
  verbos), com `@Size`/`@Pattern` também em `role` e `route`. Isso fecha o achado nº 3 de 29/08: o
  evento de auditoria não grava mais `httpMethod = null`.
- `AuditEventJournal` + `AuditEventFileWriter`: cada validação também é gravada em
  `logs/audit-events.txt`. O `AuditLogListener` grava pelas duas portas sem saber qual é banco e qual
  é arquivo.
- Documentação atualizada: `ARCHITECTURE.md` (mapa de módulos, fluxo, ADR-003 resolvido, ADR-005 e
  ADR-006 novos, estrutura de pacotes), `API.md`, `DOMAIN.md`, `PATTERNS.md`, `TEST-ARCHITECTURE.md`.

**Concluído — dia 10 (fechamento):**
- `ProjectRepositoryAdapterIT` com 5 casos cobrindo o 1-N, o append em agregado já gravado, as duas
  consultas derivadas e a cascata do `deleteById`.
- Plugin **failsafe** adicionado ao `pom.xml`. Os testes `*IT` existiam mas nunca eram executados;
  agora `./mvnw verify` roda a suíte inteira — 22 testes de unidade + 6 de integração, todos verdes.
- `Dockerfile` deixou de usar `-DskipTests`: o build da imagem roda os testes.
- Coleção Postman atualizada e re-executada com `newman`: **30 requisições, 39 asserções, 0 falhas**.
- Verificação manual ponta a ponta contra PostgreSQL 16 com `ddl-auto: validate` — o mapeamento JPA
  bate com as migrations Flyway `V1`–`V8` sem ajuste.

**Mudanças de escopo decididas em 31/08:**

1. **Seed e loaders de arquivo texto removidos do código final** (`ProjectFileLoader`,
   `SeedFileException`, `ProjectSeedRunner`, `src/main/resources/data/*.txt`). Motivo e consequência em
   `docs/ARCHITECTURE.md`, ADR-005. ⚠️ Os itens **4, 5, 7 e 8** da rubrica passam a ser evidenciados
   **apenas pelas tags** `etapa-1` a `etapa-3`, não pelo `HEAD`.
2. **Submódulos por entidade em todas as camadas do `project`** (`project/`, `role/`, `route/`),
   espelhando o `billing`; `RoleController` e `RouteController` saíram do `ProjectController`, com as
   mesmas URLs. Ver ADR-006.

**Cortado, como previsto na "ordem de corte":**

- **OpenFeign / `GeoLocationClient`** (item 16 da rubrica) — primeiro da lista de corte. Some-se a isso
  que o projeto está em Spring Boot 4.1.0 e a versão de Spring Cloud compatível teria de ser validada,
  risco alto para o tempo restante. `PermissionCheckEvent` já tem os campos `ipAddress` e `country`,
  que é onde o cliente entraria — a lacuna é só o cliente HTTP.
- **Front-end estático** (itens 11 e 12) — não houve tempo; segue como "Não incluído".
- **Achados 1 e 2 de 29/08** (`POST /plans` devolvendo 200 em vez de 201, e 500 em nome duplicado) —
  são do módulo `billing`, entregue na disciplina anterior. Ficam registrados como dívida.

### Escopo acrescentado em 31/08 — `RoleRoute` (a regra de negócio de verdade)

Durante a revisão da entrega ficou claro que a validação de permissão ainda não respondia à pergunta
que o produto vende: **o cargo X pode acessar a rota Y no projeto Z?** Faltava a associação entre
`Role` e `Route`. Foi implementada no mesmo dia.

**Modelo:**

```
Project 1 ──── N Role  1 ──── N RoleRoute
Project 1 ──── N Route 1 ──── N RoleRoute
```

`RoleRoute` é **entidade associativa com histórico** (`grantedAt` / `revokedAt`), não tabela de
junção. Revogar fecha a linha em vez de apagá-la, e é isso que permite à auditoria responder *quando*
um cargo deixou de poder acessar uma rota. Decisão completa em `docs/ARCHITECTURE.md`, ADR-007.

**Entregue:**
- Migration `V9__create_role_routes_table.sql`, com FK para `roles` e `routes` (`ON DELETE CASCADE`)
  e o índice único **parcial** `uq_role_routes_active ... WHERE revoked_at IS NULL`, que garante no
  máximo uma concessão ativa por par sem impedir o empilhamento de histórico.
- `project/domain/roleroute/` — `RoleRoute` + três exceções de domínio.
  `Project.grantRouteToRole()`, `Project.revokeRouteFromRole()` e `Project.allows()`; `Role` passou a
  carregar `List<RoleRoute> permissions`, com `hasActiveAccessTo()` e `activePermissions()`.
- `RoleRouteJpaEntity` com `@ManyToOne` para `Role` **e** para `Route`; `RoleJpaEntity` ganhou o
  `@OneToMany` com cascata e o `RouteJpaEntity` o inverso somente leitura.
- Use cases `GrantRouteToRoleUseCase`, `RevokeRouteFromRoleUseCase`, `FindRolePermissionsUseCase` e o
  `RoleRouteController` (`POST`/`DELETE`/`GET` em
  `/projects/{projectId}/roles/{roleId}/routes/{routeId}`, com `?includeRevoked=true` para o histórico).
- `CheckRouteAccessUseCase` ganhou o resultado `ROLE_HAS_NO_ACCESS_TO_ROUTE`, que vira a negativa
  `role has no active grant on this route` no `RoleRouteValidationHandler`.
- `RoleResponse` passou a expor as concessões ativas, então `GET /projects/{id}` mostra o grafo
  `Project → Role → RoleRoute` inteiro numa resposta só.

**Bug encontrado e corrigido pelos testes de integração:** um agregado que **nasce** com concessões
falhava no primeiro `save()` com `ObjectRetrievalFailureException` — a `RoleRouteJpaEntity` aponta para
a `RouteJpaEntity` por `@ManyToOne` sem cascata, e a rota ainda era transiente. O adapter passou a
gravar em duas etapas dentro da mesma transação: esqueleto primeiro, concessões depois. Pela API o
problema não aparecia, porque rota e concessão chegam em requisições separadas.

**Verificação final:**
- `./mvnw verify` — 22 testes de unidade + **9** de integração, 0 falhas.
- Coleção Postman com uma pasta nova ("Role x Route — concessão e histórico"):
  **42 requisições, 60 asserções, 0 falhas** no `newman`.
- Fluxo conferido contra PostgreSQL 16: sem concessão o acesso é negado; concedido, é permitido;
  revogado, volta a ser negado; e a tabela `role_routes` fica com as duas linhas do histórico.

Com isso o item "`Role`↔`Route` N-N" sai de "trabalho futuro" e entra na entrega.

### Refinamento final de 31/08 — mappers de persistência por submódulo

Revisão de código antes do fechamento apontou duas coisas no `ProjectRepositoryAdapter`, ambas
corrigidas:

1. **Helpers genéricos demais.** Havia um `index(List<T>, Function<T, UUID>)` e um
   `ids(List<T>, Function<T, UUID>)` montando `Map`/`Set` para casar as listas por id. Trocados por
   cinco buscas com nome (`findRoleEntity`, `findRouteEntity`, `findRole`, `findRoute`,
   `findPermission`). Ficou O(n²) em vez de O(n), o que é irrelevante na escala de um projeto (poucas
   dezenas de cargos e rotas) e deixa o código legível sem conhecer `Function`.
2. **O adapter traduzia entidades que não são dele.** A tradução saiu para um mapper por submódulo —
   `ProjectJpaMapper`, `RoleJpaMapper`, `RouteJpaMapper`, `RoleRouteJpaMapper` —, cada um no pacote da
   sua entidade, como manda o ADR-006. O adapter ficou só com a orquestração do agregado e as
   consultas, caindo de 317 para 195 linhas.

A divisão de responsabilidade está documentada em `docs/ARCHITECTURE.md`, seção "Mapper de
persistência × adapter — quem faz o quê".

Reverificado depois das duas mudanças: `./mvnw verify` com 22 testes de unidade + 9 de integração e a
coleção Postman com 42 requisições / 60 asserções, tudo sem falha.
