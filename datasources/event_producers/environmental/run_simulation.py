import asyncio
import json

from aiokafka import AIOKafkaProducer
from event_producers.environmental.sensors import BaseSensor
from event_producers.environmental.setup import setup_sensors


# Config Kafka
KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"

async def send_sensor(sensor: BaseSensor, producer: AIOKafkaProducer):
    sensor_type: str = sensor.__class__.__name__.replace("Sensor", "").lower()
    print(f"[{sensor_type}] Iniciando envio para sensor {sensor.sensor_id}")

    while True:
        evento = sensor.generate_event()
        topic = f"sensor.{sensor_type}"

        await producer.send_and_wait(
            topic=topic,
            key=evento["sensor_id"].encode(),
            value=json.dumps(evento).encode()
        )

        print(f"[{sensor_type}] Evento enviado → {evento['s_hour']} | região: {evento['region']}")
        await asyncio.sleep(1)

async def main():
    # Kafka producer
    producer = AIOKafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
    await producer.start()

    try:
        sensors = setup_sensors(50)
        print(f"Total de sensores: {len(sensors)}")

        # Cria tasks assíncronas para cada sensor
        tasks = [send_sensor(sensor, producer) for sensor in sensors]
        await asyncio.gather(*tasks)

    finally:
        await producer.stop()

if __name__ == "__main__":
    asyncio.run(main())
