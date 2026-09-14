# Desafio técnico — enunciado original

> Transcrição **literal** do enunciado fornecido pelo usuário em 2026-09-12.
> Este arquivo existe para tornar auditáveis as afirmações marcadas como `[verified]`
> na especificação (`docs/spec/coupon-api-spec.md`). Não editar: divergências de
> interpretação vão para a seção "Decisões tomadas" da spec, não para cá.

---

Utilizando o Spring Initializer, gere um projeto Java Spring que implemente os endpoints
definidos na documentação, seguindo as regras de negócio estabelecidas.

start.spring.io

Documentação da API.

/coupon - Coupon Api

Regras de Negócio.

## 1. Create

- Um cupom pode ser cadastrado a qualquer momento e possui como obrigatórios os campos:
  code
  description
  discountValue
  expirationDate
- O código de um cupom é um campo alfanumérico com um tamanho padrão de 6 caracteres.
- Caracteres especiais podem ser aceitos na criação, contudo precisam ser removidos pela
  aplicação antes de salvar e retornar na resposta, garantindo o tamanho de 6 caracteres.
- O valor de desconto do cupom possui um saldo mínimo de 0,5 sem máximo predeterminado.
  (saldo é absoluto e não há preocupações com moeda.)
- O cupom nunca pode ser criado com data de expiração no passado.
  Um cupom pode ser criado como já publicado.

## 2. Delete

- Um cupom pode ser deletado a qualquer momento.
- Deve ser feito um soft delete do cupom no banco de dados, garantindo a não perda de
  informações recebidas no cadastro.
- Não deve ser possível deletar um cupom já deletado.

## 3. Expectativas

- Testes cobrindo as regras de negócio (80%). (Utilize o Jacoco para validar)
- Utilização de banco em memória H2.
- Publicação do projeto no GitHub com repositório público.
- As regras de negócio devem estar encapsuladas em objetos de domínio.
  (Domínio é diferente de entidade JPA.)
- Docker e Docker Compose.
- Swagger.
- Reamed.md
- Utilizar os novos recursos do Java 21

## 4. Estrutura

- 2 modulos, uma "api" e outro "domain"
- Utilizar principios de CleanCode e Clean Architecture
- Evitar JavaDoc, somente onde realmente é necessário pois o codigo tem que ser claro

---

## Nota sobre a "Documentação da API"

O enunciado referencia uma documentação da API `/coupon` que **não foi fornecida** e que o
usuário confirmou não possuir (2026-09-12). Todo o contrato HTTP na spec é, portanto,
**inferido** a partir das regras de negócio acima — registrado como risco aceito na
seção 11 da spec.
