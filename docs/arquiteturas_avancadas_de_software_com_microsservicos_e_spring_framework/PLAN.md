# 📋 Planejamento — Arquiteturas avançadas de software com microsserviços e Spring Framework

**Aluno:** Jairo Williams Guedes Lopes Neto
**Disciplina:** Arquiteturas avançadas de software com microsserviços e Spring Framework
**Prazo:** 05/10/2026 23:59 (entrega única no Moodle)
**Disponibilidade:** ~1h/dia nos dias úteis; 3–4h nos fins de semana (26–27/09 e 03–04/10)
**Base:** projeto Permission SaaS, na versão entregue em 31/08/2026 (tag `etapa-4`)

> ⚠️ Este arquivo é o plano **desta** disciplina. Os planos anteriores
> (`docs/clean_code_e_padroes_de_projeto/PLAN.md` e
> `docs/desenvolvimento_de_aplicacoes_java_com_spring_boot/PLAN.md`) são evidência já
> submetida e **não devem ser alterados**.

---

## Contexto

O Permission SaaS chega nesta disciplina como um **monolito modular** com cinco módulos
(`shared`, `identity`, `billing`, `permission`, `project`, `audit`), persistência em PostgreSQL
via Flyway, CRUD REST completo, Swagger e fronteiras verificadas pelo Spring Modulith.

A disciplina pede o caminho seguinte: sair do monolito para uma solução distribuída — extrair um
serviço, comunicar por HTTP com OpenFeign, externalizar configuração, containerizar tudo e
introduzir mensageria e processamento em lote.

Três itens que estavam registrados como *trabalho futuro* no `docs/` entram agora no escopo:
**OpenFeign** (cortado na disciplina anterior), **mensageria assíncrona para a trilha de
auditoria** e **bancos separados por serviço**. É a evolução aditiva que o `CLAUDE.md` pede — nada
do que já foi entregue é reescrito.

### Lacunas medidas contra a rubrica (estado em 21/09/2026)

| Item da rubrica                                                                   | Estado hoje                                                                                                                                            |
| --------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 1 — Controller/Service/Repository separados, sem regra de negócio no controller | ✅ atendido (`api` → `application` → `domain` ← `infrastructure`)                                                                           |
| 2 — módulos do domínio identificáveis                                         | ✅ no código; ❌ falta a apresentação dos módulos no`README.md` como a Etapa 1 pede                                                              |
| 3 — Bean Validation, tratamento de exceções, consultas Spring Data, Swagger    | ⚠️ parcial: validação e`GlobalExceptionHandler` ✅; consultas derivadas existem mas são poucas; anotações Swagger em apenas 5 dos controllers |
| 4 — dependências entre módulos + candidata à separação                      | ❌ análise não escrita                                                                                                                               |
| 5 — funcionalidade extraída para um serviço Spring Boot independente           | ❌ inexistente                                                                                                                                         |
| 6 — API REST do novo serviço com DTOs e Swagger                                 | ❌ inexistente                                                                                                                                         |
| 7 — OpenFeign + endereço externalizado                                          | ❌ inexistente (cortado na disciplina anterior)                                                                                                        |
| 8 — tratamento da indisponibilidade do serviço remoto                           | ❌ inexistente                                                                                                                                         |
| 9 — Profiles + variáveis de ambiente                                            | ⚠️ parcial: há`${VAR:default}` no `application.yml`, mas **nenhum** `application-dev`/`application-prod`                              |
| 10 — banco relacional, cada serviço dono dos próprios dados                    | ⚠️ PostgreSQL ✅, mas há um único banco para tudo                                                                                                  |
| 11 — imagens Docker das duas aplicações                                        | ⚠️ só a aplicação principal tem`Dockerfile`                                                                                                     |
| 12 — Docker Compose integrando os componentes                                    | ⚠️ compose atual sobe app + Postgres apenas                                                                                                          |
| 13, 14 — produtor, fila e consumidor de mensagens                                | ❌ inexistente (hoje o Observer é síncrono, em processo)                                                                                             |
| 15 — Job Spring Batch com chunks                                                 | ❌ inexistente                                                                                                                                         |
| 16 — quando usar REST, mensageria ou Batch                                       | ❌ reflexão não escrita                                                                                                                              |
| Tags de marco                                                                     | ✅ `arq-etapa-1` … `arq-etapa-4` — ver Decisão 4 |

