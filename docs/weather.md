# 🌍 Real-Time Disaster Data with Weather.gov API

The **Weather.gov API** (from the National Weather Service, NWS) is a great source for **real-time weather and disaster data**. Unlike NOAA’s API, it’s **free**, doesn’t require an API key, and provides **live weather alerts, forecasts, and radar data**.  

---

## 🔹 Key Features of Weather.gov API

| Feature | API Endpoint | Example Use Case |
|---------|-------------|------------------|
| **Real-Time Weather Alerts** | `/alerts/active` | Get active **hurricane, flood, tornado, wildfire** alerts |
| **Forecast Data** | `/gridpoints/{office}/{gridX},{gridY}/forecast` | Get **7-day weather forecasts** for a location |
| **Current Weather Conditions** | `/stations/{station_id}/observations/latest` | Get **live temperature, wind, pressure** data |
| **Hurricane Tracking** | `/alerts/active?event=Hurricane` | Get **real-time hurricane warnings** |

---

## 🔹 Step 1: Get Real-Time Disaster Alerts

You can query **active weather alerts** (e.g., hurricanes, floods, tornadoes) in real time.

### Example: Fetch Current Disaster Alerts

```python
import requests

URL = "https://api.weather.gov/alerts/active"

response = requests.get(URL, headers={"User-Agent": "YourAppName"})

if response.status_code == 200:
    alerts = response.json()
    for alert in alerts["features"]:
        print(alert["properties"]["headline"])
        print(alert["properties"]["description"])
        print("-" * 50)
else:
    print("Error:", response.status_code, response.text)
```

✅ **Example output:**
```
🌪 Tornado Warning - Severe storms in Texas
   Take cover immediately.
--------------------------------------------------
🌊 Flash Flood Watch - Heavy rains in Florida
   Flooding expected in low-lying areas.
--------------------------------------------------
```

---

## 🔹 Step 2: Get a Location’s Forecast (Next 7 Days)

If you want **detailed weather forecasts** for a specific location, you need **grid coordinates** from the API.

### 1️⃣ Get the Nearest Weather Station for a Location
```python
LAT, LON = 40.7128, -74.0060  # New York City

url = f"https://api.weather.gov/points/{LAT},{LON}"
response = requests.get(url, headers={"User-Agent": "YourAppName"})

if response.status_code == 200:
    data = response.json()
    forecast_url = data["properties"]["forecast"]
    print("Forecast API URL:", forecast_url)
else:
    print("Error:", response.status_code, response.text)
```
✅ This gives you the **forecast API URL** for that location.

---

### 2️⃣ Fetch the 7-Day Forecast
```python
response = requests.get(forecast_url, headers={"User-Agent": "YourAppName"})

if response.status_code == 200:
    forecast = response.json()
    for period in forecast["properties"]["periods"]:
        print(f"{period['name']}: {period['detailedForecast']}")
else:
    print("Error:", response.status_code, response.text)
```
✅ **Example output:**
```
Monday: Sunny, with a high near 75°F.
Monday Night: Clear, with a low around 60°F.
Tuesday: Cloudy, chance of rain, high of 70°F.
```

---

## 🔹 Step 3: Stream Weather Alerts to Kafka

To process weather alerts in **real-time**, send them to **Kafka**.

### Example: Stream Alerts to Kafka
```python
from kafka import KafkaProducer
import json
import requests
import time

KAFKA_TOPIC = "weather-alerts"
KAFKA_BROKER = "localhost:9092"

producer = KafkaProducer(
    bootstrap_servers=KAFKA_BROKER,
    value_serializer=lambda v: json.dumps(v).encode("utf-8")
)

URL = "https://api.weather.gov/alerts/active"

while True:
    response = requests.get(URL, headers={"User-Agent": "YourAppName"})
    if response.status_code == 200:
        alerts = response.json()
        for alert in alerts["features"]:
            producer.send(KAFKA_TOPIC, value=alert["properties"])
            print("Sent:", alert["properties"]["headline"])
    
    time.sleep(60)  # Check for new alerts every 60 seconds
```

✅ **This will:**  
1. **Fetch real-time weather alerts** every 60 seconds.  
2. **Send them to Kafka** for processing.  

---

## 🔹 Step 4: Use Cases for Your Real-Time Data Platform

- **🚨 Disaster Early Warning System** → Automatically send SMS/email alerts for severe weather.  
- **📊 Real-Time Dashboard** → Visualize hurricanes, floods, and tornadoes on a map.  
- **📡 IoT Integration** → Trigger sirens, IoT devices, or traffic management systems based on alerts.  

---

## 🔹 Next Steps
Would you like help **storing the alerts in a database** or **visualizing them on a real-time dashboard**? 🚀
