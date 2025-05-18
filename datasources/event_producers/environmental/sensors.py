from datetime import datetime
from math import sin, pi
from uuid import uuid4
from random import uniform

from event_producers.environmental.entities import Region, SensorType
from event_producers.environmental.events import AirSensorEvent, OutputEvent, WaterSensorEvent, GroundSensorEvent
from event_producers.environmental.measures import AirSensorMeasures, WaterSensorMeasures, GroundSensorMeasures


def format_t_to_hour(t: int) -> str:
  return f"{(t // 60) % 24:02d}:{t % 60:02d}"


class BaseSensor:
  def __init__(self):
    self.sensor_id = str(uuid4())

  def generate_event(self) -> OutputEvent: ...

class AirSensor(BaseSensor):
  def __init__(self, region: Region):
    super().__init__()
    self.region = region
    self.temperature = 20.0  # °C
    self.humidity = 60.0  # %
    self.pressure = 1013.0  # hPa
    self.wind_speed = 2.0  # m/s
    self.wind_direction = 90  # graus
    self.radiation = 200.0  # W/m²

    # Controle de simulação temporal
    self.t = 0  # minutos simulados

  @staticmethod
  def _variate(value: float, noise_range: float):
    return round(value + uniform(-noise_range, noise_range), 2)

  def _simulate_temperature(self):
    # Variação senoidal para simular ciclo diário (amplitude de 10°C)
    return round(20 + 5 * sin(self.t / 60 * pi / 12), 2)

  def _simulate_humidity(self):
    # Umidade inversamente proporcional à temperatura
    return round(80 - (self.temperature - 20) * 1.2, 2)

  def _simulate_pressure(self):
    # Pequenas flutuações naturais
    return round(1013 + sin(self.t / 180) * 3, 2)

  def _simulate_wind(self):
    self.wind_speed = self._variate(self.wind_speed, 0.1 * sin(self.t / 10))
    self.wind_direction = (self.wind_direction + uniform(-3, 3)) % 360
    return round(self.wind_speed, 2), round(self.wind_direction)

  def _simulate_radiation(self):
    simulated_hour = (self.t % 1440) / 60
    if 6 <= simulated_hour <= 18:
      base = 800 * sin(pi * (simulated_hour - 6) / 12)
      return self._variate(base, 25.0)
    return 0.0

  def generate_event(self) -> OutputEvent:
    self.t += 1  # avança 1 minuto simulado
    timestamp = datetime.now()

    self.temperature = self._simulate_temperature()
    self.humidity = self._simulate_humidity()
    self.pressure = self._simulate_pressure()
    self.wind_speed, self.wind_direction = self._simulate_wind()
    self.radiation = self._simulate_radiation()

    event = AirSensorEvent(
      sensor_id=self.sensor_id,
      type=SensorType.Air,
      s_hour=format_t_to_hour(self.t),
      timestamp=timestamp,
      region=self.region,
      measures=AirSensorMeasures(
        temperature=self.temperature,
        humidity=self.humidity,
        pressure=self.pressure,
        wind_speed=self.wind_speed,
        wind_direction=self.wind_direction,
        solar_radiation=self.radiation
      )
    )

    return event.serialize()


class WaterSensor(BaseSensor):
  def __init__(self, region: Region):
    super().__init__()
    self.region = region
    self.precipitation_rate = 0.0  # mm/h
    self.daily_accumulated = 0.0   # mm
    self.water_level = 1.5         # m

    self.t = 0

  @staticmethod
  def _variate(value: float, noise_range: float):
    return round(value + uniform(-noise_range, noise_range), 2)

  def _simulate_precipitation(self):
    # Pico de chuva à tarde (14h-18h)
    hour = (self.t % 1440) / 60
    if 14 <= hour <= 18:
      base = 20 * sin(pi * (hour - 14) / 4)
      return max(0.0, self._variate(base, 2.0))
    return self._variate(0.0, 0.3)

  def _simulate_accumulated(self):
    # Reinicia acumulado à meia-noite
    if self.t % 1440 == 0:
      self.daily_accumulated = 0.0
    self.daily_accumulated += self.precipitation_rate / 60  # mm por minuto
    return round(self.daily_accumulated, 2)

  def _simulate_water_level(self):
    # Aumenta com chuva, diminui sem chuva
    delta = 0.02 if self.precipitation_rate > 1 else -0.01
    self.water_level = max(0.5, min(6.0, self.water_level + delta))
    return round(self.water_level, 2)

  def generate_event(self) -> OutputEvent:
    self.t += 1
    timestamp = datetime.now()

    self.precipitation_rate = self._simulate_precipitation()
    accumulated = self._simulate_accumulated()
    level = self._simulate_water_level()

    event = WaterSensorEvent(
      sensor_id=self.sensor_id,
      type=SensorType.Water,
      s_hour=format_t_to_hour(self.t),
      timestamp=timestamp,
      region=self.region,
      measures=WaterSensorMeasures(
        precipitation_rate=self.precipitation_rate,
        daily_accumulated=accumulated,
        water_level=level
      )
    )

    return event.serialize()


class GroundSensor(BaseSensor):
  def __init__(self, region: Region):
    super().__init__()
    self.region = region
    self.acceleration = 0.01         # m/s²
    self.frequency = 2.0             # Hz
    self.inclination = 0.3           # graus
    self.soil_temp = 18.0            # °C
    self.soil_humidity = 30.0        # %

    self.t = 0

  @staticmethod
  def _variate(value: float, noise_range: float):
    return round(value + uniform(-noise_range, noise_range), 2)

  def _simulate_acceleration(self):
    # Ruído normal + picos ocasionais (terremoto)
    base = 0.01
    if uniform(0, 1) < 0.001:  # 1 evento raro
      base += uniform(0.5, 2.0)
    return self._variate(base, 0.02)

  def _simulate_frequency(self):
    return self._variate(self.frequency, 0.1)

  def _simulate_inclination(self):
    return self._variate(self.inclination, 0.02)

  def _simulate_soil_temp(self):
    base = 18 + 3 * sin(self.t / 60 * pi / 12)
    return self._variate(base, 0.3)

  def _simulate_soil_humidity(self):
    return self._variate(self.soil_humidity, 1.0)

  def generate_event(self) -> OutputEvent:
    self.t += 1
    timestamp = datetime.now()

    self.acceleration = self._simulate_acceleration()
    self.frequency = self._simulate_frequency()
    self.inclination = self._simulate_inclination()
    self.soil_temp = self._simulate_soil_temp()
    self.soil_humidity = self._simulate_soil_humidity()

    event = GroundSensorEvent(
      sensor_id=self.sensor_id,
      type=SensorType.Ground,
      s_hour=format_t_to_hour(self.t),
      timestamp=timestamp,
      region=self.region,
      measures=GroundSensorMeasures(
        ground_acceleration=self.acceleration,
        frequency=self.frequency,
        inclination=self.inclination,
        soil_temperature=self.soil_temp,
        soil_humidity=self.soil_humidity
      )
    )

    return event.serialize()
