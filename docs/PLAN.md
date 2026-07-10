# 📋 Planejamento — Projeto Clean Code & Padrões de Projeto

**Aluno:** Jairo Williams Guedes Lopes Neto
**Disciplina:** Clean Code e Padrões de Projeto
**Domínio:** SaaS de Gerenciamento de Permissões por Projeto
**Prazo:** 27/06/2026 → 05/07/2026 (9 dias)
**Disponibilidade:** ~1h/dia
**Stack:** Java 21 + Spring Boot + PostgreSQL + Docker (Arquitetura Modular)

---

## ⏱️ Premissa principal: escopo reduzido (vertical slice)

Você tem ~8h de codificação reais. O sistema completo (10 sprints) é grande demais.
A estratégia é implementar **uma fatia vertical** do sistema — pequena, mas que
atravesse várias camadas e módulos, e que **roda de ponta a ponta**.

### ✅ Entra no projeto acadêmico

- Cadastro de Cliente (US01, simplificado)
- Escolha/assinatura de Plano com geração de API Key (US02–US04, pagamento **simulado**)
- **Middleware de validação de permissões** (EP04) — é o coração da demo, roda isolado

### ❌ Fica de fora (citar como "trabalho futuro" no relatório)

**Decisão de escopo tomada no dia da entrega (05/07), com ~1h e meia de tempo real restante — ver Dia 5/6 abaixo:**

- Criação de Projeto/Cargo/Rota respeitando limite do plano (US06–US08) — módulo `project`, Builder e `PlanLimitValidator` não implementados
- Log de auditoria (EP05) — módulo `audit`, Observer não implementado
- Gateway de pagamento real
- Autenticação/JWT completos, login do painel
- Exportação CSV/JSON
- Front-end (tudo via REST + Postman/curl)

O middleware de permissão (`permission`) foi implementado e testado ponta a ponta mesmo sem `project`: a validação de ApiKey é real (contra `billing`), e os handlers de Token/Role-Rota existem na chain (provando OCP) mas sempre concedem, já que dependem exatamente do que ficou de fora — ver `docs/PATTERNS.md` e `docs/DOMAIN.md`.

Isso é importante para você escrever no relatório: delimita claramente **o que é
interno** (módulos acima) **e externo** (gateway de pagamento, sistema do cliente que
consome a API) à aplicação — que é justamente um dos pontos pedidos na descrição do
projeto.

---

## 🏗️ Arquitetura modular proposta (Modular Monolith)

Um único projeto Spring Boot, mas organizado por **módulos de domínio** (bounded
contexts), cada um com suas próprias camadas internas. Mais rápido de montar que
multi-módulo Maven, e já demonstra modularidade e separação de responsabilidades.

```
src/main/java/com/saas/permissions/
├── shared/
│   ├── domain/            (Entity base, DomainException, ValueObject)
│   └── infrastructure/    (configs gerais)
│
└── modules/                        → todos os módulos de negócio agrupados aqui
    ├── identity/                       → Cliente
    │   ├── domain/      Client.java, ClientRepository.java (porta)
    │   ├── application/ RegisterClientUseCase.java
    │   ├── infrastructure/ JpaClientRepository.java
    │   └── api/         ClientController.java
    │
    ├── billing/                        → Plano / Assinatura / API Key
    │   ├── domain/      Plan.java, Subscription.java, ApiKey.java,
    │   │                PaymentGateway.java (porta)
    │   ├── application/ SubscribeToPlanUseCase.java
    │   ├── infrastructure/ FakePaymentGatewayAdapter.java, ApiKeyFactory.java
    │   └── api/         SubscriptionController.java
    │
    ├── project/                        → Projeto / Cargo / Rota
    │   ├── domain/      Project.java, Role.java, Route.java,
    │   │                ProjectBuilder.java, PlanLimitValidator.java
    │   ├── application/ CreateProjectUseCase.java
    │   └── api/         ProjectController.java
    │
    ├── permission/                     → 🔑 Middleware de validação (núcleo)
    │   ├── domain/      PermissionValidationHandler.java (porta),
    │   │                ApiKeyValidationHandler, TokenValidationHandler,
    │   │                RoleRouteValidationHandler
    │   ├── application/ ValidatePermissionUseCase.java
    │   └── api/         PermissionController.java
    │
    └── audit/                          → Log de auditoria
        ├── domain/      AuditLog.java, PermissionEventListener.java (porta)
        ├── application/ AuditLogListener.java
        └── infrastructure/ JpaAuditLogRepository.java
```

