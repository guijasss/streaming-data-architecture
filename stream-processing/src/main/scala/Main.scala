package org.tcc2.streaming

import EventJsonSerializer.*
import JsonSerdes.serdeFor

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.{JoinWindows, KStream, Produced, StreamJoined, ValueJoiner}
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import play.api.libs.json.Json

import java.time.Duration
import java.util.Properties
import java.util.logging.{Level, Logger}


object Main extends App {

  import Serdes.*

  private val logger: Logger = Logger.getLogger(getClass.getName)
  logger.setLevel(Level.INFO)

  private val topicName = "user-events"
  println(s"Topic name: '$topicName', length: ${topicName.length}")

  private val uniqueAppId = "abandoned-checkout-detector"

  private val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, uniqueAppId)
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    p.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass)
    p.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass)
    p
  }

  private val builder = new StreamsBuilder()
  private val eventsStream: KStream[String, String] = builder.stream[String, String](topicName)

  // Checkout Start
  private val checkouts: KStream[String, CheckoutStartEvent] =
    eventsStream
      .filter((_, json) => parseEvent(json).exists(_.isInstanceOf[CheckoutStartEvent]))
      .mapValues(json => Json.parse(json).as[CheckoutStartEvent])
      .selectKey((_, event) => event.userId.toString)

  // Purchase
  private val purchases: KStream[String, PurchaseEvent] =
    eventsStream
      .filter((_, json) => parseEvent(json).exists(_.isInstanceOf[PurchaseEvent]))
      .mapValues(json => Json.parse(json).as[PurchaseEvent])
      .selectKey((_, event) => event.userId.toString)

  eventsStream.foreach { (key, value) =>
    logger.info(s"Received event - key: $key, value: $value")
  }

  private val unmatchedCheckouts: KStream[String, CheckoutStartEvent] =
    checkouts
      .leftJoin(
        purchases,
        new ValueJoiner[CheckoutStartEvent, PurchaseEvent, CheckoutStartEvent] {
          override def apply(checkout: CheckoutStartEvent, purchase: PurchaseEvent): CheckoutStartEvent = {
            if (purchase == null) checkout else null
          }
        },
        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(60)),
        StreamJoined.`with`(
          Serdes.String(),
          serdeFor[CheckoutStartEvent],
          serdeFor[PurchaseEvent]
        )
      )
      .filter((_, checkout) => checkout != null)

  unmatchedCheckouts.to("abandoned-checkouts", Produced.`with`(Serdes.String(), serdeFor[CheckoutStartEvent]))

  private val topology = builder.build()
  println(topology.describe())
  private val streams: KafkaStreams = new KafkaStreams(topology, props)

  streams.cleanUp()

  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }
}
