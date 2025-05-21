from abc import ABC
from random import uniform
from uuid import uuid4


class Sensor(ABC):
    def __init__(self, name: str, base_value: float, noise: float):
        self.name = name
        self.base_value = base_value
        self.noise = noise

    def read(self):
        return round(self.fluctuate(), 2)

    def set_value(self, new_value: float):
        if new_value < 0.0:
            self.base_value = 0
        else:
            self.base_value = new_value

    def fluctuate(self):
        # Pequena mudança gradual no base_value
        delta = uniform(-0.001, 0.001)  # variação suave e lenta
        self.base_value = max(0.0, self.base_value + delta)
        # Ruído mínimo sobre base_value
        noise = uniform(-0.005, 0.005)
        return max(0.0, self.base_value + noise)


# Sensores individuais
class TemperatureSensor(Sensor):
    def __init__(self):
        super().__init__("temperature", 20.0, 0.3)


class AirHumiditySensor(Sensor):
    def __init__(self):
        super().__init__("air_humidity", 60.0, 1.0)


class PressureSensor(Sensor):
    def __init__(self):
        super().__init__("pressure", 1013.0, 0.5)


class WindSpeedSensor(Sensor):
    def __init__(self):
        super().__init__("wind_speed", 2.0, 0.2)


class WindDirectionSensor(Sensor):
    def __init__(self):
        super().__init__("wind_direction", 90.0, 5.0)

    def _fluctuate(self):
        # Direção do vento é circular (0–360°)
        return (self.base_value + uniform(-self.noise, self.noise)) % 360


class SolarRadiationSensor(Sensor):
    def __init__(self):
        super().__init__("solar_radiation", 200.0, 20.0)


class UVSensor(Sensor):
    def __init__(self):
        super().__init__("uv_index", 5.0, 0.5)


class PrecipitationSensor(Sensor):
    def __init__(self):
        super().__init__("precipitation_rate", 0.0, 0.01)


class SoilHumiditySensor(Sensor):
    def __init__(self):
        super().__init__("soil_humidity", 30.9, 1.0)


# Estação meteorológica
class WeatherStation:
    def __init__(self):
        self.station_id = str(uuid4())
        self.sensors = {
            "temperature": TemperatureSensor(),
            "air_humidity": AirHumiditySensor(),
            "pressure": PressureSensor(),
            "wind_speed": WindSpeedSensor(),
            "wind_direction": WindDirectionSensor(),
            "solar_radiation": SolarRadiationSensor(),
            "uv_index": UVSensor(),
            "precipitation_rate": PrecipitationSensor(),
            "soil_humidity": SoilHumiditySensor()
        }

    def read_sensor(self, sensor_name: str):
        sensor = self.sensors.get(sensor_name)
        if sensor:
            return sensor.read()
        else:
            raise ValueError(f"Sensor '{sensor_name}' not found.")

    def set_sensor_value(self, sensor_name: str, new_value: float):
        sensor = self.sensors.get(sensor_name)
        if sensor:
            sensor.set_value(new_value)
        else:
            raise ValueError(f"Sensor '{sensor_name}' not found.")
