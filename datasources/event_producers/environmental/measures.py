from typing import Dict, Union

from dataclasses import dataclass

class Measures:
  def to_dict(self) -> Dict[str, Union[int, float]]:
    return self.__dict__

@dataclass
class AirSensorMeasures(Measures):
  temperature: float
  humidity: float
  pressure: float
  wind_speed: float
  wind_direction: int
  solar_radiation: float

@dataclass
class GroundSensorMeasures(Measures):
  ground_acceleration: float
  frequency: float
  inclination: float
  soil_temperature: float
  soil_humidity: float

@dataclass
class WaterSensorMeasures(Measures):
  precipitation_rate: float
  daily_accumulated: float
  water_level: float
