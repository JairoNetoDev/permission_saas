
Etapa 4: **Comunicação Assíncrona e Processamento em Lote**

**Competência avaliada**

*Processar informações em lote com microsserviços, compreendendo diferentes formas de comunicação e processamento em aplicações distribuídas.*

**Objetivo**

*Evoluir a solução desenvolvida nas etapas anteriores, introduzindo duas formas de processamento diferentes da comunicação síncrona através de APIs REST:*

- *comunicação assíncrona através de mensageria;*
- *processamento em lote utilizando Spring Batch.*

Nesta etapa, o objetivo não será construir uma arquitetura complexa orientada a eventos. O foco estará em compreender quando uma operação precisa de uma resposta imediata e quando ela pode ser processada posteriormente, de maneira assíncrona ou em lote. Ao final da etapa, o aluno deverá ser capaz de diferenciar situações adequadas para utilização de:

- API REST;
- mensageria;
- processamento Batch.

**Feature a ser desenvolvida**

1. **Identificação de uma operação assíncrona**

   Escolher uma funcionalidade do domínio que não precise necessariamente ser concluída durante a requisição principal.

   Exemplos:

   - envio de notificação;
   - envio de e-mail;
   - registro de uma atividade;
   - atualização de histórico;
   - processamento de uma solicitação;
   - geração de uma informação que possa ocorrer posteriormente.

   A funcionalidade deverá representar uma necessidade coerente com o projeto.

   No `README.md`, explicar brevemente por que essa operação pode ser executada de maneira assíncrona.

2. **Introdução à mensageria**

   Implementar uma comunicação assíncrona utilizando um broker de mensagens. Durante a disciplina, será utilizada uma solução definida pelo professor, como RabbitMQ ou tecnologia equivalente. A solução deverá possuir:

   - um produtor de mensagens;
   - uma fila;
   - um consumidor.

   Uma representação possível é:

   ```
   Aplicação Principal
   ↓
   Mensagem
   ↓
   Fila
   ↓
   Consumidor
   ```

   O objetivo é permitir que uma aplicação envie uma informação sem precisar aguardar que todo o processamento seja concluído imediatamente.

3. **Produção da mensagem**

   Implementar uma operação que publique uma mensagem relacionada ao domínio da aplicação. A mensagem deverá conter apenas as informações necessárias para que o consumidor realize sua responsabilidade.

   Exemplo:

   ```json
   {
     "tipo": "ALUNO_CADASTRADO",
     "identificador": 10,
     "email": "aluno@email.com"
   }
   ```

   O formato deverá ser adequado ao domínio escolhido. Não será necessário implementar contratos de eventos complexos ou mecanismos avançados de versionamento.

4. **Consumo da mensagem**

   Criar um consumidor responsável por receber e processar as mensagens publicadas.

   O consumidor poderá:

   - registrar a informação;
   - atualizar um dado sob sua responsabilidade;
   - gerar uma notificação;
   - simular o envio de uma comunicação;
   - executar outra operação coerente com o domínio.

   A execução deverá permitir visualizar que o produtor e o consumidor não precisam concluir suas operações no mesmo momento.

5. **Teste de comunicação assíncrona**

   Demonstrar o funcionamento da mensageria através de um cenário completo.

   O teste deverá permitir observar:

   - uma operação sendo realizada na aplicação;
   - uma mensagem sendo publicada;
   - a mensagem chegando à fila;
   - o consumidor processando a mensagem.

   Também deverá ser demonstrado o comportamento quando o consumidor estiver temporariamente indisponível.

   O objetivo é perceber uma diferença importante em relação à comunicação REST: a mensagem poderá permanecer aguardando processamento no broker.

**Processamento em lote com Spring Batch**

1. **Definição de um processamento Batch**

   Criar uma funcionalidade que processe um conjunto de informações.

   Exemplos:

   - importação de alunos através de arquivo CSV;
   - atualização de cadastros;
   - processamento de pagamentos;
   - importação de produtos;
   - geração de registros;
   - atualização de status;
   - tratamento periódico de informações.

   O processamento deverá possuir relação com o domínio do projeto.

