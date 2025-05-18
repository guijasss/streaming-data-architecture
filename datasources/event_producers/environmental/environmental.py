from event_producers.environmental.sensors import AirSensor

air = AirSensor(region="north-1")

for i in range(0, 30):
    print(air.generate_event())