---

## Decisões

1. **Política de IA (🟢).** Esta disciplina incentiva o uso de IA, exigindo citação. O modo de
   trabalho muda em relação à anterior: a IA pode escrever código, e não só plano e documentação.
   Em contrapartida, o `README.md` ganha uma seção **Uso de IA** declarando ferramenta
   (Claude Code / Opus 5), em que partes foi usada e o que foi revisado pelo aluno.
   A regra registrada no `CLAUDE.md` para a disciplina anterior deixa de valer aqui.
2. **Serviço a extrair: `audit`.** É a escolha coerente com o domínio, não uma extração para
   cumprir requisito:

   - a trilha de auditoria é *append-only* e ninguém do fluxo principal lê dela para decidir nada;
   - já é consumida por evento (`PermissionValidatedEvent` → `AuditLogListener`), ou seja, o
     acoplamento com o resto já é o mais fraco do projeto;
   - tem dados próprios (`audit_events`) e sem FK para as tabelas dos outros módulos (ADR-004) —
     separar o banco não quebra integridade referencial;
   - cresce em volume por um motivo diferente do resto (uma validação de permissão gera um evento;
     um projeto criado gera um), então escala de forma independente.

   Extrair `billing` seria o oposto: `ApiKeyValidationHandler` depende dele **dentro** da
   requisição de validação, e a separação transformaria uma chamada de método em ponto de falha no
   caminho crítico.
3. **Layout do repositório: pastas irmãs, não multi-módulo Maven.** Um repositório só, com cada
   aplicação na sua pasta e `pom.xml` próprio, sem `pom` agregador:

   ```
   permission_saas/
   ├── permission-service/  aplicação principal
   ├── audit-service/       serviço extraído
   ├── config-server/       Spring Cloud Config Server
   └── docker-compose.yml   orquestra todos
   ```

   **Revisada em 28/09/2026.** A primeira versão mantinha a aplicação principal na raiz, para não
   "sujar o diff" movendo `src/`. O Git registra o movimento como renomeação, sem linha alterada,
   e as tags antigas seguem intactas; já o serviço aninhado dentro da aplicação principal
   confundia a leitura do repositório e a IDE. Decisão, revisão e alternativa descartada no
   **ADR-008** de `docs/ARCHITECTURE.md`.
4. **Nome das tags — há conflito.** A disciplina pede `etapa-1` … `etapa-4`, mas essas tags **já
   existem** apontando para a disciplina de Spring Boot. Mover qualquer uma delas destrói a
   evidência já avaliada.

   - **Aprovado pelo professor em 28/09/2026:** `arq-etapa-1` … `arq-etapa-4`, com uma tabela no `README.md`
     ligando cada tag ao marco correspondente da disciplina. `arq-etapa-1` foi a primeira criada.
   - As tags antigas `etapa-1` … `etapa-4` nunca são reapontadas.
5. **A consulta de auditoria continua exposta pela aplicação principal, como proxy Feign.** Ao
   extrair o `audit`, a aplicação principal mantém um `GET /audit-events` que **não toca banco**:
   delega ao `audit-service` pelo `AuditClient`. É o desenho que a Etapa 2 pede
   (`Controller → Service → Feign Client → HTTP → Microsserviço`) e resolve três coisas de uma vez:

   - **mantém os itens 7 e 8 vivos na tag final.** Na Etapa 4 a *gravação* migra para a fila; sem
     este endpoint, o `@FeignClient` ficaria sem nenhum chamador em `arq-etapa-4` — justamente a
     tag que o professor abre — e o OpenFeign viraria código morto;
   - **dá a demonstração de indisponibilidade** que o item 9 da Etapa 2 exige: com o
     `audit-service` parado, a consulta degrada com resposta amigável enquanto o
     `POST /validate-permission` continua respondendo;
   - **é barato**: um controller fino, sem banco, sem projeto novo.
