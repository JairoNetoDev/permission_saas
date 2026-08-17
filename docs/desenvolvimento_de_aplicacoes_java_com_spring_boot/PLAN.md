# 📋 Planejamento — Desenvolvimento de aplicações Java com Spring Boot

**Aluno:** Jairo Williams Guedes Lopes Neto
**Disciplina:** Desenvolvimento de aplicações Java com Spring Boot
**Prazo:** 24/08/2026 23:59 (entrega única no Moodle)
**Disponibilidade:** ~1h/dia
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

| Dia | Data | Entrega |
|---|---|---|
| 1 | Seg 11/08 | ✅ `Project`, `Role`, `Route` no `domain` — atributos, comportamentos, `toString()` *(feito em 12/08)* |
| 2 | Ter 12/08 | ✅ `AuditEvent` abstrato + 2 subclasses; arquivos `.txt` de seed *(o `ProjectFileLoader` escorregou para o dia 3)* |
| 3 | Qua 13/08 | ✅ `ProjectFileLoader` *(feito em 14/08)*; runners de demo *(feitos em 17/08)* → falta só criar a tag `etapa-1` |
| 4 | Qui 14/08 | ⚠️ `ProjectRepository` + `InMemoryProjectRepository` + use cases de leitura/criação ✅ *(feito em 17/08)*; falta a porta `AuditEventRepository` e os use cases de update/delete |
| 5 | Sex 15/08 | Streams e lambdas: filtrar rotas por método, ordenar eventos por data, buscar por path, transformar em DTO; exceções de domínio → **tag `etapa-2`** |
| 6 | Sáb 16/08 | `ProjectController` CRUD completo (GET/POST/PUT/DELETE, 200/201/204/400/404) + DTOs + mappers |
| 7 | Dom 17/08 | `AuditEventController` com filtros + anotações Swagger + coleção Postman versionada → **tag `etapa-3`** |
| 8 | Seg 18/08 | `ProjectJpaEntity` com `@OneToMany`/`@ManyToOne`, herança `SINGLE_TABLE` no audit, migrations Flyway `V5`–`V7` |
| 9 | Ter 19/08 | `JpaRepository` + adapters; remover os `InMemory*`; consultas `findBy…` + operação de filtro/ordenação |
| 10 | Qua 20/08 | Bean Validation nos DTOs novos + serialização sem referência circular |
| 11 | Qui 21/08 | Observer: evento publicado no `permission`, `AuditLogListener` grava no banco e no `.txt`; **completar o `RoleRouteValidationHandler`** com a regra real |
| 12 | Sex 22/08 | OpenFeign: enriquecer `PermissionCheckEvent` com país/cidade do IP |
| 13 | Sáb 23/08 | Documentação (`DOMAIN`, `API`, `ARCHITECTURE`, `PATTERNS`, este arquivo), README de execução, revisão → **tag `etapa-4`** |
| 14 | Dom 24/08 | Buffer, PDF e postagem no Moodle |

Os dias 1 a 7 são o caminho crítico. Em caso de atraso, cortar o OpenFeign (dia 12) — nunca as tags.

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
Se sobrar tempo no dia 14, uma página estática única
(`src/main/resources/static/index.html` com `fetch` para `/projects` e `/audit-events`) atende
aos dois itens em cerca de 2h, sem exigir build separado.

**Spring Security / autenticação real.** Sugerida pelo professor, mas sem item de rubrica
correspondente; consumiria os 14 dias disponíveis.

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