Cada módulo segue o mesmo padrão: `domain` (entidades + interfaces/portas),
`application` (casos de uso), `infrastructure` (implementações JPA/adapters),
`api` (controllers REST). Isso já é, por si só, uma boa demonstração de
arquitetura em camadas para a Parte 3 da rubrica.

---

## 🧩 Mapeamento dos 5 princípios SOLID

A boa notícia: com esse desenho, **os 5 princípios saem quase "de graça"** —
você só precisa saber apontá-los no relatório.

| Princípio    | Onde aplicar                                                                                                               | Por quê                                 |
| ------------- | -------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------- |
| **SRP** | Cada`UseCase` faz uma coisa só (ex: `RegisterClientUseCase` só cadastra; não envia e-mail, não audita)             | Uma única razão para mudar             |
| **OCP** | `PermissionValidationHandler` (chain) e `PaymentGateway` (interface) — novo handler/gateway sem alterar os existentes | Extensão sem modificação              |
| **LSP** | Qualquer handler da chain ou listener de auditoria pode substituir outro do mesmo tipo sem quebrar o fluxo                 | Substituibilidade garantida              |
| **ISP** | Interfaces pequenas:`PaymentGateway` só tem `process()`, `PermissionEventListener` só tem `onValidated()`        | Ninguém implementa método que não usa |
| **DIP** | `UseCases` dependem de interfaces (`ClientRepository`, `PaymentGateway`), nunca de classes JPA concretas             | Domínio não conhece infraestrutura     |

A instrução pede "2 ou mais", mas a rubrica avalia os 5 — vale a pena documentar
todos, já que o desenho já cobre isso.

---

## 🎨 Mapeamento dos Padrões GoF (mínimo 1 criacional + 1 estrutural + 1 comportamental)

| Padrão                                           | Categoria      | Onde           | Status                    | Objetivo                                                                                                                             |
| ------------------------------------------------- | -------------- | -------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **Factory Method** (`ApiKeyFactory`)      | Criacional     | `billing`    | ✅ Implementado           | Centraliza a criação da chave de API; permite trocar a estratégia de geração (UUID hoje, JWT no futuro) sem afetar quem consome |
| **Adapter** (`FakePaymentGatewayAdapter`) | Estrutural     | `billing`    | ✅ Implementado           | Adapta o formato de um gateway de pagamento externo (simulado) para a interface interna`PaymentGateway`                            |
| **Chain of Responsibility**                 | Comportamental | `permission` | ✅ Implementado           | Cada handler valida sua parte (ApiKey → Token → Role/Rota) e passa adiante — adicionar nova regra não toca nas existentes        |
| ~~**Builder** (`ProjectBuilder`)~~       | Criacional     | `project`    | ❌ Fora de escopo (05/07) | Montaria um`Project` complexo (com Roles e Routes) passo a passo, evitando construtor telescópico                                 |
| ~~**Observer** (`AuditLogListener`)~~    | Comportamental | `audit`      | ❌ Fora de escopo (05/07) | Desacoplaria "o que acontece na validação" de "quem precisa saber disso" (auditoria)                                               |

**1 criacional + 1 estrutural + 1 comportamental implementados** — atende exatamente
a exigência mínima da rubrica. Builder e Observer ficaram de fora por falta de tempo
no dia da entrega (ver Dia 5/6 abaixo) — cite-os no relatório como "trabalho futuro",
já desenhados na arquitetura mas não codificados.

---

## 💡 Dica de ouro para a rubrica: mostre o "antes e depois"

A rubrica pergunta explicitamente se você **identificou bad smells** e **aplicou
refatoração em código já existente** — não basta escrever código limpo direto.

**Sugestão:** escolha o `SubscribeToPlanUseCase` (tem pagamento + geração de chave +
ativação, é rico o suficiente) e no relatório mostre:

1. **Versão "ruim" (antes):** um método único, grande, com `if/else` aninhado,
   validando pagamento, gerando chave, salvando tudo e fazendo log — tudo junto,
   nomes genéricos (`process()`, `data`, `flag`).
2. **Versão refatorada (depois):** o mesmo fluxo dividido entre o `UseCase`, o
   `PaymentGateway` (Adapter) e o `ApiKeyFactory` (Factory), aplicando SRP e OCP.

Isso resolve de uma vez 3-4 itens da rubrica que um código limpo "direto" não cobre.

---

## 📅 Cronograma dia a dia

> Estratégia: peça ao **Claude Code** para gerar o esqueleto de cada módulo com um
> prompt bem específico (sugestões abaixo), e use seu 1h para **revisar, entender,
> ajustar nomes e testar** — não para digitar tudo do zero.

### Dia 1 — Sáb 27/06 — Setup do ambiente

