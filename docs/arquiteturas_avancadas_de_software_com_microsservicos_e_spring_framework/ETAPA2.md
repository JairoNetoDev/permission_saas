
Etapa 2: **Separação e Comunicação entre Serviços**

**Competência avaliada**

*Modelar e implementar microsserviços, compreendendo a separação de responsabilidades e a comunicação entre aplicações independentes.*

**Objetivo**

*Evoluir a aplicação organizada na etapa anterior, selecionando uma responsabilidade do sistema e transformando-a em um serviço independente.*

Nesta etapa, o foco estará na compreensão da mudança arquitetural provocada pela separação de uma funcionalidade. Uma operação que anteriormente era realizada através de uma chamada interna da aplicação passará a depender da comunicação entre duas aplicações Spring Boot distintas. O objetivo não é transformar toda a solução em microsserviços. Será suficiente extrair uma única funcionalidade que possua uma responsabilidade clara e adequada ao domínio escolhido.

**Feature a ser desenvolvida**

1. **Escolha da funcionalidade**

   Utilizar como ponto de partida a funcionalidade candidata identificada na Etapa 1.

   A funcionalidade escolhida deverá possuir uma responsabilidade que possa ser compreendida e executada de maneira relativamente independente.

   Exemplos:

   - envio de notificações;
   - comunicação por e-mail;
   - consulta de endereço;
   - geração de relatórios;
   - avaliação;
   - consulta de informações externas;
   - processamento de uma operação específica do domínio.

   A escolha deverá representar uma necessidade coerente com a aplicação.

2. **Criação do serviço independente**

   Criar uma nova aplicação Spring Boot responsável pela funcionalidade escolhida.

   Ao final desta atividade, a solução deverá possuir pelo menos:

   - aplicação principal;
   - serviço independente.

   Exemplo:

   ```
   Aplicação Acadêmica
   ↓
   Serviço de Comunicação
   ```

   As duas aplicações deverão possuir projetos independentes e poderão ser executadas separadamente.

3. **Definição da responsabilidade**

   A responsabilidade do novo serviço deverá estar claramente definida.

   No `README.md`, registrar:

   - nome do serviço;
   - responsabilidade principal;
   - funcionalidade que foi removida ou separada da aplicação principal;
   - motivo da separação.

   O aluno deverá evitar a criação de um serviço apenas para atender ao requisito da atividade.

   A separação deverá possuir uma justificativa relacionada ao domínio.

4. **Criação da API REST do novo serviço**

   Disponibilizar a funcionalidade do novo serviço através de uma API REST.

   O serviço deverá possuir pelo menos uma operação utilizada efetivamente pela aplicação principal.

   Utilizar adequadamente:

   - recursos REST;
   - métodos HTTP;
   - códigos HTTP;
   - parâmetros;
   - corpo das requisições e respostas.

   A API deverá representar uma operação coerente com a responsabilidade do serviço.

5. **Utilização de DTOs**

   Criar objetos específicos para representar os dados trocados entre as aplicações.

   Evitar utilizar diretamente as entidades JPA como contrato de comunicação entre os serviços.

   Os DTOs deverão representar apenas as informações necessárias para cada operação.

   O objetivo é começar a diferenciar:

   > modelo utilizado internamente pela aplicação
   >
   > de
   >
   > contrato utilizado para comunicação entre aplicações.

6. **Documentação da API**

   Documentar a API do novo serviço utilizando OpenAPI/Swagger.

   A documentação deverá permitir identificar:

   - endpoints;
   - operações disponíveis;
   - estruturas recebidas;
   - estruturas retornadas;
   - possíveis respostas HTTP.

   A documentação poderá ser utilizada durante os testes de integração entre as aplicações.

7. **Comunicação entre as aplicações**

   Modificar a aplicação principal para consumir a API do novo serviço.

   Utilizar OpenFeign para implementar essa comunicação.

   A aplicação principal deverá possuir um cliente responsável pela chamada ao serviço externo.

   Exemplo conceitual:

   ```
   Aplicação Principal
   ↓
   Service
   ↓
   Feign Client
   ↓
   HTTP
   ↓
   Microsserviço
   ```

   A lógica de comunicação deverá permanecer separada da camada Controller.

8. **Configuração do endereço do serviço**

   O endereço do serviço remoto não deverá ser inserido diretamente no código Java.

   Utilizar configuração externa através do `application.properties`, `application.yml` ou mecanismo equivalente.

   Exemplo:

   ```
   servico.comunicacao.url=http://localhost:8081
   ```

   A aplicação deverá utilizar essa configuração para localizar o serviço.

   A centralização de configurações será aprofundada em uma etapa posterior.

9. **Tratamento de falhas de comunicação**

   Executar testes com o serviço disponível e indisponível.

   A aplicação principal deverá tratar adequadamente uma situação na qual a comunicação com o novo serviço não possa ser realizada.

   O tratamento deverá evitar que detalhes internos da falha sejam apresentados diretamente ao usuário.

   Criar uma resposta adequada para representar a indisponibilidade da operação.

   Nesta etapa, não será obrigatório utilizar mecanismos avançados de resiliência.

   O objetivo inicial é compreender que uma chamada realizada através da rede pode falhar de maneira diferente de uma chamada interna realizada entre classes Java.

10. **Testes da comunicação**

    Utilizar Postman, Swagger UI ou ferramenta equivalente para demonstrar:

    - funcionamento isolado da API do novo serviço;
    - funcionamento da operação através da aplicação principal;
    - comportamento quando o serviço externo estiver indisponível.

    Os testes deverão permitir visualizar claramente a comunicação entre as duas aplicações.

**Arquitetura esperada ao final da Etapa 2**

Ao final desta etapa, a solução deverá apresentar uma arquitetura semelhante a:

```
Cliente HTTP
↓
Aplicação Principal
├── Controller
├── Service
├── Repository
└── Feign Client
  ↓ HTTP
  Serviço Independente
  ├── Controller
  ├── Service
  └── demais componentes necessários
```

A aplicação principal continuará concentrando a maior parte das funcionalidades.

Apenas uma responsabilidade deverá ter sido extraída.

**Reflexão arquitetural**

No `README.md`, responder brevemente:

- Qual funcionalidade foi separada da aplicação principal?
- Por que ela foi escolhida?
- O que ficou mais complexo depois da separação?
- O que aconteceria com a funcionalidade principal caso o novo serviço ficasse indisponível?
- A funcionalidade realmente precisa permanecer como um serviço independente ou poderia continuar dentro da aplicação?

Não existe uma resposta única para a última pergunta. O objetivo é demonstrar que a adoção de microsserviços representa uma decisão arquitetural e não uma obrigação tecnológica.

**Marco da Etapa 2**

Ao concluir esta etapa, registrar no repositório a tag: `etapa-2`.

Esse marco deverá representar o primeiro momento em que a solução passa a possuir aplicações independentes comunicando-se através da rede. A Etapa 2 será utilizada como evidência da capacidade do aluno de identificar uma responsabilidade, separá-la da aplicação principal e implementar a comunicação entre serviços.
