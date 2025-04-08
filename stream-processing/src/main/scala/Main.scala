package org.tcc2.streaming

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}

import scala.jdk.CollectionConverters.*
import java.util.Properties
import java.time.Duration
import java.util.logging.{Level, Logger}


object Main extends App {

  private val logger: Logger = Logger.getLogger(getClass.getName)
  logger.setLevel(Level.INFO)

  private val topicName = "user-events"
  println(s"Topic name: '$topicName', length: ${topicName.length}")

  private val uniqueAppId = "event-processor"

  private val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, uniqueAppId)
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass)
    p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass)
    p
  }

  private val builder = new StreamsBuilder()
  private val eventStream: KStream[String, String] = builder.stream[String, String](topicName)

  eventStream.foreach { (key, value) =>
    logger.info(s"Received event - key: $key, value: $value")
  }

  private val topology = builder.build()
  println(topology.describe())
  private val streams: KafkaStreams = new KafkaStreams(topology, props)

  streams.cleanUp()

  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }
}
