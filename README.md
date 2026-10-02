# crediya-microservice-loan

[![CI](https://github.com/0103juan/crediya-microservice-loan/actions/workflows/ci.yml/badge.svg)](https://github.com/0103juan/crediya-microservice-loan/actions/workflows/ci.yml)

Loan requests for **CrediYa**, a fictional lender. A client asks for a loan; an adviser sees the requests that need a manual review, each with the client's data and the monthly payment. It is one of two reactive microservices; users and login live in [crediya-microservice-auth](https://github.com/0103juan/crediya-microservice-auth).

Java 21, Spring Boot 3.5 with WebFlux, R2DBC on PostgreSQL, JWT, a non-blocking `WebClient` to the auth service, and a clean architecture laid out with Bancolombia's open-source [scaffold plugin](https://github.com/bancolombia/scaffold-clean-architecture). The API messages are in Spanish; this README is in English.

## What it does

| Endpoint | Who can call it | What it does |
|---|---|---|
| `POST /api/v1/loans` | `ROLE_CLIENTE` | Registers a loan request for the signed-in client |
| `GET /api/v1/loans?page=&size=&userEmail=&userIdNumber=` | `ROLE_ASESOR` | Requests waiting for manual review, paginated and filterable |

- **The client is taken from the token, not from the body.** A request carries only the amount, the term and the loan type, so a client cannot ask for a loan in someone else's name.
- **The loan type decides the first state.** Types marked for automatic validation start as `REVIEW_PENDING`; the others start as `MANUAL_REVIEW` and show up in the adviser's list.
- **The adviser's list is one page of loans plus two lookups**, not a call per row: the users of that page are fetched from the auth service in one request, the loan types in one query, and they are joined in memory.
- The service forwards the caller's own token to the auth service, so it holds no credentials of its own.
- An unknown loan type is a `404`, an invalid body is a `400` that lists every field that failed, and the wrong role is a `403`.
- The contract is in `applications/app-service/src/main/resources/static/openapi/swagger.yaml`, served at `/swagger-ui.html`.

## How it is built

```
domain/model          Loan, LoanType, State, the exceptions and the gateway interfaces. No framework imports.
domain/usecase        RegisterLoan, FindLoans: plain classes over the gateways.
infrastructure/
  driven-adapters/r2dbc-postgresql   loans and loan types on R2DBC, with a dynamic paginated query
  driven-adapters/web-client         the auth service as a gateway (AuthUserRepository)
  entry-points/reactive-web          router functions, handlers, JWT filter, error handler
  helpers/request-validator          Jakarta validation as a reactive step
applications/app-service             wiring and configuration
```

Dependencies point inwards: the use cases see the database and the auth service only as interfaces, which is why they are tested with mocks and no Spring context. `ArchitectureTest` enforces part of that with ArchUnit.

## Tests

```bash
./gradlew test
```

55 tests in 17 classes: use cases with `StepVerifier`, the handlers through `WebTestClient`, the auth client against a mock web server, the repository adapters, the mapper that computes the monthly payment, the error handler and the architecture rules. The same command runs **PIT mutation testing** and writes JaCoCo coverage. CI runs it on every push.

## Run it

It needs Java 21, a PostgreSQL database called `loan`, and the auth service running with the same `SECRET_JWT_TOKEN`.

```bash
docker exec crediya-pg psql -U postgres -c "CREATE DATABASE loan"   # the container from the auth README

export SECRET_JWT_TOKEN="the-same-secret-as-the-auth-service"
export APP_DB_INIT_ENABLED=true      # creates the tables, the states and five loan types
./gradlew bootRun                    # http://localhost:8080
```

Then sign in on the auth service as a client and send the token:

```bash
curl -X POST localhost:8080/api/v1/loans -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" -d '{"amount": 30000000, "term": 48, "loanType": 2}'
```

I checked the whole path by hand on 1 October 2026, with both services against one PostgreSQL. A client asked for a vehicle loan (stored as `MANUAL_REVIEW`) and a payroll advance (stored as `REVIEW_PENDING`). The adviser's list returned only the first, with the client's name and salary from the auth service and a monthly payment of 644,327.92 on 30,000,000 at 1.5% a year over 48 months. An adviser asking for a loan and a client asking for the list both got a `403`, and a request without a token a `401`.

## Limits

- A practice project: the lender is fictional and it has never run in production.
- **A request can only be created and listed.** Approving or rejecting one, and telling the client through a queue, is unfinished work that is not in this repository's `main`.
- The amount is not checked against the loan type's minimum and maximum, although the table has them.
- If the auth service is down, registering a loan fails; there is no retry, timeout policy or circuit breaker around that call.
- There are no integration tests against a real PostgreSQL or a real auth service in the build; the check across both services was done by hand, as described above.
- The work was done in branches named after user stories (`HU2` to `HU5`); `main` has all of it.
