"""Tells a client by email that their loan request was approved or rejected.

Triggered by the SQS queue the loan service writes to: one record is one decision.
"""

import json
import os

import boto3

ses = boto3.client("ses")

SUBJECTS = {
    "APPROVED": "Tu solicitud de préstamo fue aprobada",
    "REJECTED": "Tu solicitud de préstamo fue rechazada",
}


def handler(event, context):
    failed = []
    for record in event["Records"]:
        try:
            notify(json.loads(record["body"]))
        except Exception as error:  # one bad message must not make the whole batch come back
            print(f"message {record['messageId']} failed: {error!r}")
            failed.append({"itemIdentifier": record["messageId"]})
    # SQS redelivers only these; after three attempts the queue moves them to its dead-letter queue.
    return {"batchItemFailures": failed}


def notify(decision):
    amount = f"{decision['amount']:,.0f}".replace(",", ".")
    ses.send_email(
        Source=os.environ["SENDER_EMAIL"],
        Destination={"ToAddresses": [decision["userEmail"]]},
        Message={
            "Subject": {"Data": SUBJECTS[decision["status"]]},
            "Body": {"Text": {"Data": (
                f"Solicitud {decision['loanId']}: {decision['statusDescription']}. "
                f"Monto: $ {amount} a {decision['term']} meses."
            )}},
        },
    )