- [X] Criar repositório Git
- [X] Gerar projeto Spring Boot (Java 21 / Web, Data JPA, PostgreSQL Driver, Validation)
- [X] Criar a estrutura de pacotes modular (acima)
- [X] Criar `docker-compose.yml` com Postgres (modelo abaixo)
- [X] Validar: app sobe e conecta no banco (endpoint simples `GET /ping`)

- *(fora da 1h, quando puder)*: adaptar o texto do "Estudo de Caso" que você já tem para a seção de Descrição do Projeto

**Prompt Claude Code:** *"Crie um projeto Spring Boot 3 com Java 21, dependências
web/data-jpa/postgresql/validation, com a estrutura de pacotes [cole a árvore acima],
um endpoint GET /ping em um controller no pacote shared, e um docker-compose com
Postgres 16."*

### Dia 2 — Dom 28/06 — Módulo Identity (Cliente)

- [X] Entidade `Client` (domain) com validações básicas
- [X] Interface `ClientRepository` (porta) + `JpaClientRepository` (adapter)
- [X] `RegisterClientUseCase` (SRP)
- [X] Endpoint `POST /clients` testado via curl/Postman

### Dia 3 — Seg 29/06 — Módulo Billing: Factory + Adapter

- [X] Entidades `Plan`, `Subscription`, `ApiKey` (+ migrations `V2`–`V4`, não executadas)
- [X] Interface `PaymentGateway` + `FakePaymentGatewayAdapter` (Adapter)
- [X] `ApiKeyFactory` (Factory Method)
- [X] `SubscribeToPlanUseCase` orquestrando os três

### Dia 4 — Ter 30/06 — Módulo Project: Builder — **não executado, cortado do escopo em 05/07**

- [ ] ~~Entidades `Project`, `Role`, `Route`~~
- [ ] ~~`ProjectBuilder` (Builder)~~
- [ ] ~~`PlanLimitValidator` (respeita `max_projects` do plano — bom exemplo de OCP)~~
- [ ] ~~`CreateProjectUseCase`~~

Sem esse dia, o Builder não entra na entrega — o mínimo da rubrica (1 criacional) já
está coberto por `Factory Method`. Citar como trabalho futuro no relatório.

### Dia 5 — Dom 05/07 (reagendado) — 🔑 Middleware de validação: Chain of Responsibility

- [X] Interface `PermissionValidationHandler` (Handler abstrato, `permission/domain/`)
- [X] 3 handlers concretos encadeados: `ApiKeyValidationHandler` → `TokenValidationHandler` → `RoleRouteValidationHandler`
- [X] Endpoint `POST /validate-permission`
- [X] `ApiKeyValidationHandler` valida de verdade contra `billing` (novo `FindApiKeyByPlainKeyUseCase` + `ApiKeyRepository.findAllActive()`); testado ponta a ponta via curl com uma ApiKey real gerada por `POST /subscriptions`
- [X] **Este é o módulo mais importante para o professor testar isoladamente** — roda sozinho, sem precisar de `project`

**⚠️ Decisão de escopo (dia da entrega, 05/07):** por falta de tempo, `TokenValidationHandler` e `RoleRouteValidationHandler` ficaram como handlers estruturalmente completos mas que sempre concedem (`PermissionCheckResult.allow()`), documentado em Javadoc e em `docs/PATTERNS.md`/`docs/DOMAIN.md`. Motivo: a regra real de Role/Rota depende do módulo `project` (Dia 4, não feito), e "Token" seria um 2º fator de auth fora do escopo do `CLAUDE.md`. Isso não compromete o requisito da rubrica (mínimo 1 padrão comportamental) — o Chain of Responsibility está implementado, testável e demonstra OCP de verdade (um handler novo se pluga sem tocar nos existentes).

### Dia 6 — Qui 02/07 (não executado) — Auditoria (Observer) + Docker ponta a ponta

- [ ] ~~Interface `PermissionEventListener` + `AuditLogListener` (Observer)~~ — **fora de escopo desta entrega**, citar como trabalho futuro no relatório
- [ ] ~~Notificação disparada após cada validação de permissão~~ — idem
- [X] `Dockerfile` multi-stage já existente e validado (`docker compose up -d --build app` sobe a aplicação com o código atual)
- [X] Validado `docker compose up`: app + Postgres + `POST /clients/register`, `POST /subscriptions` e `POST /validate-permission` respondendo de ponta a ponta

**Por que Observer ficou de fora:** a rubrica exige só **1** padrão comportamental (já atendido pelo Chain of Responsibility) — o projeto já tem `Factory Method` (criacional) + `Adapter` (estrutural) + `Chain of Responsibility` (comportamental), passando da exigência mínima. Escrever `audit`/Observer sem tempo para testar corretamente seria pior do que documentar a decisão de cortar escopo — ver seção "❌ Fica de fora" no topo deste arquivo, agora também incluindo `project` e `audit`.

