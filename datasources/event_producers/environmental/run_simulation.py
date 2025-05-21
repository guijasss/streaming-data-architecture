import asyncio

from event_producers.environmental.entities import WeatherStation
from event_producers.environmental.infra import send_kafka_metrics
from event_producers.environmental.ui import WeatherStationUI


async def main():
    station = WeatherStation()
    app = WeatherStationUI(station)

    asyncio.create_task(send_kafka_metrics(station.station_id, app.readings))

    while True:
        app.update()
        await asyncio.sleep(1)

if __name__ == "__main__":
    asyncio.run(main())
