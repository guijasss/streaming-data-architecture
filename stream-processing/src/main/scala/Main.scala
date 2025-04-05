package org.tcc2.streaming

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig, Topology}
import org.apache.kafka.streams.kstream.KStream
import play.api.libs.json.{JsError, JsSuccess, Json, JsonConfiguration}
import play.api.libs.json.JsonNaming.SnakeCase
import EventJsonSerializer.eventReads

import java.util.Properties

object Main {
  def main(args: Array[String]): Unit = {
    val topic = "user-events" // 👈 Replace with your actual Kafka topic name

    // 🔹 Kafka Streams configuration
    val props: Properties = {
      val p = new Properties()
      p.put(StreamsConfig.APPLICATION_ID_CONFIG, "event-consumer")
      p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092") // 👈 Change if needed
      p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      p
    }

    // 🔹 Create Kafka Streams builder
    val builder = new StreamsBuilder()

    // 🔹 Read from Kafka topic
    val stream: KStream[String, String] = builder.stream[String, String](topic)

    implicit val config: JsonConfiguration.Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

    // 🔹 Deserialize JSON and process events
    stream.foreach { (_, jsonString) =>
      Json.parse(jsonString).validate[Event] match {
        case JsSuccess(event, _) => println(s"✅ Received event: $event")
        case JsError(errors)     => println(s"❌ Failed to parse event: $errors, $jsonString")
      }
    }

    // 🔹 Build and start Kafka Streams
    val topology: Topology = builder.build()
    val streams = new KafkaStreams(topology, props)
    streams.start()

    println("🚀 Kafka Streams started. Listening for events...")
    while (streams.state().isRunningOrRebalancing) {
      Thread.sleep(1000)
    }

    // 🔹 Keep the application running
    sys.addShutdownHook {
      println("🛑 Stopping Kafka Streams...")
      streams.close()
    }
  }
}
