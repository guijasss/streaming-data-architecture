package org.tcc2.streaming

import EventJsonSerializer.*
import JsonSerdes.serdeFor
import Helpers.flattenEvent

import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.kstream.{JoinWindows, KStream, Produced, StreamJoined, ValueJoiner}
import org.apache.kafka.streams.processor.api.{Processor, ProcessorContext, ProcessorSupplier, Record}
import org.apache.kafka.streams.state.{KeyValueStore, Stores}
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import play.api.libs.json.Json

import scala.compiletime.uninitialized
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
  private val pendingCheckoutsStoreName = "pending-checkouts-store"
  private val storeSupplier = Stores.keyValueStoreBuilder(
    Stores.persistentKeyValueStore(pendingCheckoutsStoreName),
    Serdes.String(),
    serdeFor[CheckoutStartEvent]
  )
  builder.addStateStore(storeSupplier)

  private val checkouts: KStream[String, CheckoutStartEvent] =
    eventsStream
      .filter((_, json) => parseEvent(json).exists(_.isInstanceOf[CheckoutStartEvent]))
      .mapValues(json => Json.parse(json).as[CheckoutStartEvent])
      .selectKey((_, event) => event.userId.toString)

  private val purchases: KStream[String, PurchaseEvent] =
    eventsStream
      .filter((_, json) => parseEvent(json).exists(_.isInstanceOf[PurchaseEvent]))
      .mapValues(json => Json.parse(json).as[PurchaseEvent])
      .selectKey((_, event) => event.userId.toString)

  eventsStream.foreach { (key, value) =>
    logger.info(s"Received event - key: $key, value: $value")
  }

  // Changed the window time to 30 minutes (1800 seconds)
  private val unmatchedCheckouts: KStream[String, CheckoutStartEvent] =
    checkouts
      .leftJoin(
        purchases,
        new ValueJoiner[CheckoutStartEvent, PurchaseEvent, CheckoutStartEvent] {
          override def apply(checkout: CheckoutStartEvent, purchase: PurchaseEvent): CheckoutStartEvent = {
            if (purchase == null) checkout else null
          }
        },
        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(10)),
        StreamJoined.`with`(
          Serdes.String(),
          serdeFor[CheckoutStartEvent],
          serdeFor[PurchaseEvent]
        )
      )
      .filter((_, checkout) => checkout != null)

  // Send abandoned checkouts to the target topic
  checkouts
    .process(new ProcessorSupplier[String, CheckoutStartEvent, String, CheckoutStartEvent] {
      override def get() = new AbandonedCheckoutProcessor()
    }, pendingCheckoutsStoreName)
    .to("abandoned-checkouts", Produced.`with`(Serdes.String(), serdeFor[CheckoutStartEvent]))

  // Additionally, handle purchase events to remove corresponding checkouts from store
  purchases.process(new ProcessorSupplier[String, PurchaseEvent, String, PurchaseEvent] {
    override def get(): Processor[String, PurchaseEvent, String, PurchaseEvent] = new Processor[String, PurchaseEvent, String, PurchaseEvent] {
      private var store: KeyValueStore[String, CheckoutStartEvent] = uninitialized

      override def init(context: ProcessorContext[String, PurchaseEvent]): Unit = {
        this.store = context.getStateStore(pendingCheckoutsStoreName).asInstanceOf[KeyValueStore[String, CheckoutStartEvent]]
      }

      override def process(record: Record[String, PurchaseEvent]): Unit = {
        // Remove the checkout entry when a purchase happens
        store.delete(record.key())
      }

      override def close(): Unit = {}
    }
  }, pendingCheckoutsStoreName)

  private val flattenedEvents: KStream[String, String] = eventsStream.mapValues(flattenEvent)
  flattenedEvents.to("user-events-flatten")

  private val topology = builder.build()
  println(topology.describe())
  private val streams: KafkaStreams = new KafkaStreams(topology, props)

  streams.cleanUp()

  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }
}