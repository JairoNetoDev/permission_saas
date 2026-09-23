
Etapa 1: **Organização Arquitetural da Aplicação**

**Competência avaliada**

*Implementar arquiteturas de microsserviços, compreendendo inicialmente a organização interna de uma aplicação e a importância da separação adequada de responsabilidades antes da distribuição dos serviços.*

**Objetivo**

*Revisar e organizar a aplicação construída anteriormente, garantindo uma estrutura clara, coesa e preparada para futuras evoluções arquiteturais.*

Nesta etapa, o foco ainda não estará na criação de microsserviços.

Antes de distribuir uma aplicação, é importante compreender suas responsabilidades, identificar seus módulos e garantir que as diferentes partes do sistema estejam corretamente organizadas.

Ao final desta etapa, o aluno deverá ser capaz de observar sua aplicação não apenas como um conjunto de classes, mas como uma solução formada por diferentes responsabilidades de negócio.

**Feature a ser desenvolvida**

1. **Revisão da estrutura da aplicação**

   Utilizar o projeto desenvolvido na disciplina anterior ou uma aplicação Spring Boot equivalente.

   A aplicação deverá possuir, no mínimo, a seguinte estrutura funcional:

   ```
   Cliente HTTP → Controller → Service → Repository → Banco de Dados
   ```

   Revisar essa organização, garantindo que:

   - controllers sejam responsáveis pela comunicação HTTP;
   - services concentrem as regras e operações da aplicação;
   - repositories sejam responsáveis pelo acesso aos dados;
   - controllers não realizem acesso direto aos repositories;
   - regras de negócio não estejam concentradas nos controllers.

2. **Organização por domínio ou funcionalidade**

   Reorganizar os pacotes da aplicação priorizando as funcionalidades ou áreas do domínio.

   Em vez de uma estrutura exclusivamente técnica como:

   ```
   controller
   service
   repository
   model
   ```

   o projeto deverá buscar uma organização semelhante a:

   ```
   aluno
   turma
   projeto
   comunicação
   ```

   Cada funcionalidade poderá possuir internamente seus próprios controllers, services, repositories e demais classes necessárias. A organização deverá refletir o domínio escolhido pelo aluno. Não é obrigatório utilizar exatamente essa estrutura ou esses nomes. O importante é que seja possível identificar claramente as principais responsabilidades da aplicação.

3. **Validação dos dados**

   Revisar ou implementar a validação dos dados recebidos pela API utilizando Bean Validation. Utilizar anotações adequadas às regras do domínio, como:

   - `@NotNull`;
   - `@NotBlank`;
   - `@Size`;
   - `@Min`;
   - `@Max`;
   - `@Email`;
   - ou outras que façam sentido para a aplicação.

   As operações que recebem dados através da API deverão validar as informações antes da execução das regras de negócio. Requisições contendo dados inválidos deverão produzir respostas HTTP adequadas.

4. **Tratamento de exceções**

   Implementar ou revisar o tratamento de situações excepcionais da aplicação.

   Devem ser contempladas situações como:

   - tentativa de consultar um objeto inexistente;
   - tentativa de excluir um objeto inexistente;
   - dados inválidos;
   - operações que não possam ser realizadas devido às regras do domínio.

   O tratamento deverá evitar que detalhes internos da aplicação sejam retornados diretamente ao cliente.

   Sempre que possível, centralizar o tratamento das exceções utilizando os recursos disponibilizados pelo Spring.

5. **Consultas utilizando Spring Data**

   Criar ou revisar consultas relacionadas ao domínio utilizando os recursos do Spring Data JPA.

   Além das operações básicas fornecidas por `JpaRepository`, implementar pelo menos duas consultas relacionadas às necessidades da aplicação.

   Exemplos:

   - buscar por nome;
   - buscar por status;
   - buscar por período;
   - buscar registros associados a outra entidade;
   - filtrar por alguma característica do domínio;
   - retornar resultados ordenados.

   As consultas deverão representar necessidades coerentes com a aplicação escolhida.

6. **Documentação da API**

   Documentar os principais endpoints da aplicação utilizando OpenAPI/Swagger.

   A documentação deverá permitir identificar:

   - os principais recursos da aplicação;
   - os endpoints disponíveis;
   - os métodos HTTP utilizados;
   - os parâmetros necessários;
   - as estruturas recebidas e retornadas.

   O objetivo não é produzir uma documentação extensa, mas garantir que outra pessoa consiga compreender como utilizar a API.

7. **Identificação dos módulos da aplicação**

   Analisar o domínio e identificar pelo menos três responsabilidades ou módulos existentes na aplicação.

   Exemplo:

   > Sistema Acadêmico
   >
   > - Alunos;
   > - Turmas;
   > - Projetos;
   > - Comunicação.

   No arquivo `README.md`, apresentar brevemente esses módulos e explicar a responsabilidade de cada um.

   Exemplo:

   > Aluno — responsável pelo cadastro e manutenção das informações dos alunos.
   >
   > Turma — responsável pela organização das turmas e pelo relacionamento entre alunos e disciplinas.
   >
   > Comunicação — responsável pelas funcionalidades relacionadas ao envio de notificações.

8. **Análise de dependências**

   Observar como os módulos identificados se relacionam.

   Registrar no `README.md` pelo menos um exemplo de dependência existente entre duas partes da aplicação.

   Exemplo:

   > Turma → Aluno
   >
   > Uma turma precisa consultar informações dos alunos associados a ela.

   O objetivo desta atividade é começar a perceber o acoplamento existente entre diferentes responsabilidades da aplicação.

9. **Candidato a serviço independente**

   Escolher uma funcionalidade da aplicação que, futuramente, poderia ser separada e executada como um serviço independente.

   Exemplos:

   - envio de notificações;
   - comunicação por e-mail;
   - consulta de endereço;
   - geração de relatórios;
   - processamento de pagamentos;
   - avaliação;
   - consulta a informações externas.

   Neste momento, a funcionalidade ainda não deverá ser separada.

   No `README.md`, registrar:

   - qual funcionalidade foi escolhida;
   - qual é sua responsabilidade;
   - por que ela poderia ser executada separadamente;
   - quais partes da aplicação atualmente dependem dela.

   Essa análise será utilizada como ponto de partida para a próxima etapa.

**Arquitetura esperada ao final da Etapa 1**

Ao final desta etapa, a aplicação continuará sendo uma única aplicação Spring Boot.

Uma representação possível é:

```
Cliente HTTP
↓
Aplicação Spring Boot
├── Módulo A
├── Módulo B
├── Módulo C
└── Módulo D
↓
Banco de Dados
```

Cada módulo deverá apresentar responsabilidades claramente identificáveis.

Não será necessário criar outro projeto Spring Boot ou implementar comunicação entre microsserviços nesta etapa. O objetivo desta primeira etapa é garantir uma base organizada antes de introduzir os desafios de uma arquitetura distribuída.

**Marco da Etapa 1**

Ao concluir esta etapa, registrar no repositório a tag: `etapa-1`.

Esse marco deverá representar a versão organizada da aplicação antes da separação de qualquer funcionalidade em um serviço independente.

A Etapa 1 será utilizada como evidência da capacidade do aluno de identificar responsabilidades, organizar a aplicação e preparar sua arquitetura para futuras evoluções.
