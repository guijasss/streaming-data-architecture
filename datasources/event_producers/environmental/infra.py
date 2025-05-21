from typing import Dict, Union

import asyncio
from aiokafka import AIOKafkaProducer
import json

async def send_kafka_metrics(station_id: str,
                             readings: Dict[str, Union[int, float]],
                             bootstrap_servers='localhost:9092'):
    producer = AIOKafkaProducer(bootstrap_servers=bootstrap_servers)
    await producer.start()
    try:
        while True:
            # Copia para evitar leitura concorrente
            snapshot = dict(readings)
            for name, value in snapshot.items():
                topic = f"sensor_{name}"
                message = json.dumps({"sensor": station_id, "metric": name, "value": value}).encode()
                await producer.send_and_wait(topic, message)
            await asyncio.sleep(1)  # espera 1 segundo
    finally:
        await producer.stop()
