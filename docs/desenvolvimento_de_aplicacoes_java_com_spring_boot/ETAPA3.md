
Etapa 3: **API REST com Spring Boot**

**Competência avaliada**

*Desenvolver e configurar aplicações utilizando o framework Spring Boot.*

**Objetivo**

*Transformar a aplicação construída anteriormente em uma API REST utilizando Spring Boot e Spring MVC.*

**Feature a ser desenvolvida**

1. **Organização da aplicação**Organizar o projeto utilizando uma separação clara de responsabilidades. A aplicação deverá possuir: Controller → Service. Neste momento, os dados ainda poderão permanecer armazenados em memória através das estruturas desenvolvidas na Etapa 2.
2. **Implementação dos endpoints REST**Criar controllers REST para disponibilizar as principais funcionalidades através de HTTP. Para as principais entidades deverão existir operações de: inclusão; alteração; exclusão; obtenção da lista; obtenção por identificador. Utilizar adequadamente os métodos HTTP: GET; POST; PUT; DELETE. As URLs deverão representar recursos do domínio.
3. **Comunicação entre Controller e Service**Os controllers deverão ser responsáveis pela comunicação HTTP e delegar as operações e regras da aplicação para a camada de serviço. Evite implementar regras de negócio diretamente nos controllers. Utilizar os mecanismos de Inversão de Controle e Injeção de Dependência disponibilizados pelo Spring, preferencialmente através de injeção por construtor.
4. **Respostas HTTP**A API deverá utilizar códigos HTTP adequados para representar o resultado das operações. Entre eles: 200 OK; 201 Created; 204 No Content; 400 Bad Request; 404 Not Found.
5. **Testes da API**Utilizar o Postman ou ferramenta equivalente para executar e testar as requisições. Os testes deverão contemplar as principais operações implementadas e estar organizados de maneira que seja possível demonstrar o funcionamento da API.
6. **Documentação da API**Documentar os principais endpoints utilizando uma solução baseada em OpenAPI/Swagger. A documentação deverá permitir identificar recursos, operações, parâmetros e estruturas disponibilizadas pela aplicação.

**Marco da Etapa 3**

Ao concluir esta etapa, registrar no repositório a tag: etapa-3. Nesse momento, espera-se uma arquitetura semelhante a: Cliente HTTP → Controller → Service → Map.

Esse marco será utilizado para avaliar a construção da aplicação Spring Boot e sua API REST antes da introdução da persistência.
