# Coupon API

API HTTP para **cadastro** e **remoção lógica (soft delete)** de cupons de desconto.
Stack: Java 21, Spring Boot 4.1.1, Gradle multi-módulo (`:api` + `:domain`), H2 em memória, Flyway, Spring Data JPA, springdoc-openapi.

## Módulos

| Módulo | Conteúdo |
|---|---|
| `:domain` | Domínio puro (`domain`), adapter JPA (`persistence`), config de persistência (`config`) |
| `:api` | Controllers, DTOs, Problem Details, OpenAPI, `CouponApplication` |

## Pré-requisitos

- JDK 21
- Docker (opcional — só para empacotar/rodar em contêiner; **não** é necessário para build/teste)

## Build e testes

```bash
./gradlew.bat clean build
```

A suíte completa roda contra H2 em memória (sem Docker, sem rede, sem banco instalado).

Cobertura global (JaCoCo, sem exclusões) ≥ 80% — o build falha abaixo disso.

Relatório agregado:

```text
build/reports/jacoco/aggregated/index.html
```

### Natureza dos testes

1. **Domínio** — Java puro, sem Spring
2. **Integração** — `@SpringBootTest` + H2 + consulta JDBC (prova do soft delete)
3. **Web** — MockMvc (Problem Details, OpenAPI, content-type)

## Executar localmente

```bash
./gradlew.bat :api:bootRun
```

- API: http://localhost:8080/api/coupon
- Swagger UI: http://localhost:8080/api/coupon/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/coupon/v3/api-docs

### Endpoints

| Método | Caminho | Sucesso |
|---|---|---|
| `POST` | `/api/coupon/coupons` | `201` + `Location` + corpo |
| `DELETE` | `/api/coupon/coupons/{id}` | `204` |

Não há endpoints `GET` de cupom.

### Exemplo — criar

```bash
curl -X POST http://localhost:8080/api/coupon/coupons ^
  -H "Content-Type: application/json" ^
  -d "{\"code\":\"ab-12#c7\",\"description\":\"Boas-vindas\",\"discountValue\":10.50,\"expirationDate\":\"2026-12-31\",\"published\":true}"
```

### Exemplo — deletar

```bash
curl -X DELETE http://localhost:8080/api/coupon/coupons/{id}
```

## Console H2 — verificar soft delete

O console existe **apenas para o desafio técnico** (inspeção manual do banco). Não faz parte do contrato da API e deve permanecer desligado em produção (`spring.profiles.active=prod`).

| | |
|---|---|
| URL | http://localhost:8080/api/coupon/h2-console |
| JDBC URL | `jdbc:h2:mem:coupon` |
| User | `coupon` |
| Password | `coupon-dev` |

### Passo a passo

1. Suba a aplicação (`bootRun` ou Docker).
2. Crie um cupom via Swagger ou `curl` (anote o `id` e o `code`).
3. Delete o cupom (`DELETE /api/coupon/coupons/{id}` → `204`).
4. Abra o console H2, conecte com a JDBC URL e as credenciais acima.
5. Execute:

```sql
SELECT id, code, raw_code, description, discount_value, expiration_date,
       published, deleted, deleted_at, created_at
FROM coupon;
```

6. Confirme: a linha **continua** na tabela, `deleted = TRUE`, `deleted_at` preenchido, e os campos do cadastro (incluindo `raw_code`) intactos.

### Banco volátil

O H2 é **em memória**. Quando a JVM termina, os dados são descartados. Isso **não** contradiz o soft delete: a regra exige não apagar a linha **na operação de deleção**, não durabilidade entre execuções.

A cada boot o Flyway recria o schema do zero (`ddl-auto: none`).

## Docker

O `bootJar` gera `api/build/libs/app.jar` (nome fixo usado pelo `Dockerfile`).

```bash
docker compose up --build
```

Serviço único na porta `8080` (aplicação + console H2 no mesmo HTTP). Sem contêiner de banco.

```bash
./gradlew.bat :api:bootJar
docker build -t coupon-api .
docker run --rm -p 8080:8080 coupon-api
```

## Decisões de arquitetura (resumo)

- Domínio e persistência no módulo `:domain`; API HTTP no `:api`
- Unicidade de `code` entre não deletados garantida na aplicação — H2 não suporta índice parcial
- `rawCode` persistido, nunca exposto na API
- Erros com `code`/`message` (e `errors[]` na validação), mensagens pt-BR
- Console H2 ligado no perfil default (desafio técnico)

## Checklist de testes manuais

Use após `./gradlew.bat :api:bootRun` (ou `docker compose up --build`).

| # | Ação | Esperado |
|---|---|---|
| M-01 | Abrir Swagger UI | Dois endpoints apenas: `POST /api/coupon/coupons`, `DELETE /api/coupon/coupons/{id}` |
| M-02 | `POST` com `code: "ab-12#c7"` e data futura | `201`; resposta com `code: "AB12C7"`; **sem** `rawCode`; header `Location` |
| M-03 | `POST` com mesmo código de novo | `409` `CODE_DUPLICATED` |
| M-04 | `DELETE` do `id` criado | `204` |
| M-05 | Console H2 → `SELECT * FROM coupon` | Linha existe; `deleted=TRUE`; `raw_code` original; demais campos intactos |
| M-06 | `DELETE` de novo no mesmo `id` | `409` `COUPON_ALREADY_DELETED` |
| M-07 | `POST` reusando o código do cupom deletado | `201` (código liberado) |
| M-08 | `POST` com `discountValue: 0.49` | `400` `DISCOUNT_BELOW_MIN` |
| M-09 | `POST` com `expirationDate` de ontem | `400` `EXPIRATION_IN_PAST` |
| M-10 | Acessar `/api/coupon/actuator` | `404` (sem Actuator) |

Documentação: `docs/desafio-tecnico.md`.
