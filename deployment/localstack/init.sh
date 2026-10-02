#!/bin/sh
# Runs inside LocalStack once it is ready. awslocal is the AWS CLI pointed at LocalStack,
# so these are the same calls that would create the resources on AWS.
set -eu

ACCOUNT=000000000000
REGION=us-east-1
QUEUE=notificaciones-prestamo
SENDER=no-reply@crediya.example

# The queue, and a dead-letter queue for messages that fail three times.
awslocal sqs create-queue --queue-name "$QUEUE-dlq"
cat > /tmp/redrive.json <<EOF
{"RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:$REGION:$ACCOUNT:$QUEUE-dlq\",\"maxReceiveCount\":\"3\"}"}
EOF
awslocal sqs create-queue --queue-name "$QUEUE" --attributes file:///tmp/redrive.json

# SES only sends from an address it has verified.
awslocal ses verify-email-identity --email-address "$SENDER"

# The Lambda: one Python file, zipped as it is.
(cd /opt/notifier && python3 -m zipfile -c /tmp/notifier.zip handler.py)
awslocal lambda create-function \
  --function-name loan-status-notifier \
  --runtime python3.13 \
  --handler handler.handler \
  --zip-file fileb:///tmp/notifier.zip \
  --role "arn:aws:iam::$ACCOUNT:role/loan-status-notifier" \
  --environment "Variables={SENDER_EMAIL=$SENDER}"
awslocal lambda wait function-active-v2 --function-name loan-status-notifier

# The trigger: the queue invokes the Lambda, which reports the records that failed so only those come back.
awslocal lambda create-event-source-mapping \
  --function-name loan-status-notifier \
  --event-source-arn "arn:aws:sqs:$REGION:$ACCOUNT:$QUEUE" \
  --batch-size 10 \
  --function-response-types ReportBatchItemFailures
