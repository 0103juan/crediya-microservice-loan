# crediya-microservice-loan

[![CI](https://github.com/0103juan/crediya-microservice-loan/actions/workflows/ci.yml/badge.svg)](https://github.com/0103juan/crediya-microservice-loan/actions/workflows/ci.yml)

Loan requests for **CrediYa**, a fictional lender. I use it to show three things working together: a small reactive microservice with a hexagonal architecture, a queue, and a Lambda function that reads it.

```
client  ── POST /loans ───────▶ ┌──────────────┐ ──▶ PostgreSQL (R2DBC)
adviser ── GET  /loans ───────▶ │ loan service │ ──▶ auth service (WebClient): users, login, JWT
adviser ── PUT  /loans/{id} ──▶ └──────┬───────┘
                                       │ the decision, as JSON
                                       ▼
                         SQS  notificaciones-prestamo ── 3 failed deliveries ──▶ dead-letter queue
                                       │ event source mapping, batches of 10
                                       ▼
                         Lambda  loan-status-notifier (Python) ──▶ SES ──▶ email to the client
```

A client asks for a loan, an adviser approves or rejects it, and the client gets an email. The service never sends that email: it saves the decision and puts a message on a queue, and the function does the rest. Users and login live in [crediya-microservice-auth](https://github.com/0103juan/crediya-microservice-auth).

Java 21, Spring Boot 3.5 with WebFlux, R2DBC on PostgreSQL, JWT, the AWS SDK's async SQS client, and a clean architecture laid out with Bancolombia's open-source [scaffold plugin](https://github.com/bancolombia/scaffold-clean-architecture). The AWS side runs on LocalStack. The API messages are in Spanish; this README is in English.

## What it does

| Endpoint | Who can call it | What it does |
|---|---|---|
| `POST /api/v1/loans` | `ROLE_CLIENTE` | Registers a loan request for the signed-in client |
| `GET /api/v1/loans?page=&size=&userEmail=&userIdNumber=` | `ROLE_ASESOR` | Requests waiting for manual review, paginated and filterable |
| `PUT /api/v1/loans/{id}` with `{"state": "APPROVED"}` or `"REJECTED"` | `ROLE_ASESOR` | Decides a request and queues the notification |

- **The client is taken from the token, not from the body.** A request carries only the amount, the term and the loan type, so a client cannot ask for a loan in someone else's name.
- **The loan type decides the first state.** Types marked for automatic validation start as `REVIEW_PENDING`; the others start as `MANUAL_REVIEW` and show up in the adviser's list.
- **The adviser's list is one page of loans plus two lookups**, not a call per row: the users of that page are fetched from the auth service in one request, the loan types in one query, and they are joined in memory.
- **A request is decided once.** Only a pending request can be approved or rejected. Deciding it again, or sending a state that is not a decision, is a `409`, and nothing is saved or queued.
- The service forwards the caller's own token to the auth service, so it holds no credentials of its own.
- An unknown loan or loan type is a `404`, an invalid body is a `400` that lists every field that failed, an unreadable body or a state that does not exist is a `400`, and the wrong role is a `403`.
- The contract is in `applications/app-service/src/main/resources/static/openapi/swagger.yaml`, served at `/swagger-ui.html`.

## The queue and the Lambda

- **The message** is `{"loanId", "userEmail", "status", "statusDescription", "amount", "term"}`. The email address is the one stored with the loan, which came from the client's token when they asked for it.
- **The function** ([lambda/loan-status-notifier/handler.py](lambda/loan-status-notifier/handler.py)) is about 40 lines of Python. For each record it sends one email through SES.
- **Failures are per message.** The function returns the ids of the records it could not process (`ReportBatchItemFailures`), so SQS redelivers only those and one bad message does not send the rest of the batch again. After three failed deliveries the queue moves the message to a dead-letter queue.
- **The resources are created by a script**, [deployment/localstack/init.sh](deployment/localstack/init.sh): the two queues, the verified sender, the function and the trigger between the queue and the function. It uses the AWS CLI, pointed at LocalStack.

## How it is built

```
domain/model          Loan, LoanType, State, the exceptions and the gateway interfaces. No framework imports.
domain/usecase        RegisterLoan, FindLoans, UpdateLoanStatus: plain classes over the gateways.
infrastructure/
  driven-adapters/r2dbc-postgresql   loans and loan types on R2DBC, with a dynamic paginated query
  driven-adapters/web-client         the auth service as a gateway (AuthUserRepository)
  driven-adapters/sqs-sender         the queue as a gateway (NotificationGateway)
  entry-points/reactive-web          router functions, handlers, JWT filter, error handler
  helpers/request-validator          Jakarta validation as a reactive step
  helpers/metrics                    AWS SDK metrics published through Micrometer
applications/app-service             wiring and configuration
lambda/loan-status-notifier          the function that reads the queue
deployment/                          Dockerfile, and LocalStack with the queue, the function and SES
```

Dependencies point inwards. `UpdateLoanStatusUseCase` knows there is something to notify (`NotificationGateway`) and nothing about SQS; swapping the queue for another transport means writing one adapter. The same holds for the database and the auth service, which is why the use cases are tested with mocks and no Spring context. `ArchitectureTest` enforces part of that with ArchUnit.

## Tests

```bash
./gradlew test                                              # the service
uv run --with boto3 --with pytest pytest lambda             # the function
sh deployment/localstack/smoke.sh                           # the wiring, with the compose file up
```

- **68 tests in 20 classes** for the service: use cases with `StepVerifier`, the handlers through `WebTestClient`, the SQS adapter against a mocked client (the message goes to the configured queue and carries the client's address), the auth client against a mock web server, the repository adapters, the mapper that computes the monthly payment, the error handler and the architecture rules. The same command runs **PIT mutation testing** and writes JaCoCo coverage.
- **2 tests** for the function: a decision becomes an email to the client, and a bad message is reported without stopping the batch.
- **One smoke check on the real chain:** it puts a decision on the LocalStack queue and waits for the email to come out of SES.

CI runs all three on every push.

## Run it

It needs Java 21, Docker, a PostgreSQL database called `loan`, and the auth service running with the same `SECRET_JWT_TOKEN`.

```bash
docker exec crediya-pg psql -U postgres -c "CREATE DATABASE loan"   # the container from the auth README
docker compose -f deployment/docker-compose.yml up -d               # LocalStack: queues, Lambda, SES

export SECRET_JWT_TOKEN="the-same-secret-as-the-auth-service"
export APP_DB_INIT_ENABLED=true          # creates the tables, the states and five loan types
export AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test   # LocalStack accepts any value
./gradlew bootRun                        # http://localhost:8080
```

Sign in on the auth service as a client and ask for a loan; sign in as an adviser and decide it:

```bash
curl -X POST localhost:8080/api/v1/loans -H "Authorization: Bearer $CLIENT_TOKEN" \
     -H "Content-Type: application/json" -d '{"amount": 30000000, "term": 48, "loanType": 2}'
curl -X PUT localhost:8080/api/v1/loans/1 -H "Authorization: Bearer $ADVISER_TOKEN" \
     -H "Content-Type: application/json" -d '{"state": "APPROVED"}'
curl localhost:4566/_aws/ses             # the emails LocalStack's SES has "sent"
```

The queue settings are `adapters.sqs.region`, `queueUrl` and `endpoint` in `application.yaml`. They point at LocalStack by default; with an empty `endpoint` the client uses AWS for that region.

I checked the whole path by hand on 2 October 2026, with both services, one PostgreSQL and LocalStack 4.14:

- A client asked for a vehicle loan (stored as `MANUAL_REVIEW`) and a payroll advance (stored as `REVIEW_PENDING`). The adviser's list returned only the first, with the client's name and salary from the auth service and a monthly payment of 644,327.92 on 30,000,000 at 1.5% a year over 48 months.
- The adviser approved the first and rejected the second. The table still had two rows, with the new states, and SES showed two emails to the client's address: "Tu solicitud de préstamo fue aprobada" and "Tu solicitud de préstamo fue rechazada".
- Rejecting the approved loan afterwards was a `409`. A client trying to approve was a `403`, a loan that does not exist a `404`, and a state that does not exist or an id that is not a number a `400`.
- A message that was not JSON, put on the queue by hand, ended in the dead-letter queue after its retries, while the valid message sent with it became an email.

## Limits

- **It has not been deployed to AWS.** The queue, the function, the trigger and SES have only run on LocalStack, which emulates them. On AWS the function would also need an IAM role with `ses:SendEmail` and permission to read the queue, and SES would need a verified domain; `init.sh` passes a role name that LocalStack does not check.
- **There is no outbox.** The decision is saved first and queued second. If SQS is unreachable at that moment the adviser gets a `500`, the decision stays saved and the email is never sent.
- **Two advisers deciding the same request at the same moment are not serialised.** There is no version column, so the last one wins and both notifications go out.
- **Automatic validation is not built.** Requests that start as `REVIEW_PENDING` stay there until an adviser decides them by id; they are not in the adviser's list.
- The amount is not checked against the loan type's minimum and maximum, although the table has them.
- If the auth service is down, registering a loan fails; there is no retry, timeout policy or circuit breaker around that call.
- The build has no integration test against a real PostgreSQL or a real auth service; the check across both services was done by hand, as described above.
- The compose file pins LocalStack 4.14, the last release that starts without an account token.

## Where it comes from

The lender is fictional and this has never run in production. It began as the practice project of a backend training programme at Pragma, which is where the user stories and the `co.com.pragma` package name come from; the work was done in branches named after those stories (`HU2` to `HU6`) and `main` has all of it. The design decisions, the code and the tests described here are my own, and I finished the approval flow, the queue and the Lambda on my own afterwards.