### Dia 7 — Sex 03/07 (reagendado para 05/07) — Documentação SOLID + Padrões

- [X] Para cada princípio SOLID: destacar trecho, nome, objetivo, explicação → `docs/RELATORIO.md`
- [X] Para cada padrão GoF: destacar trecho, nome, objetivo, explicação → `docs/RELATORIO.md`
- [X] Escrever a seção "antes/depois" — usada a duplicação real encontrada e corrigida em `ClientRepositoryAdapter.toDomain()` (mais forte que um exemplo fabricado) → `docs/RELATORIO.md`
- [X] Escrever os parágrafos sobre **importância** do Clean Code e dos Patterns no ciclo de vida do projeto → `docs/RELATORIO.md`
- [ ] Revisar `docs/RELATORIO.md` com suas palavras, copiar para Word/Docs, aplicar negrito/marca-texto nos trechos indicados por "🔶 Destacar" e exportar PDF

### Dia 8 — Sáb 04/07 — Polimento Clean Code

- [ ] Revisar nomes (vocabulário do domínio), tamanho de função, máx. 2 parâmetros/função
- [ ] Remover comentários desnecessários, padronizar formatação/indentação
- [ ] Atualizar README com instruções de `docker compose up`

### Dia 9 — Dom 05/07 — Entrega final

- [X] Dias 5 e 6 (Chain of Responsibility) executados hoje, com escopo reduzido — ver decisão acima
- [X] Consolidar PDF: Descrição do Projeto + Parte 1 (Clean Code) + Parte 2 (SOLID) + Parte 3 (GoF) + anexo/link do código
- [X] Nomear arquivo: `JairoWilliamsGuedesLopesNeto_CleanCodeEPadroesDeProjeto_pd.pdf`
- [X] Checar a checklist da rubrica abaixo
- [X] Postar no Moodle

---

## 🐳 Starter de Docker (Dia 1)

**docker-compose.yml**

```yaml
version: "3.9"
services:
   postgres:
      image: postgres:16
      environment:
         POSTGRES_DB: permissions_saas
         POSTGRES_USER: saas
         POSTGRES_PASSWORD: saas123
      ports:
         - "5432:5432"
      volumes:
         - pgdata:/var/lib/postgresql/data

   app:
      build: ..
      depends_on:
         - postgres
      environment:
         SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/permissions_saas
         SPRING_DATASOURCE_USERNAME: saas
         SPRING_DATASOURCE_PASSWORD: saas123
      ports:
         - "8080:8080"

volumes:
   pgdata:
```

**Dockerfile (multi-stage)**

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## ✅ Checklist final mapeado à rubrica

| Item da rubrica                                          | Onde você atende                                                                                  |
| -------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Pilares de OO / encapsulamento                           | Entidades ricas (não anêmicas) em`domain`                                                      |
| Identificar classes não coesas / refatorar              | Seção "antes/depois" do`SubscribeToPlanUseCase`                                                |
| 5 princípios SOLID aplicados                            | Tabela de mapeamento SOLID acima                                                                   |
| Importância do Clean Code no ciclo de vida              | Parágrafo introdutório da Parte 1                                                                |
| Bad smells e pilares do Refactoring                      | Seção "antes/depois"                                                                             |
| Importância dos Design Patterns                         | Parágrafo introdutório da Parte 3                                                                |
| 1 criacional + 1 estrutural + 1 comportamental           | Factory Method + Adapter + Chain of Responsibility (Builder/Observer citados como trabalho futuro) |
| Código adaptável (novas classes sem afetar existentes) | OCP via interfaces (handlers, gateway)                                                             |
| Arquitetura em camadas                                   | Estrutura modular`domain/application/infrastructure/api`                                         |
| Integração com APIs de terceiros                       | `PaymentGateway` (Adapter) simulando gateway externo                                             |

---

## 🤖 Usando o Claude Code no dia a dia

Para cada dia, um prompt **específico e pequeno** funciona melhor que pedir o
sistema inteiro de uma vez. Padrão sugerido:

> "Estou seguindo este plano: [cole a seção do dia]. Gere [a classe/módulo
> específico], seguindo a estrutura de pacotes [cole a árvore], aplicando [o
> princípio/padrão do dia], com nomes do domínio (Client, Plan, Subscription...).
> Não gere nada além do que foi pedido hoje."

Isso evita receber um projeto gigante de uma vez que você não teria tempo de
entender — e o entendimento é o que vale na entrega.