6. **Mensageria: RabbitMQ** (confirmado com o professor em 28/09/2026). Produtor na aplicação
   principal, fila `audit.events`, consumidor no `audit-service`. **Quem publica na fila é a aplicação
   principal, direto no RabbitMQ** — e não o `audit-service` enfileirando o que recebe por HTTP: com
   a fila atrás do serviço, a aplicação principal continuaria esperando a resposta HTTP e perderia o
   evento com o serviço fora do ar; com o broker entre os dois, a mensagem espera na fila até o
   consumidor voltar. Na Etapa 2 a gravação de
   auditoria passa por Feign (síncrona); na Etapa 4 ela migra para a fila, e o Feign permanece
   para a **consulta** (`GET /audit-events`). Isso dá à Etapa 4 uma comparação real entre os dois
   estilos dentro do mesmo domínio, em vez de dois mecanismos desconexos. A reflexão da Etapa 4
   parte do que a Etapa 2 mostrar na prática — com o `audit-service` fora do ar, a validação espera
   o timeout do Feign e o evento de auditoria se perde —, e a fila entra como a evolução que resolve
   essa dor, não como requisito cumprido.
7. **Batch: importação de rotas por CSV.** Um cliente que migra sua API para o SaaS precisa
   cadastrar dezenas de rotas de uma vez — é a operação do domínio que naturalmente é lote, e não
   requisição. `Job` → `Step` (chunk 10) → `FlatFileItemReader` → processor que normaliza
   `httpMethod`/`path` e descarta duplicadas → writer que persiste via o repositório do módulo
   `project`.

---

## Arquitetura alvo

### Hoje (tag `etapa-4` da disciplina anterior)

```
Cliente HTTP
↓
Aplicação Spring Boot (monolito modular)
├── identity ├── billing ├── permission ├── project ├── audit
↓
PostgreSQL (permissions_saas)
```

### Ao final da Etapa 2

```
Cliente HTTP ──▶ Aplicação Principal
                 ├── permission / project / billing / identity
                 └── AuditClient ──Feign──▶ audit-service
                                            └── audit_events (grava e consulta)
```

### Ao final da Etapa 4

```
Config Server
  ↓ (configuração)
Aplicação Principal ──Feign──▶ audit-service        (consulta)
        │                          ▲
        └──mensagem──▶ RabbitMQ ───┘                (gravação)
        │
        └── Spring Batch: CSV de rotas → Job → project

Aplicação Principal → permissions_saas      audit-service → audit_db
```

---

## Cronograma

### Etapa 1 — Organização Arquitetural (22–24/09)

| Dia | Data      | Horas | Entrega                                                                                                                                                                                                                                                                      |
| --- | --------- | ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1 ✅ | Ter 22/09 | 1h    | **Feito em 23/09.** `README.md`: apresentação dos módulos e responsabilidades, análise de dependências (ex.: `permission → project` via `RouteAccessChecker`, `permission → billing` via `ApiKeyValidator`) e justificativa do `audit` como candidato a serviço independente |
| 2 ✅ | Qua 23/09 | 1h    | **Feito em 28/09, com ajuste de escopo.** Duas consultas JPQL no `audit` — `search` (tipo, projeto, período) e `searchDenied` (negadas por projeto e período) —, com a filtragem saindo da memória para o banco (**ADR-009**); verificadas contra o PostgreSQL real. Ficaram de fora: a consulta de rotas por método/`path` (as rotas vivem dentro do agregado `Project`, ADR-006, e um repositório só de rotas quebraria esse desenho), o teste de integração das consultas (vai para o `audit-service`) e a varredura dos `@Size` que faltam em `RegisterClientRequest`/`RegisterPlanRequest` (buffer do dia 14). De carona: Queries separadas dos Commands em `application/query/` (`docs/PATTERNS.md`) |
| 3 ✅ | Qui 24/09 | 1h    | **Feito em 25/09.** Anotações Swagger (`@Tag`, `@Operation`, `@ApiResponse`) nos controllers que faltavam; **ADR-008** (layout de vários projetos no mesmo repositório). **Tag `arq-etapa-1`** criada no fechamento do dia 2, em 28/09                                                                           |

