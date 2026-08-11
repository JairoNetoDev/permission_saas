
Etapa 4: **APIs REST e Persistência com Spring Data**

**Competência avaliada**

*Implementar APIs REST e camada de persistência com Spring Data.*

**Objetivo**

*Evoluir a aplicação substituindo o armazenamento temporário em memória por persistência em banco de dados utilizando Spring Data JPA. Ao final desta etapa, a aplicação deverá apresentar uma arquitetura semelhante a: Cliente HTTP → Controller → Service → Repository → Banco de Dados.*

**Feature a ser desenvolvida**

1. **Mapeamento das entidades**Mapear as classes que necessitam de persistência utilizando JPA. Utilizar as anotações apropriadas, incluindo: @Entity; @Id; geração de identificadores quando necessária; anotações para representação dos relacionamentos. O relacionamento um-para-muitos deverá ser representado utilizando @OneToMany e @ManyToOne, quando aplicável. Caso o modelo utilize herança entre entidades persistentes, utilizar uma estratégia adequada de mapeamento JPA.
2. **Criação da camada Repository**Criar interfaces Repository utilizando Spring Data JPA. Os repositories deverão estender JpaRepository ou outra interface adequada do Spring Data. Utilizar os recursos do framework para realizar operações como: save; findById; findAll; deleteById.
3. **Evolução da camada de serviço**Modificar a camada de serviço para substituir o armazenamento realizado através de Map pelos repositories. A arquitetura deverá evoluir de: Controller → Service → Map para: Controller → Service → Repository → Banco de Dados. Os controllers deverão continuar utilizando a camada de serviço, sem acessar diretamente os repositories. A implementação com Map não precisa permanecer no código final, pois estará preservada no marco etapa-3.
4. **Consultas personalizadas**Criar consultas adicionais utilizando os recursos do Spring Data. Implementar métodos findBy relacionados às características do domínio escolhido. Também deverá existir pelo menos uma operação que permita filtrar ou ordenar resultados.
5. **Validação dos dados**Implementar validações dos dados recebidos pela API utilizando Bean Validation. Utilizar anotações adequadas às regras da aplicação, como: @NotNull; @NotBlank; @Size; @Min; @Max; ou outras que façam sentido para o modelo. Requisições contendo dados inválidos deverão produzir respostas HTTP adequadas.
6. **Relacionamentos e serialização**Garantir o funcionamento adequado dos relacionamentos entre entidades nas respostas JSON. Quando necessário, utilizar estratégias para evitar referências circulares e controlar a serialização dos objetos relacionados.

**Marco da Etapa 4**

Ao concluir esta etapa, registrar no repositório a tag: etapa-4. Essa tag deverá representar a versão final submetida para avaliação.
