from datetime import datetime
from typing import Any, DefaultDict, Dict, Protocol, TypedDict, Union, TypeVar
from dataclasses import dataclass

from event_producers.environmental.entities import SensorType, Region
from event_producers.environmental.measures import AirSensorMeasures, Measures

class OutputEvent(TypedDict):
  sensor_id: str
  type: str
  timestamp: str
  region: str
  measures: Dict[str, Union[int, float]]

@dataclass
class BaseEvent:
  sensor_id: str
  type: SensorType
  timestamp: datetime
  region: Region
  measures: Measures

  def serialize(self) -> OutputEvent:
    return {
      "sensor_id": self.sensor_id,
      "type": self.type.value,
      "timestamp": self.timestamp.isoformat() + "Z",
      "region": self.region,
      "measures": self.measures.to_dict()
    }

@dataclass
class AirSensorEvent(BaseEvent):
  measures: AirSensorMeasures
