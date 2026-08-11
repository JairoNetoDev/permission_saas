
Etapa 2: **Estruturas de Dados e Camada de Serviço**

**Competência avaliada**

*Implementar estruturas de dados e manipular coleções em Java.*

**Objetivo**

*Evoluir a aplicação utilizando Collections, Generics, lambdas e Streams para armazenar, organizar, consultar e manipular os objetos.*

**Feature a ser desenvolvida**

1. **Armazenamento em memória**Criar estruturas utilizando Map para simular temporariamente o armazenamento dos dados da aplicação. Cada Map deverá utilizar: um identificador como chave; o objeto correspondente como valor. Os objetos deverão ser cadastrados e recuperados a partir dessas estruturas.
2. **Criação da camada de serviço**Criar classes Service responsáveis pelas operações realizadas sobre os objetos. A camada deverá disponibilizar, no mínimo, operações para: incluir; alterar; excluir; obter por identificador; obter a lista de objetos. O armazenamento utilizando Map deverá ficar encapsulado na camada de serviço.
3. **Consultas e manipulação das coleções**Implementar funcionalidades relacionadas ao domínio utilizando Collections, lambdas e Streams. A aplicação deverá possuir exemplos de operações como: filtragem; ordenação; busca; transformação de coleções. As operações deverão representar necessidades coerentes com o domínio escolhido.
4. **Tratamento de situações excepcionais**Implementar tratamento adequado para situações como: tentativa de obtenção de um objeto inexistente; fornecimento de dados inválidos; execução de operações que não possam ser realizadas. As exceções deverão representar situações relevantes para a aplicação.

**Marco da Etapa 2**

Ao concluir esta etapa, registrar no repositório a tag: etapa-2

Nesse momento, espera-se uma arquitetura semelhante a: Aplicação → Service → Map

Esse marco preservará a implementação baseada em Collections, mesmo que ela seja posteriormente substituída pela persistência em banco de dados.