2. **Estrutura do Job**

   Implementar um Job utilizando Spring Batch.

   O processamento deverá possuir pelo menos:

   - Job;
   - Step;
   - ItemReader;
   - ItemProcessor;
   - ItemWriter.

   Uma representação possível é:

   ```
   Arquivo ou fonte de dados
   ↓
   ItemReader
   ↓
   ItemProcessor
   ↓
   ItemWriter
   ↓
   Destino
   ```

3. **Leitura dos dados**

   Utilizar uma fonte de dados adequada ao processamento.

   Para simplificar a atividade, poderá ser utilizado um arquivo CSV.

   O arquivo deverá possuir múltiplos registros relacionados ao domínio.

   Exemplo:

   ```csv
   nome,email,dataNascimento
   Ana,ana@email.com,2000-05-10
   Carlos,carlos@email.com,1998-02-15
   ```

   O ItemReader deverá realizar a leitura dos registros.

4. **Processamento das informações**

   Utilizar o ItemProcessor para aplicar alguma regra sobre os dados.

   Exemplos:

   - validar informações;
   - normalizar textos;
   - converter formatos;
   - calcular valores;
   - ignorar dados que não possam ser utilizados;
   - complementar informações.

   O processamento deverá representar uma regra coerente com o domínio.

5. **Gravação dos dados**

   Utilizar o ItemWriter para armazenar ou registrar os dados resultantes do processamento.

   O destino poderá ser:

   - banco de dados;
   - arquivo;
   - outro mecanismo utilizado durante as aulas.

   Quando houver persistência em banco de dados, utilizar a estrutura de persistência pertencente à aplicação responsável pelo processamento.

6. **Processamento em chunks**

   Configurar o processamento para trabalhar com chunks.

   O aluno deverá compreender que o Spring Batch permite processar conjuntos de registros de forma controlada, em vez de tratar todo o conjunto de dados de uma única vez.

   Não será necessário realizar testes com grandes volumes de dados.

   O objetivo é compreender o funcionamento do modelo:

   ```
   leitura → processamento → escrita.
   ```

**Relação entre mensageria e Batch**

No `README.md`, explicar brevemente a diferença entre as duas funcionalidades implementadas nesta etapa.

A resposta deverá considerar que:

- a mensageria permite comunicação assíncrona entre componentes;
- o Spring Batch permite executar processamento estruturado sobre conjuntos de dados.

O aluno também deverá identificar uma situação do próprio projeto em que seria mais adequado utilizar:

- REST;
- mensageria;
- Batch.

**Arquitetura esperada ao final da Etapa 4**

Ao final da disciplina, a solução poderá apresentar uma arquitetura semelhante a:

```
Aplicação Principal
├── API REST
├── Banco de Dados
│
├── HTTP → Serviço Independente
│
└── Mensagem → Broker → Consumidor
```

Além disso, a solução deverá possuir um processamento em lote:

```
Fonte de Dados
↓
Spring Batch
↓
Processamento
↓
Destino
```

Não é necessário que todos os componentes estejam diretamente ligados entre si.

Cada recurso deverá existir porque atende a uma necessidade específica da solução.

**Reflexão arquitetural**

No `README.md`, responder brevemente:

- Qual operação foi escolhida para comunicação assíncrona?
- Por que essa operação não precisa necessariamente ser concluída durante a requisição original?
- O que acontece com a mensagem caso o consumidor esteja temporariamente indisponível?
- Qual funcionalidade foi escolhida para processamento em lote?
- Por que essa funcionalidade é adequada para Batch?
- Em quais situações da aplicação seria mais adequado utilizar REST, mensageria ou Batch?

As respostas deverão estar relacionadas ao projeto desenvolvido.

**Marco da Etapa 4**

Ao concluir esta etapa, registrar no repositório a tag: `etapa-4`.

Essa tag deverá representar a versão final submetida para avaliação. A Etapa 4 será utilizada como evidência da capacidade do aluno de compreender e implementar diferentes formas de processamento em uma arquitetura moderna, utilizando comunicação síncrona, comunicação assíncrona e processamento em lote.