### Etapa 2 — Separação e Comunicação (25–28/09)

| Dia | Data       | Horas | Entrega                                                                                                                                                                                                                                                                                                                                                                                    |
| --- | ---------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 4 ✅ | Sex 25/09  | 1h    | **Feito em 28/09.** Esqueleto do `audit-service/`: `pom.xml` (Spring Boot 4.1.0, sem Security), classe `@SpringBootApplication`, `application.yml` na porta 8081; cópia do `audit/domain`, do mapeamento JPA e das consultas JPQL do ADR-009 |
| 5 ✅ | Sáb 26/09 | 3h    | **Feito em 28/09, com ajuste.** `GET /audit-events` (com os filtros da etapa 1) e `POST /audit-events/permission-checks` — o caminho é específico do tipo porque o corpo só serve para validações de permissão —, DTOs de contrato próprios, Bean Validation, `GlobalExceptionHandler`, Swagger, migration `V1`; pasta `audit-service (8081)` na coleção do Postman e seção no `docs/API.md` |
| 6 ⚠️ | Dom 27/09  | 3h    | **Começado em 28/09:** BOM Spring Cloud `2025.1.3` + `spring-cloud-starter-openfeign`, `@EnableFeignClients` e `audit.service.url` feitos. **Em 30/09:** `AuditClient` e a gravação pelo Feign com timeout e `try/catch`. O restante está em "Onde parou" abaixo. Aplicação principal:`AuditClient` (`@FeignClient`) no lugar do `AuditEventRepositoryAdapter`; `AuditLogListener` passa a chamar o cliente; `GET /audit-events` da aplicação principal vira proxy Feign (Decisão 5); **tratamento de indisponibilidade** (falha do Feign não pode derrubar a validação de permissão nem vazar stack trace); URL em `audit.service.url`; remoção do `audit` do módulo principal (controller e adapter JPA) |
| 7   | Seg 28/09  | 1h    | Testes da comunicação pelo Postman/Swagger (serviço isolado, operação via aplicação principal, serviço fora do ar); reflexão arquitetural da Etapa 2 no `README.md` → **tag `arq-etapa-2`**. Folga desta hora é buffer do fim de semana anterior                                                                                                                    |

### Etapa 3 — Configuração e Execução (29/09–02/10)

