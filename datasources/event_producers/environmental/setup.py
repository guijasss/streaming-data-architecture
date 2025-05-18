from random import choice
from random import randint
from typing import List, TypedDict

from event_producers.environmental.entities import REGIONS
from event_producers.environmental.sensors import AirSensor, GroundSensor, WaterSensor, BaseSensor


class SensorsEstimation(TypedDict):
  air: int
  water: int
  ground: int
  total_events: int

def _create_sensors(w: int, g: int, a: int) -> List[BaseSensor]:
  sensors = []

  for _ in range(w):
    sensors.append(WaterSensor(region=choice(REGIONS)))

  for _ in range(g):
    sensors.append(GroundSensor(region=choice(REGIONS)))

  for _ in range(a):
    sensors.append(AirSensor(region=choice(REGIONS)))

  return sensors


def _define_num_sensors(target_num_events: int, retries: int = 10000) -> SensorsEstimation:
  if target_num_events < 14:  # menor soma possível com w, g, a ≥ 1
      raise ValueError("O valor mínimo permitido é 14 (3w + 5g + 6a com w,g,a ≥ 1)")

  for _ in range(retries):
      w = randint(1, target_num_events // 3)
      remaining_w = target_num_events - 3 * w
      if remaining_w < 5 + 6:
          continue

      g = randint(1, remaining_w // 5)
      remaining = target_num_events - (3 * w + 5 * g)

      if remaining > 0 and remaining % 6 == 0:
        a = remaining // 6
        if a > 0:
          estimation: SensorsEstimation = {
            "air": a,
            "ground": g,
            "water": w,
            "total_events": 3*w + 5*g + 6*a
          }
          return estimation

  raise ArithmeticError(f"No solution found for 3w + 5g + 6a = {target_num_events}")


def setup_sensors(num_events: int) -> List[BaseSensor]:
  estimation: SensorsEstimation = _define_num_sensors(num_events)
  return _create_sensors(
    w=estimation["water"],
    a=estimation["air"],
    g=estimation["ground"]
  )
