from datetime import datetime
from math import sin, pi
from uuid import uuid4

from event_producers.environmental.entities import Region, SensorType
from event_producers.environmental.events import AirSensorEvent, OutputEvent
from event_producers.environmental.measures import AirSensorMeasures


class BaseSensor:
  def generate_event(self) -> OutputEvent: ...

class AirSensor(BaseSensor):
  def __init__(self, region: Region):
    self.sensor_id = str(uuid4())
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
  def _variate(valor_atual, delta):
    return round(valor_atual + delta, 2)

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
    self.wind_direction = (self.wind_direction + 2) % 360
    return round(self.wind_speed, 2), round(self.wind_direction)

  def _simulate_radiation(self):
    # Radiação aumenta até o meio do "dia" e depois diminui
    simulated_hour = (self.t % 1440) / 60  # minutos → horas do dia (0-24)
    if 6 <= simulated_hour <= 18:
      return round(800 * sin(pi * (simulated_hour - 6) / 12), 2)
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