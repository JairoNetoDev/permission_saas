
### **Uso de IAs: Sinal Verde 🟢**

Neste trabalho, os alunos são incentivados a explorar o uso de ferramentas baseadas em IA para concluir as tarefas. Todas as fontes, incluindo ferramentas de IA, devem ser devidamente citadas. O uso de IA sem a devida citação será considerado má conduta acadêmica e estará sujeito à aplicação do código disciplinar. Observe que os resultados da IA podem ser tendenciosos e imprecisos. É sua responsabilidade garantir que as informações que você usa da IA sejam precisas. Aprender como usar ferramentas baseadas em IA de maneira cuidadosa e estratégica contribui para o desenvolvimento das habilidades, refinamento de seu trabalho e prepara o aluno para sua futura carreira.

---

Neste projeto, você deve aplicar os conceitos de microsserviços para transformar uma aplicação monolítica em uma arquitetura escalável, segura e adequada para ambientes Cloud Native. O projeto é dividido em quatro etapas, cada uma com suas tarefas específicas. A conclusão de cada etapa visa garantir o domínio de aspectos cruciais no desenvolvimento de microsserviços, desde a separação da aplicação até a implementação de segurança, mensageria e deploy em nuvem.

---

### **Etapas**

| Etapa | Competência avaliada | Marco |
|---|---|---|
| [Etapa 1 — Organização Arquitetural da Aplicação](ETAPA1.md) | Implementar arquiteturas de microsserviços | `etapa-1` |
| [Etapa 2 — Separação e Comunicação entre Serviços](ETAPA2.md) | Modelar e implementar microsserviços | `etapa-2` |
| [Etapa 3 — Configuração e Execução dos Serviços](ETAPA3.md) | Desenvolver aplicações Cloud Native | `etapa-3` |
| [Etapa 4 — Comunicação Assíncrona e Processamento em Lote](ETAPA4.md) | Processar informações em lote com microsserviços | `etapa-4` |

---

### **Entrega**

Ao completar todas as tarefas de cada competência, você deve ter desenvolvido uma aplicação modular e escalável, com microsserviços bem definidos, comunicação eficiente, segurança robusta, capacidade de processamento em lote, e pronta para ser executada em um ambiente Cloud Native.

Assim que terminar, salve o seu arquivo PDF e poste no Moodle. Utilize o seu nome para nomear o arquivo, identificando também a disciplina no seguinte formato: “nomedoaluno_nomedadisciplina_pd.PDF”.

---

### **Status da entrega**

| | |
|---|---|
| Número da tentativa | Esta é a tentativa 1 (2 tentativas permitidas). |
| Status da entrega | Nenhuma tentativa |
| Status da avaliação | Não avaliado |
| Data de entrega | segunda, 5 out 2026, 23:59 |
| Tempo restante | 14 dias 3 horas |

---

### **Rubrica**

Template de Rubrica para ser utilizado com a extensão Rubricator

1. **Implementar arquiteturas de Microsserviços**

   O aluno organizou a aplicação de forma que as responsabilidades de Controller, Service e Repository estejam claramente separadas, sem regras de negócio indevidas nos Controllers ou acesso direto destes aos Repositories?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

2. **Implementar arquiteturas de Microsserviços**

   O aluno organizou a aplicação de forma que seja possível identificar claramente os principais módulos ou funcionalidades do domínio e suas respectivas responsabilidades?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

3. **Implementar arquiteturas de Microsserviços**

   O aluno implementou adequadamente validação de dados, tratamento de exceções, consultas adicionais com Spring Data JPA e documentação da API com OpenAPI/Swagger?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

4. **Implementar arquiteturas de Microsserviços**

   O aluno identificou as dependências entre os módulos e justificou de forma coerente uma funcionalidade candidata a ser separada da aplicação, considerando responsabilidade, acoplamento e dependências existentes?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

5. **Modelar e implementar Microsserviços**

   O aluno separou uma funcionalidade coerente da aplicação principal em um serviço Spring Boot independente, mantendo claramente definida a responsabilidade de cada aplicação?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

6. **Modelar e implementar Microsserviços**

   O aluno disponibilizou adequadamente a funcionalidade do novo serviço através de uma API REST, utilizando DTOs para representar os dados trocados entre as aplicações e documentando-a com OpenAPI/Swagger?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

7. **Modelar e implementar Microsserviços**

   O aluno implementou corretamente a comunicação entre a aplicação principal e o serviço independente utilizando OpenFeign e configuração externa para o endereço do serviço?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

8. **Modelar e implementar Microsserviços**

   O aluno tratou adequadamente a indisponibilidade do serviço remoto e demonstrou compreender os impactos introduzidos pela distribuição da aplicação, justificando a decisão de manter ou não a funcionalidade como serviço independente?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

9. **Desenvolver Aplicações Cloud Native**

   O aluno configurou adequadamente diferentes ambientes utilizando Profiles e externalizou configurações que podem variar entre ambientes através de arquivos de configuração ou variáveis de ambiente?

   - Não demonstrou o item de rubrica
   - Demonstrou o item de rubrica

10. **Desenvolver Aplicações Cloud Native**

    O aluno configurou a solução utilizando PostgreSQL, MySQL ou banco relacional equivalente, preservando a responsabilidade de cada serviço sobre seus próprios dados e evitando acesso direto ao banco pertencente a outro serviço?

    - Não demonstrou o item de rubrica
    - Demonstrou o item de rubrica

11. **Desenvolver Aplicações Cloud Native**

    O aluno criou corretamente as imagens Docker necessárias para executar a aplicação principal e o serviço independente em containers?

    - Não demonstrou o item de rubrica
    - Demonstrou o item de rubrica

12. **Desenvolver Aplicações Cloud Native**

    O aluno criou uma configuração Docker Compose capaz de executar de maneira integrada os principais componentes da solução, mantendo corretamente as configurações e a comunicação entre aplicações e bancos de dados?

    - Não demonstrou o item de rubrica
    - Demonstrou o item de rubrica

13. **Processar informações em lote com Microsserviços**

    O aluno implementou uma comunicação assíncrona coerente com o domínio utilizando produtor, broker/fila e consumidor de mensagens?

    - Não demonstrou o item de rubrica
    - Demonstrou o item de rubrica

14. **Processar informações em lote com Microsserviços**

    O consumidor processa corretamente as mensagens publicadas e a solução demonstra o comportamento esperado quando o consumidor está temporariamente indisponível?

    - Não demonstrou o item de rubrica
    - Demonstrou o item de rubrica

15. **Processar informações em lote com Microsserviços**

    O aluno implementou um Job Spring Batch relacionado ao domínio contendo Job, Step, ItemReader, ItemProcessor e ItemWriter, utilizando processamento em chunks?

    - Não demonstrou o item de rubrica
    - Demonstrou o item de rubrica

16. **Processar informações em lote com Microsserviços**

    O aluno demonstrou compreender as diferenças entre comunicação REST, mensageria assíncrona e processamento Batch, justificando adequadamente em quais situações do próprio projeto cada abordagem deve ser utilizada?

    - Não demonstrou o item de rubrica
    - Demonstrou o item de rubrica
