import tkinter as tk
from tkinter import ttk

from event_producers.environmental.entities import WeatherStation

UPDATE_INTERVAL = 1.0  # segundos

class WeatherStationUI(tk.Tk):
    def __init__(self, station):
        super().__init__()
        self.station: WeatherStation = station
        self.title("Estação Meteorológica")
        self.geometry("480x500")
        self.configure(bg="#ffffff")

        self.readings = {}
        self.labels = {}

        self._style_widgets()
        self._build_interface()
        self.update_readings()

    def _style_widgets(self):
        style = ttk.Style(self)
        style.configure("TButton", font=("Arial", 10), padding=5)
        style.configure("Title.TLabel", font=("Arial", 18, "bold"), background="#ffffff", foreground="#333")
        style.configure("Sensor.TLabel", font=("Arial", 12), background="#ffffff", foreground="#555")
        style.configure("Value.TLabel", font=("Courier", 12, "bold"), background="#f0f0f0", foreground="#000", relief="sunken")

    def _build_interface(self):
        ttk.Label(self, text="Medições Atuais", style="Title.TLabel").pack(pady=15)

        container = ttk.Frame(self, padding=10, style="Sensor.TLabel")
        container.pack(padx=20, pady=10, fill="both", expand=True)

        for i, (name, sensor) in enumerate(self.station.sensors.items()):
            row = ttk.Frame(container)
            row.pack(fill="x", pady=5)

            label_name = ttk.Label(row, text=name.replace("_", " ").capitalize(), style="Sensor.TLabel", width=20)
            label_name.pack(side="left", padx=5)

            btn_minus = ttk.Button(row, text="–", width=2, command=lambda n=name: self.adjust(n, -1.0))
            btn_minus.pack(side="left", padx=2)

            value_label = ttk.Label(row, text="--", style="Value.TLabel", width=8, anchor="center")
            value_label.pack(side="left", padx=5)
            self.labels[name] = value_label

            btn_plus = ttk.Button(row, text="+", width=2, command=lambda n=name: self.adjust(n, +1.0))
            btn_plus.pack(side="left", padx=2)

    def adjust(self, sensor_name, delta):
        sensor = self.station.sensors.get(sensor_name)
        if sensor:
            new_value = sensor.base_value + delta
            sensor.set_value(new_value)

    def update_readings(self):
        for name, sensor in self.station.sensors.items():
            self.labels[name].config(text=f"{sensor.read():.2f}")
            self.readings.update({
                name: sensor.read()
            })
        self.after(int(UPDATE_INTERVAL * 1000), self.update_readings)

# 🧪 Exemplo com sensores fictícios
if __name__ == "__main__":
    station = WeatherStation()
    app = WeatherStationUI(station)
    app.mainloop()
