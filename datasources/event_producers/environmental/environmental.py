from event_producers.environmental.sensors import AirSensor, GroundSensor, WaterSensor

air = AirSensor(region="north-1")
water = WaterSensor(region="north-1")
ground = GroundSensor(region="north-1")

for i in range(0, 5):
    print(air.generate_event())
    print(water.generate_event())
    print(ground.generate_event())