| Dia | Data      | Horas | Entrega                                                                                                                                                                                                                                                  |
| --- | --------- | ----- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 8   | Ter 29/09 | 1h    | `application-dev`/`application-prod` nos três serviços; variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AUDIT_SERVICE_URL`); `.env.example` atualizado                                                                  |
| 9 ✅ | Qua 30/09 | 1h    | **Metade feita em 28/09:** o `audit-service` já nasceu com banco próprio (`audit_db`, container `audit-postgres`, usuário `audit`, porta 5433). **Resto em 30/09:** migration `V10` na aplicação principal removendo `audit_events`, junto com a persistência local do `audit` (ADR-010) |
| 10  | Qui 01/10 | 1h    | `config-server/` com Spring Cloud Config (backend de arquivos versionado em `config-repo/`); aplicação principal e `audit-service` passam a buscar configuração nele                                                                           |
| 11  | Sex 02/10 | 1h    | `Dockerfile` do `audit-service` e do `config-server`; `docker-compose.yml` subindo tudo em rede própria (sem `localhost` entre containers); reflexão arquitetural da Etapa 3 → **tag `arq-etapa-3`** |

### Etapa 4 — Assíncrono e Batch (03–05/10)

| Dia | Data       | Horas | Entrega                                                                                                                                                                                                                                                             |
| --- | ---------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 12  | Sáb 03/10 | 4h    | RabbitMQ no compose; produtor na aplicação principal publicando o evento de validação na fila`audit.events`; consumidor no `audit-service` gravando no banco dele; demonstração com o consumidor parado (mensagem aguardando na fila) e depois religado   |
| 13  | Dom 04/10  | 4h    | Spring Batch:`Job` de importação de rotas por CSV com `Step`, `ItemReader`, `ItemProcessor`, `ItemWriter` e chunk; arquivo de exemplo com múltiplos registros; reflexão REST × mensageria × Batch no `README.md` → **tag `arq-etapa-4`** |
| 14  | Seg 05/10  | 1–2h | Buffer:`./mvnw verify` nos quatro projetos, `docker compose up --build` do zero, coleção Postman atualizada, seção **Uso de IA**, PDF e postagem no Moodle                                                                                            |

Total planejado: **~24h em 14 dias**. O caminho crítico é o fim de semana de 26–27/09 (extração +
Feign): sem ele caem de uma vez os itens 5, 6, 7 e 8 da rubrica, e as Etapas 3 e 4 ficam sem o
segundo serviço para configurar, containerizar e alimentar por fila.

### Replanejamento em 28/09

A Etapa 1 fechou com quatro dias de atraso e o fim de semana de 26–27/09 não entregou a extração.
Os dias restantes ficam assim; as tabelas acima continuam valendo como descrição do conteúdo de
cada dia.

| Data | Dias do plano | Entrega |
|---|---|---|
| Seg 28/09 | 2 | Consultas do `audit`, documentação, tag `arq-etapa-1` |
| ~~Ter 29 – Qua 30/09~~ Seg 28/09 ✅ | 4, 5 e 9 (metade) | `audit-service`: esqueleto, `POST`/`GET`, banco próprio — adiantado. De carona, a aplicação principal saiu da raiz para `permission-service/` (ADR-008 revisado) |
| Ter 29/09 – Qui 01/10 | 6, 7 e 9 (resto) | Feign (gravação e consulta), indisponibilidade, remoção do `audit` do monolito, testes pelo Postman/Swagger, tag `arq-etapa-2` — a folga ganha no `audit-service` vai para cá |
| Sex 02/10 | 8 e 11 | Profiles, variáveis de ambiente, `Dockerfile` e Compose, tag `arq-etapa-3` |
| Sáb 03 – Dom 04/10 | 12 e 13 | RabbitMQ e Batch, tag `arq-etapa-4`; Config Server (dia 10) só se sobrar tempo |
| Seg 05/10 | 14 | Buffer, seção **Uso de IA**, entrega |

O banco próprio do `audit-service` (dia 9) sobe para a criação do serviço: nascer com `audit_db`
custa menos do que migrar depois. **Testes automatizados** das partes novas saem do caminho
crítico — a rubrica não pontua testes JUnit, e as demonstrações que ela pede (Postman/Swagger,
serviço fora do ar, fila com consumidor parado) são manuais. Ficam como trabalho futuro, a começar
pelo teste de integração das consultas do ADR-009.

### Onde parou (atualizado em 30/09)

Retomar pelo **passo 11**. A sequência até a tag `arq-etapa-2`, na ordem:

| Passo | Quem | Entrega |
|---|---|---|
| 6 ✅ | Jairo + Claude | BOM Spring Cloud `2025.1.3` (a última GA para Spring Boot 4.0–4.1, conferida no Initializr e no Maven Central), `spring-cloud-starter-openfeign`, `@EnableFeignClients`, `audit.service.url: ${AUDIT_SERVICE_URL:http://localhost:8081}`. Commit `611aa53` |
| 7 ✅ | Jairo + Claude | `AuditClient` (`@FeignClient(name = "audit-service", url = "${audit.service.url}")`) em `audit/infrastructure/client/`, com `registerPermissionCheck` (POST) e `searchAuditEvents` (GET); DTOs de contrato espelhados em `client/dto/` (`RegisterPermissionCheckRequest`, `AuditEventResponse`, sem Bean Validation). Nomes explícitos em cada `@RequestParam`, `required = false` (nulo omite o filtro) e `from`/`to` como `Instant`, para não mandar `+` na URL |
| 8 ✅ | Jairo + Claude | Gravação: o `AuditLogListener` traduz o evento e chama a porta `AuditTrail` (adapter `AuditTrailClientAdapter` sobre o `AuditClient`), com timeout de 1s/2s no cliente `audit-service` e `try/catch` — com o serviço fora do ar a validação responde normalmente e o evento se perde (limitação que a fila resolve na etapa 4). Testado em 30/09: serviço ligado (evento no `audit_db`), desligado (200 em 0,4s + `WARN`) e travado (200 em 2,5s) |
| 9 ✅ | Jairo + Claude | Consulta: o `GET /audit-events` da aplicação principal vira repasse ao serviço pela porta `AuditTrail` (modelo de leitura `AuditTrailEntry`); período invertido e filtro inválido seguem `400` locais; serviço fora do ar responde `503` (`ServiceUnavailableException` no `shared`). Testado em 30/09: 8080 devolve o mesmo que a 8081, filtros `projectId`/`onlyDenied`/`type` minúsculo/`from` com fuso `+02:00`, os dois `400` e o `503` |
| 10 ✅ | Claude | Persistência do `audit` apagada do monolito (entidades, repositórios, adapter, portas antigas, `AuditDemoRunner`, file writer, `ProjectLifecycleEvent`) e migration `V10` apagando `audit_events` (ADR-010) — fecha o dia 9. Testado em 30/09: `clean verify` (22 + 9 testes) e as 10 migrations num PostgreSQL vazio. **Aguardando revisão do Jairo antes do commit** |
| **11** | juntos | Testes da comunicação no Postman (serviço isolado, pela aplicação principal, serviço fora do ar), reflexão da etapa 2 no `README.md` (Strangler Fig) e tag `arq-etapa-2` |

Pendências menores fora da sequência: o `docker-compose.yml` ainda não repassa `AUDIT_SERVICE_URL`
ao container da aplicação principal (entra com o `audit-service` no Compose, dia 11).

### Ordem de corte, se atrasar

Cortar de baixo para cima, nunca as tags:

1. **Filtros do `GET /audit-events`** no serviço novo — entregar a consulta sem query params. Não
   há item de rubrica sobre filtros, e o endpoint continua provando a comunicação Feign.
2. **Config Server** (dia 10) — **cai por último**: não tem item de rubrica próprio (o item 9 é
   coberto por Profiles + variáveis de ambiente), mas é **exigência escrita do enunciado da
   Etapa 3** (`ETAPA3.md`, "Configuração centralizada"). Só cortar se a Etapa 4 estiver em risco;
   nesse caso vira "trabalho futuro" documentado e a reflexão da Etapa 3 responde o *porquê* da
   centralização mesmo sem o serviço no ar.

Se a Etapa 4 não fechar até 04/10, o dia 05/10 deixa de ser buffer e vira dia de execução, com a
postagem no Moodle na última hora.

---

## Reaproveitamento (não reinventar)

- `audit/domain/` e `audit/infrastructure/` — vão quase inteiros para o `audit-service`; o trabalho
  é de recorte e contrato, não de modelagem nova.
- `shared/api/GlobalExceptionHandler.java` — copiar para o serviço novo mantém o mesmo mapeamento
  de status HTTP nos dois lados.
- `shared/domain/Mapper.java` — mesma interface para os mappers do serviço extraído.
- `permission/domain/event/PermissionValidatedEvent` — já é o contrato do que vai para a fila na
  Etapa 4; a carga da mensagem sai dele.
- `permission-service/Dockerfile` — modelo para os `Dockerfile` dos projetos novos.
- `audit/api/` — o controller atual é o molde do `GET /audit-events` que vira proxy Feign
  (Decisão 5) e do controller equivalente dentro do `audit-service`.

---

## Não incluído

- **Service discovery (Eureka) e API Gateway.** Não há item de rubrica; o Compose resolve os nomes
  dos serviços na rede interna, que é o suficiente para a solução desta entrega.
- **Resiliência avançada (Resilience4j, circuit breaker, retry).** A própria Etapa 2 dispensa
  ("não será obrigatório utilizar mecanismos avançados de resiliência"); o tratamento será o
  `try/catch` do Feign com resposta de indisponibilidade.
- **Quebrar `billing`, `identity` ou `project` em serviços.** A Etapa 2 pede uma única
  responsabilidade extraída, e nenhum dos três tem o desacoplamento que o `audit` já tem.
- **Aplicação consumidora externa (`permission-client-demo`).** Estava prevista como demonstração
  do SaaS pelo lado de quem o compra, e foi **cortada do escopo em 23/09/2026**: nenhum item de
  rubrica depende dela (o item 7 pede a *aplicação principal* consumindo o serviço extraído), a
  Etapa 2 aceita Postman/Swagger UI como "Testes da comunicação" (item 10) e o enunciado
  desaconselha múltiplas extrações ("Será suficiente extrair uma única funcionalidade"). Um
  terceiro projeto custaria o dia 7 inteiro sem pontuar. Fica registrada como **trabalho futuro**
  no `README.md`.
- **Spring Security / JWT real e front-end.** Continuam fora, como nas disciplinas anteriores.
- **Migração dos eventos de auditoria já gravados** para o banco do serviço novo. O histórico local
  não é evidência de nada avaliado; o serviço começa com a tabela vazia.

---

## Citação de IA (exigida pela política 🟢)

Registrar no `README.md`, antes da entrega:

- **Ferramenta:** Claude Code (modelo Opus 5), usado como par de programação e revisor.
- **Onde foi usada:** planejamento, divisão do enunciado, configuração de Feign/RabbitMQ/Batch,
  `Dockerfile` e Compose, documentação e revisão de código.
- **O que foi verificado pelo aluno:** toda a solução roda localmente via
  `docker compose up --build`, com os testes do repositório passando — a checagem descrita em
  "Verificação".

---

## Verificação

Ao final de cada dia, nos projetos tocados:

```bash
./mvnw test                       # inclui verifiesModularStructure()
./mvnw clean package -DskipTests
```

A partir da Etapa 2:

```bash
docker compose up --build -d
curl http://localhost:8080/ping
curl http://localhost:8081/audit-events                     # serviço novo, isolado
curl -X POST http://localhost:8080/validate-permission ...  # deve gerar evento no serviço novo
docker compose stop audit-service                           # a validação deve continuar respondendo
```

A partir da Etapa 4:

```bash
docker compose stop audit-service    # publicar eventos com o consumidor parado
# conferir a fila audit.events no painel do RabbitMQ (15672)
docker compose start audit-service   # as mensagens devem ser consumidas ao religar
curl -X POST http://localhost:8080/routes/import -F 'file=@routes.csv'
```

Antes da tag final:

```bash
git tag -l    # deve listar as quatro tags desta disciplina, sem tocar em etapa-1..etapa-4
```
