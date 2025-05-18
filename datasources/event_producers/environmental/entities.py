from typing import Literal
from enum import Enum


class SensorType(Enum):
  Air = "air"
  Water = "water"
  Ground = "ground"

REGIONS = ["south-1", "south-2", "north-1", "north-2"]

Region = Literal["south-1", "south-2", "north-1", "north-2"]
