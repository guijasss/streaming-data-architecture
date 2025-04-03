from json import dumps

from confluent_kafka import Producer


KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
TOPIC_NAME = "user-events"


def send_to_kafka(event: dict):
    producer_conf = {"bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS}
    producer = Producer(producer_conf)
    producer.produce(TOPIC_NAME, key=event["tracking_id"], value=dumps(event))
    producer.flush()
    print(f"✅ Sent: {event}")
