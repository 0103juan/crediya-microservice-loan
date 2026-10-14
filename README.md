# crediya-microservice-loan

[![CI](https://github.com/0103juan/crediya-microservice-loan/actions/workflows/ci.yml/badge.svg)](https://github.com/0103juan/crediya-microservice-loan/actions/workflows/ci.yml)

Loan requests for **CrediYa**, a fictional lender. I use it to show four things working together: a small reactive microservice with a hexagonal architecture, a Kafka topic, a queue, and a Lambda function that reads it.

```
client  ── POST /loans ───────▶ ┌──────────────┐ ──▶ PostgreSQL (R2DBC)
adviser ── GET  /loans ───────▶ │ loan service │ ──▶ auth service (WebClient): users, login, JWT
adviser ── PUT  /loans/{id} ──▶ └──────┬───────┘
                                       │ the decision, as an event keyed by loan id
                                       ▼
                         Kafka  loan-decisions ── unreadable, or 3 failed attempts ──▶ loan-decisions-dlt
                                       │ consumer group loan-notifier (a listener in the same service)
                                       ▼
                         SQS  notificaciones-prestamo ── 3 failed deliveries ──▶ dead-letter queue
                                       │ event source mapping, batches of 10
                                       ▼
                         Lambda  loan-status-notifier (Python) ──▶ SES ──▶ email to the client
```

A client asks for a loan, an adviser approves or rejects it, and the client gets an email. The service never sends that email: it saves the decision and publishes it once to a Kafka topic. A consumer of that topic puts the notification on an SQS queue, and the function does the rest. Users and login live in [crediya-microservice-auth](https://github.com/0103juan/crediya-microservice-auth).

Java 21, Spring Boot 3.5 with WebFlux, R2DBC on PostgreSQL, JWT, Spring for Apache Kafka, the AWS SDK's async SQS client, and a clean architecture laid out with Bancolombia's open-source [scaffold plugin](https://github.com/bancolombia/scaffold-clean-architecture). Kafka runs as one broker in Docker and the AWS side runs on LocalStack. The API messages are in Spanish; this README is in English.

## What it does

| Endpoint | Who can call it | What it does |
|---|---|---|
| `POST /api/v1/loans` | `ROLE_CLIENTE` | Registers a loan request for the signed-in client |
| `GET /api/v1/loans?page=&size=&userEmail=&userIdNumber=` | `ROLE_ASESOR` | Requests waiting for manual review, paginated and filterable |
| `PUT /api/v1/loans/{id}` with `{"state": "APPROVED"}` or `"REJECTED"` | `ROLE_ASESOR` | Decides a request and publishes the decision |

- **The client is taken from the token, not from the body.** A request carries only the amount, the term and the loan type, so a client cannot ask for a loan in someone else's name.
- **The loan type decides the first state.** Types marked for automatic validation start as `REVIEW_PENDING`; the others start as `MANUAL_REVIEW` and show up in the adviser's list.
- **The adviser's list is one page of loans plus two lookups**, not a call per row: the users of that page are fetched from the auth service in one request, the loan types in one query, and they are joined in memory.
- **A request is decided once.** Only a pending request can be approved or rejected. Deciding it again, or sending a state that is not a decision, is a `409`, and nothing is saved or published.
- The service forwards the caller's own token to the auth service, so it holds no credentials of its own.
- An unknown loan or loan type is a `404`, an invalid body is a `400` that lists every field that failed, an unreadable body or a state that does not exist is a `400`, and the wrong role is a `403`.
- The contract is in `applications/app-service/src/main/resources/static/openapi/swagger.yaml`, served at `/swagger-ui.html`.

## The topic

- **The event** is `{"loanId", "userEmail", "status", "amount", "term"}` on the topic `loan-decisions`, published once the decision has been saved.
- **The key is the loan id.** Kafka keeps order inside a partition and sends the same key to the same partition, so the events of one loan are read in the order they were written. The topic has three partitions, so there is no order between loans, and none is needed.
- **The producer waits for the broker to confirm the write** (`acks=all`) before the adviser gets a `200`. `KafkaTemplate.send` can block while it fetches the topic's metadata, so the adapter runs it off the Netty event loop. Without a broker the request fails after 10 seconds, not the two minutes of the client's defaults.
- **The notification is one consumer group**, `loan-notifier`: a listener in the same service that puts each decision on the SQS queue. Its offset moves only after SQS has accepted the message, so a crash in between delivers the event again and does not lose it.
- **A failure does not block the partition.** A message that is not a decision goes straight to `loan-decisions-dlt`. One that fails for another reason, such as SQS being down, is tried three times a second apart and then goes there too, and the consumer moves on.
- **Why a topic in front of a queue.** The decision is a fact that other parts of a lender would want (a report, an audit trail). On a topic each of them reads it with its own consumer group, from the beginning if it starts later, and the use case does not change. Today there is one reader, so for this service alone the topic is more than the problem needs: it is here to show the pattern.

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
  driven-adapters/kafka-producer     the topic as a gateway (LoanEventGateway)
  driven-adapters/sqs-sender         the queue as a gateway (NotificationGateway)
  entry-points/reactive-web          router functions, handlers, JWT filter, error handler
  entry-points/kafka-consumer        the listener that turns a decision into a notification
  helpers/request-validator          Jakarta validation as a reactive step
  helpers/metrics                    AWS SDK metrics published through Micrometer
applications/app-service             wiring and configuration
lambda/loan-status-notifier          the function that reads the queue
deployment/                          Dockerfile, and a compose file with Kafka and LocalStack (queue, function, SES)
```

Dependencies point inwards. `UpdateLoanStatusUseCase` knows there is a decision to publish (`LoanEventGateway`) and nothing about Kafka; swapping the broker means writing one adapter. Putting the topic in front of the queue was that kind of change: the use case traded one gateway for another, and the SQS adapter was not touched. The same holds for the database and the auth service, which is why the use cases are tested with mocks and no Spring context. `ArchitectureTest` enforces part of that with ArchUnit.

## Tests

```bash
./gradlew test                                              # the service
uv run --with boto3 --with pytest pytest lambda             # the function
sh deployment/localstack/smoke.sh                           # the wiring, with the compose file up
```

- **79 tests in 24 classes** for the service: use cases with `StepVerifier`, the handlers through `WebTestClient`, the Kafka producer against a mocked template (topic, key and body), the listener (a decision is handed over, an unreadable message is rejected), the SQS adapter against a mocked client (the message goes to the configured queue and carries the client's address), the auth client against a mock web server, the repository adapters, the mapper that computes the monthly payment, the error handler and the architecture rules. The same command runs **PIT mutation testing** and writes JaCoCo coverage.
- **Two of those run the producer and the consumer against a real Kafka broker**, started inside the test JVM: a published decision reaches the notifier through the topic, and a message that is not a decision ends in the dead-letter topic. Only the SQS side is a mock there.
- **Two more run the service's own beans against a real PostgreSQL**, which Testcontainers starts in Docker and throws away: a saved loan comes back with its id and its state, and the adviser's query filters, pages and counts over real rows. On a machine without Docker they are skipped.
- **2 tests** for the function: a decision becomes an email to the client, and a bad message is reported without stopping the batch.
- **One smoke check on the real chain:** it puts a decision on the LocalStack queue and waits for the email to come out of SES.

CI runs all three on every push.

## Run it

It needs Java 21, Docker, a PostgreSQL database called `loan`, and the auth service running with the same `SECRET_JWT_TOKEN`.

```bash
docker exec crediya-pg psql -U postgres -c "CREATE DATABASE loan"   # the container from the auth README
docker compose -f deployment/docker-compose.yml up -d               # Kafka, and LocalStack: queues, Lambda, SES

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

docker exec crediya-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
     --topic loan-decisions --from-beginning --property print.key=true          # the events
docker exec crediya-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
     --describe --group loan-notifier                                           # the consumer's offsets and lag
```

The queue settings are `adapters.sqs.region`, `queueUrl` and `endpoint` in `application.yaml`. They point at LocalStack by default; with an empty `endpoint` the client uses AWS for that region. The broker is `spring.kafka.bootstrap-servers` and the topic is `adapters.kafka.decisions-topic`; the service creates the topic and its dead-letter topic at startup.

I checked the whole path by hand on 2 October 2026, with both services, one PostgreSQL and LocalStack 4.14, when the service still wrote to the queue itself:

- A client asked for a vehicle loan (stored as `MANUAL_REVIEW`) and a payroll advance (stored as `REVIEW_PENDING`). The adviser's list returned only the first, with the client's name and salary from the auth service and a monthly payment of 644,327.92 on 30,000,000 at 1.5% a year over 48 months.
- The adviser approved the first and rejected the second. The table still had two rows, with the new states, and SES showed two emails to the client's address: "Tu solicitud de préstamo fue aprobada" and "Tu solicitud de préstamo fue rechazada".
- Rejecting the approved loan afterwards was a `409`. A client trying to approve was a `403`, a loan that does not exist a `404`, and a state that does not exist or an id that is not a number a `400`.
- A message that was not JSON, put on the queue by hand, ended in the dead-letter queue after its retries, while the valid message sent with it became an email.

On 13 October 2026, with the topic in between, I ran the loan service against Kafka 4.1, LocalStack 4.14 and a PostgreSQL container. The auth service was not running: the loans were inserted with SQL and the adviser's token was signed by hand with the service's secret.

- Approving one loan and rejecting another returned `200` and left one event each on the topic, keyed `1` and `2`, in different partitions. SES showed the two emails, and the consumer group's lag was 0.
- Rejecting the approved loan afterwards was a `409` and published nothing.
- A line of text that was not JSON, written to the topic by hand, ended in `loan-decisions-dlt` and the consumer went on.
- With LocalStack stopped, approving a third loan still returned `200`. The listener tried SQS three times and the event ended in the dead-letter topic.
- With Kafka stopped, a decision was a `500` after 10 seconds, and the loan had already been saved with its new state.

## Limits

- **It has not been deployed to AWS.** The queue, the function, the trigger and SES have only run on LocalStack, which emulates them. On AWS the function would also need an IAM role with `ses:SendEmail` and permission to read the queue, and SES would need a verified domain; `init.sh` passes a role name that LocalStack does not check.
- **There is no outbox.** The decision is saved first and published second. If Kafka is unreachable at that moment the adviser gets a `500`, the decision stays saved, and the event is never published, so the email is never sent.
- **Nothing reads the dead-letter topic.** An event that could not be turned into a notification waits there; replaying it is manual.
- **A notification can go out twice.** The listener's offset is committed after SQS accepts the message. A crash between the two delivers the event again, and neither the queue nor the function removes duplicates.
- **The consumer lives in the service that produces.** It is a second entry point of the same deployable, so it scales with the HTTP API and not on its own.
- **One broker, replication factor 1, no volume.** Enough to run the flow on one machine, not a durable setup. Kafka has only run in Docker and in the tests; there is no managed cluster, authentication or TLS.
- **Two advisers deciding the same request at the same moment are not serialised.** There is no version column, so the last one wins and both notifications go out.
- **Automatic validation is not built.** Requests that start as `REVIEW_PENDING` stay there until an adviser decides them by id; they are not in the adviser's list.
- The amount is not checked against the loan type's minimum and maximum, although the table has them.
- If the auth service is down, registering a loan fails; there is no retry, timeout policy or circuit breaker around that call.
- The build has no test against a real auth service, and none that goes through HTTP into PostgreSQL: the database tests call the repositories directly. The check across both services was done by hand, as described above.
- The compose file pins LocalStack 4.14, the last release that starts without an account token.

## Where it comes from

The lender is fictional and this has never run in production. It began as the practice project of a backend training programme at Pragma, which is where the user stories and the `co.com.pragma` package name come from; the work was done in branches named after those stories (`HU2` to `HU6`) and `main` has all of it. The design decisions, the code and the tests described here are my own, and I finished the approval flow, the queue and the Lambda on my own afterwards, and added the Kafka topic in October 2026.

## License

[MIT](LICENSE).
