
### **Uso de IAs: Sinal Amarelo 🟡**

Neste trabalho, os alunos têm permissão para usar ferramentas baseadas em IA nas seguintes tarefas: **esclarecimento de dúvidas, apoio na configuração de frameworks, depuração, documentação e melhoria da qualidade do código. A modelagem, a implementação das camadas da aplicação, das APIs, da persistência de dados e das demais funcionalidades devem ser desenvolvidas e compreendidas pelo aluno**. Todas as fontes, incluindo ferramentas de IA, devem ser devidamente citadas. O uso de IA de forma inconsistente com os parâmetros acima será considerado má conduta acadêmica e estará sujeito à aplicação do código disciplinar. Observe que os resultados da IA podem ser tendenciosos e imprecisos. É responsabilidade do aluno garantir que as informações usadas da IA sejam precisas. Aprender como usar ferramentas baseadas em IA de maneira cuidadosa e estratégica contribui para o desenvolvimento das habilidades, refinamento de seu trabalho e prepara o aluno para sua futura carreira.

---

Ao longo da disciplina, você desenvolverá uma única aplicação Java que evoluirá progressivamente a cada etapa. O objetivo é acompanhar a evolução de uma aplicação moderna: começaremos pela modelagem orientada a objetos e pela manipulação de dados em memória; em seguida, organizaremos as regras da aplicação em serviços, disponibilizaremos suas funcionalidades através de uma API REST com Spring Boot e, por fim, utilizaremos Spring Data JPA para realizar a persistência dos dados em banco de dados. O mesmo projeto deverá ser utilizado durante toda a disciplina. Cada etapa representa uma evolução da implementação realizada anteriormente. O domínio da aplicação é de livre escolha. Alguns exemplos são: sistema acadêmico, biblioteca, loja, clínica, eventos, cursos, transportes, jogos ou serviços.

---

### **Evolução e Entrega do Projeto**

O projeto terá uma única entrega ao final da disciplina, realizada através de um repositório Git. Apesar de existir apenas uma entrega, o desenvolvimento será dividido em quatro etapas, correspondentes às quatro competências trabalhadas na disciplina. Como o projeto evoluirá durante as aulas, algumas implementações poderão ser modificadas ou substituídas nas etapas seguintes. Por exemplo, o armazenamento em memória utilizando Map será posteriormente substituído pela persistência utilizando Spring Data JPA. Por esse motivo, o aluno deverá preservar no histórico do repositório um marco correspondente à conclusão de cada etapa:

* etapa-1: Orientação a Objetos;
* etapa-2: Estruturas de Dados e Serviços;
* etapa-3: API REST com Spring Boot;
* etapa-4: Persistência com Spring Data JPA.

Esses marcos deverão ser registrados preferencialmente utilizando tags do Git. A versão final do projeto deverá representar a Etapa 4, enquanto os marcos anteriores serão utilizados como evidências da evolução da solução e para avaliação das competências correspondentes. Não é necessário manter implementações antigas artificialmente na versão final. O projeto deve evoluir normalmente, utilizando o controle de versão para preservar seu histórico.

---



Desafio Adicional — Integração com API Externa
Como evolução opcional, integrar a aplicação com uma API externa. A integração poderá utilizar OpenFeign ou outra solução apresentada durante a disciplina. A API escolhida deverá complementar alguma funcionalidade do domínio desenvolvido. Exemplos: consulta de endereço; cotação; informações geográficas; dados meteorológicos; informações provenientes de outro serviço. O objetivo é demonstrar que uma aplicação Spring Boot pode atuar tanto como provedora de uma API quanto como consumidora de outros serviços. O desafio adicional não substitui os requisitos obrigatórios das quatro etapas.

Entrega Final
Será realizada uma única entrega ao final da disciplina. O repositório entregue deverá conter: código-fonte completo do projeto; instruções necessárias para executar a aplicação; configuração necessária para acesso ao banco de dados; documentação da API; coleção de requisições utilizada nos testes, quando aplicável; tags etapa-1, etapa-2, etapa-3 e etapa-4. As quatro etapas não representam quatro projetos diferentes. Elas representam quatro momentos da evolução da mesma aplicação. Durante a avaliação, cada marco será utilizado como evidência da competência correspondente:

