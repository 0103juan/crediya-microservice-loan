#!/bin/sh
# Proves the wiring from the host: a decision put on the queue comes out of SES as an email.
# Needs the compose file up, curl and docker.
set -eu

QUEUE_URL=http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/notificaciones-prestamo

until curl -fs localhost:4566/_localstack/init/ready | grep -q '"completed": *true'; do sleep 2; done

docker exec crediya-localstack awslocal sqs send-message --queue-url "$QUEUE_URL" --message-body \
  '{"loanId":1,"userEmail":"smoke@crediya.example","status":"APPROVED","statusDescription":"Aprobado","amount":30000000,"term":48}' \
  > /dev/null

for _ in $(seq 45); do
  if curl -fs localhost:4566/_aws/ses | grep -q smoke@crediya.example; then
    echo "queue -> Lambda -> SES: email sent"
    exit 0
  fi
  sleep 2
done

echo "no email after 90 s" >&2
docker logs --tail 60 crediya-localstack >&2
exit 1
