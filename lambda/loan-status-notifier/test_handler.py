import json
import os

os.environ.setdefault("AWS_DEFAULT_REGION", "us-east-1")  # boto3 needs a region to build the client; no call is made
os.environ.setdefault("SENDER_EMAIL", "no-reply@crediya.example")

import handler  # noqa: E402

DECISION = {
    "loanId": 7,
    "userEmail": "cliente@test.com",
    "status": "APPROVED",
    "statusDescription": "Préstamo Aprobado",
    "amount": 30000000.00,
    "term": 48,
}


class FakeSes:
    def __init__(self):
        self.sent = []

    def send_email(self, **email):
        self.sent.append(email)


def record(message_id, body):
    return {"messageId": message_id, "body": body}


def test_each_decision_becomes_an_email_to_the_client(monkeypatch):
    ses = FakeSes()
    monkeypatch.setattr(handler, "ses", ses)

    result = handler.handler({"Records": [record("1", json.dumps(DECISION))]}, None)

    assert result == {"batchItemFailures": []}
    [email] = ses.sent
    assert email["Source"] == "no-reply@crediya.example"
    assert email["Destination"] == {"ToAddresses": ["cliente@test.com"]}
    assert email["Message"]["Subject"]["Data"] == "Tu solicitud de préstamo fue aprobada"
    assert "$ 30.000.000 a 48 meses" in email["Message"]["Body"]["Text"]["Data"]


def test_a_bad_message_is_reported_and_the_rest_of_the_batch_is_sent(monkeypatch):
    ses = FakeSes()
    monkeypatch.setattr(handler, "ses", ses)
    not_a_decision = json.dumps({**DECISION, "status": "MANUAL_REVIEW"})

    result = handler.handler({"Records": [
        record("1", "not json"),
        record("2", not_a_decision),
        record("3", json.dumps(DECISION)),
    ]}, None)

    assert result == {"batchItemFailures": [{"itemIdentifier": "1"}, {"itemIdentifier": "2"}]}
    assert len(ses.sent) == 1