Etapa 1 → Modelo Orientado a Objetos

Etapa 2 → Collections + Service + armazenamento em memória

Etapa 3 → Spring Boot + API REST

Etapa 4 → Spring Data JPA + Banco de Dados

A evolução e substituição de implementações anteriores fazem parte do projeto e são esperadas.

Expectativas e Critérios de Avaliação
Qualidade do Código: Clareza, organização e uso adequado dos conceitos aprendidos.
Funcionalidade: Implementação correta e completa das features solicitadas.
Documentação: Comentários no código e documentação das funcionalidades desenvolvidas.
Entrega no Prazo: Cumprimento dos prazos estabelecidos para cada etapa.
Assim que terminar, salve o seu arquivo PDF e poste no Moodle. Utilize o seu nome para nomear o arquivo, identificando também a disciplina no seguinte formato: “nomedoaluno_nomedadisciplina_pd.PDF”.

Status da entrega
Número da tentativa	Esta é a tentativa 1 (2 tentativas permitidas).
Status da entrega	Nenhuma tentativa
Status da avaliação	Não avaliado
Data de entrega	segunda, 24 ago 2026, 23:59
Tempo restante	14 dias 3 horas
Rubrica
Template de Rubrica para ser utilizado com a extensão Rubricator

1. Desenvolver aplicações Java utilizando conceitos avançados de orientação a objetos
   O aluno criou um projeto utilizando o framework Spring Boot?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
2. Desenvolver aplicações Java utilizando conceitos avançados de orientação a objetos
   O aluno criou pelo menos quatro classes e as relacionou com oneToMany e extends?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
3. Desenvolver aplicações Java utilizando conceitos avançados de orientação a objetos
   O aluno criou a quantidade certa de atributos com seus respectivos tipos?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
4. Desenvolver aplicações Java utilizando conceitos avançados de orientação a objetos
   O aluno criou as classes para fazer a leitura dos arquivos textos e popular os objetos?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
5. Implementar estruturas de dados e manipular coleções em Java
   O aluno atualizou os arquivos texto e as classes loader para contemplar o relacionamento oneToMany?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
6. Implementar estruturas de dados e manipular coleções em Java
   O aluno atualizou o método toString para apresentar as informações do relacionamento oneToMany?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
7. Implementar estruturas de dados e manipular coleções em Java
   O aluno criou um Map para simular uma base de dados e e guardar as informações do arquivo texto?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
8. Implementar estruturas de dados e manipular coleções em Java
   O aluno criou as classes de serviço para gerir os Maps através das operações de inclusão e de recuperação?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
9. Desenvolver e configurar aplicações utilizando o framework Spring Boot
   O aluno criou os Endpoints RESTful para cada contexto de negócio do projeto?
   Não demonstrou o item de rubrica
   Demonstrou o item de rubrica
10. Desenvolver e configurar aplicações utilizando o framework Spring Boot
    O aluno testou e documentou os endpoints RESTful criados?
    Não demonstrou o item de rubrica
    Demonstrou o item de rubrica
11. Desenvolver e configurar aplicações utilizando o framework Spring Boot
    A aluno criou um projeto front-end para se comunicar com a API através dos endpoints definidos?
    Não demonstrou o item de rubrica
    Demonstrou o item de rubrica
12. Desenvolver e configurar aplicações utilizando o framework Spring Boot
    O aluno conseguiu apresentar os dados obtidos da API no front-end?
    Não demonstrou o item de rubrica
    Demonstrou o item de rubrica
13. Implementar APIs REST e camada de persistência com Spring Data
    O aluno mapeou as classes de negócio como entidades JPA?
    Não demonstrou o item de rubrica
    Demonstrou o item de rubrica
14. Implementar APIs REST e camada de persistência com Spring Data
    O aluno criou a camada repository para cada entidade?
    Não demonstrou o item de rubrica
    Demonstrou o item de rubrica
15. Implementar APIs REST e camada de persistência com Spring Data
    O aluno atualizou a camada de serviço para injetar os repository?
    Não demonstrou o item de rubrica
    Demonstrou o item de rubrica
16. Implementar APIs REST e camada de persistência com Spring Data
    O aluno integrou APIs externas utilizando OpenFeign?
    Não demonstrou o item de rubrica
    Demonstrou o item de rubrica
