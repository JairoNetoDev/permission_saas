
Etapa 3: **Configuração e Execução dos Serviços**

**Competência avaliada**

*Desenvolver aplicações Cloud Native, compreendendo práticas de configuração externa, persistência independente e execução padronizada de serviços.*

**Objetivo**

*Evoluir a solução construída na etapa anterior, preparando as aplicações para serem configuradas e executadas de forma independente.*

Nesta etapa, o foco estará em reduzir dependências relacionadas ao ambiente de execução. Os serviços deverão deixar de depender de configurações fixas inseridas diretamente no código e passar a utilizar configurações externas. Também serão introduzidos recursos de containerização para permitir que as aplicações sejam executadas de maneira mais previsível e reproduzível.

**Feature a ser desenvolvida**

1. **Revisão das configurações da aplicação**

   Revisar as configurações utilizadas pela aplicação principal e pelo serviço criado na Etapa 2. Identificar informações que podem variar entre ambientes, como:

   - porta da aplicação;
   - endereço de outro serviço;
   - endereço do banco de dados;
   - usuário do banco;
   - senha do banco;
   - configurações específicas de execução.

   Essas informações não deverão ficar inseridas diretamente no código Java.

2. **Utilização de Profiles**

   Criar configurações para, pelo menos, dois ambientes. Exemplo:

   - desenvolvimento;
   - produção.

   Poderão ser utilizados arquivos como:

   ```
   application-dev.properties, application-prod.properties
   ```

   ou estruturas equivalentes utilizando YAML.

   Os arquivos deverão possuir configurações coerentes com cada ambiente. O objetivo é compreender que uma mesma aplicação pode utilizar configurações diferentes sem necessidade de modificar seu código-fonte.

3. **Variáveis de ambiente**

   Utilizar variáveis de ambiente para pelo menos algumas configurações da aplicação. Informações relacionadas ao ambiente deverão ser fornecidas externamente. Exemplos:

   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `SERVICO_COMUNICACAO_URL`

   A aplicação deverá possuir valores adequados para utilização dessas configurações durante a execução. Dados sensíveis, como senhas, não deverão ser inseridos diretamente no código Java.

4. **Evolução do banco de dados**

   Substituir o banco H2, caso ainda esteja sendo utilizado, por uma solução de banco de dados relacional adequada para execução da aplicação. Poderá ser utilizado:

   - PostgreSQL;
   - MySQL;
   - outra solução relacional aprovada durante a disciplina.

   Cada aplicação que possuir responsabilidade sobre seus próprios dados deverá utilizar seu próprio banco ou estrutura de persistência. Um serviço não deverá acessar diretamente as tabelas pertencentes a outro serviço. A comunicação entre responsabilidades separadas deverá ocorrer através das interfaces disponibilizadas pelos próprios serviços.

5. **Configuração centralizada**

   Implementar uma solução simples de configuração centralizada utilizando Spring Cloud Config Server. Criar um projeto responsável por disponibilizar configurações para as aplicações. Exemplo conceitual:

   ```
   Config Server
   ↓
   Aplicação Principal

   Config Server
   ↓
   Serviço Independente
   ```

   As configurações centralizadas poderão conter informações como:

   - portas;
   - URLs utilizadas pelos serviços;
   - configurações específicas de ambiente.

   Não será necessário criar uma estrutura complexa de gerenciamento de configuração. O objetivo é compreender o conceito de externalização e centralização de configurações em aplicações distribuídas.

6. **Containerização das aplicações**

   Criar um `Dockerfile` para a aplicação principal e para o serviço independente. Cada aplicação deverá possuir uma imagem capaz de executar o projeto de maneira independente. O `Dockerfile` deverá conter apenas as instruções necessárias para execução da aplicação. O aluno deverá ser capaz de:

   - gerar a aplicação;
   - construir a imagem;
   - iniciar o container;
   - acessar a aplicação em execução.

7. **Containerização do banco de dados**

   O banco de dados também poderá ser executado através de container. Configurar o banco utilizado pelo projeto para permitir que a aplicação se conecte a ele durante a execução local. Quando necessário, utilizar volumes para preservar os dados entre reinicializações dos containers.

8. **Orquestração local com Docker Compose**

   Criar um arquivo `docker-compose.yml` ou `compose.yml` para executar os principais componentes da solução. O arquivo deverá permitir iniciar, com um único comando, pelo menos:

   - aplicação principal;
   - serviço independente;
   - banco ou bancos utilizados pela solução.

   Caso o Config Server tenha sido utilizado na solução, ele também deverá ser incluído na composição. Uma representação possível é:

   ```
   Docker Compose
   ├── Config Server
   ├── Aplicação Principal
   ├── Serviço Independente
   ├── Banco A
   └── Banco B
   ```

   Os serviços deverão utilizar a rede criada pelo Docker Compose para se comunicar.

   Evitar utilizar `localhost` para comunicação entre containers.

9. **Verificação da aplicação**

   Com os containers em execução, demonstrar:

   - funcionamento da aplicação principal;
   - comunicação com o serviço independente;
   - acesso aos dados persistidos;
   - utilização das configurações externas;
   - execução integrada através do Docker Compose.

   O comportamento da aplicação deverá permanecer funcional após a containerização.

**Arquitetura esperada ao final da Etapa 3**

Ao final desta etapa, a solução poderá apresentar uma arquitetura semelhante a:

```
Config Server
  ↓
Aplicação Principal
  ↓ HTTP
Serviço Independente

Aplicação Principal → Banco A

Serviço Independente → Banco B
```

Todos os componentes executados através de containers e coordenados localmente pelo Docker Compose.

A arquitetura exata poderá variar de acordo com o domínio e com as necessidades do projeto.

**Reflexão arquitetural**

No `README.md`, responder brevemente:

- Quais configurações da aplicação podem variar entre ambientes?
- Quais dessas configurações foram externalizadas?
- Por que um serviço não deve acessar diretamente o banco de outro serviço?
- Qual problema o Docker resolve no projeto?
- Qual é a função do Docker Compose?
- Qual problema uma configuração centralizada procura resolver?

As respostas deverão estar relacionadas à solução desenvolvida pelo aluno.

**Marco da Etapa 3**

Ao concluir esta etapa, registrar no repositório a tag: `etapa-3`.

Esse marco deverá representar a versão da solução preparada para execução integrada e configurada externamente. A Etapa 3 será utilizada como evidência da capacidade do aluno de configurar, persistir e executar aplicações independentes de maneira padronizada.
