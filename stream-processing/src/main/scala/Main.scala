package org.tcc2.streaming

import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.{Branched, KStream}
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}

import java.util.Properties
import scala.jdk.CollectionConverters.*
import play.api.libs.json.{JsValue, Json}

import java.util.concurrent.CountDownLatch
import java.util.logging.{Level, Logger}


object Main extends App {
  private val logger: Logger = Logger.getLogger(getClass.getName)
  logger.setLevel(Level.INFO)

  private val topicName = "user-events"
  println(s"Topic name: '$topicName', length: ${topicName.length}")

  // Create unique application ID to avoid offsets from previous runs
  private val uniqueAppId = s"event-processor-${System.currentTimeMillis()}"

  private val props: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, uniqueAppId)
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    p
  }

  logger.info(s"Starting Kafka Streams application with ID: $uniqueAppId")
  logger.info(s"Connecting to Kafka broker at: ${props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)}")

  private def parseEvent(jsonString: String): Option[Event] = {
    try {
      logger.info(s"Parsing JSON: ${jsonString.take(100)}...")

      val json = Json.parse(jsonString)
      val eventType = (json \ "event_type").asOpt[String]

      logger.info(s"Detected event type: $eventType")

      eventType match {
        case Some("checkout_start") =>
          try {
            val event = CheckoutStartEvent(
              eventId = (json \ "event_id").as[String],
              timestamp = (json \ "timestamp").as[String],
              userId = (json \ "user_id").as[Int],
              cartItems = (json \ "cart_items").as[Array[String]],
              email = (json \ "email").asOpt[String]
            )
            logger.info(s"Successfully parsed checkout event: ${event.eventId}")
            Some(event)
          } catch {
            case e: Exception =>
              logger.warning(s"Error parsing checkout event: ${e.getMessage}")
              None
          }

        case Some("purchase") =>
          try {
            val event = PurchaseEvent(
              eventId = (json \ "event_id").as[String],
              timestamp = (json \ "timestamp").as[String],
              userId = (json \ "user_id").as[Int],
              orderId = (json \ "order_id").as[String]
            )
            logger.info(s"Successfully parsed purchase event: ${event.eventId}")
            Some(event)
          } catch {
            case e: Exception =>
              logger.warning(s"Error parsing purchase event: ${e.getMessage}")
              None
          }

        case Some(other) =>
          logger.info(s"Ignoring event with unknown type: $other")
          None

        case None =>
          logger.warning("JSON doesn't contain event_type field")
          None
      }
    } catch {
      case e: Exception =>
        logger.warning(s"Failed to parse JSON: ${e.getMessage}")
        None
    }
  }

  // Build the topology
  private val builder = new StreamsBuilder()
  private val eventStream: KStream[String, String] = builder.stream[String, String](topicName)

  // Enhanced logging
  eventStream.foreach { (key, value) =>
    println(s"[DEBUG] RAW MESSAGE RECEIVED: key=$key, value=$value")
    logger.info(s"RAW MESSAGE: key=$key, value=$value")
  }

  // Parse events
  private val processedStream: KStream[String, Event] = eventStream.flatMapValues { jsonString =>
    val events = parseEvent(jsonString).toList
    logger.info(s"Parsed ${events.size} events from message")
    events.asJava
  }

  // Branch the stream
  private val branchedStreams = processedStream.split()
    .branch((_, event) => {
      val isCheckout = event.eventType == "checkout_start"
      if (isCheckout) logger.info(s"✅ Found checkout event: ${event.eventId}")
      isCheckout
    }, Branched.as("checkout"))
    .branch((_, event) => {
      val isPurchase = event.eventType == "purchase"
      if (isPurchase) logger.info(s"💰 Found purchase event: ${event.eventId}")
      isPurchase
    }, Branched.as("purchase"))
    .defaultBranch(Branched.as("others"))

  // Process each branch with null checks
  private val checkoutStartEvents = branchedStreams.get("checkout")
  if (checkoutStartEvents != null) {
    logger.info("Setting up checkout event processing")
    val checkoutByUser = checkoutStartEvents.selectKey((_, event) => event.userId.toString)
    checkoutByUser.foreach { (userId, event) =>
      logger.info(s"👤 Checkout event: User $userId started checkout at ${event.timestamp}")
    }
  } else {
    logger.warning("Checkout events branch is null")
  }

  private val purchaseEvents = branchedStreams.get("purchase")
  if (purchaseEvents != null) {
    logger.info("Setting up purchase event processing")
    val purchaseByUser = purchaseEvents.selectKey((_, event) => event.userId.toString)
    purchaseByUser.foreach { (userId, event) =>
      logger.info(s"💵 Purchase event: User $userId purchased at ${event.timestamp}")
    }
  } else {
    logger.warning("Purchase events branch is null")
  }

  // Build and start the Kafka Streams application
  private val topology = builder.build()
  logger.info("Built topology, preparing to start streams")
  logger.info(topology.describe().toString)

  private val streams = new KafkaStreams(topology, props)

  streams.setUncaughtExceptionHandler(ex => {
    logger.severe(s"Unhandled exception in Kafka Streams: ${ex.getMessage}")
    StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD
  })

  private val latch = new CountDownLatch(1)
  sys.addShutdownHook {
    logger.info("Shutdown hook triggered, closing streams")
    streams.close()
    latch.countDown()
  }

  try {
    // Clean up local state before starting
    streams.cleanUp()
    streams.start()
    logger.info("🚀 Kafka Streams started successfully! Listening for events...")

    // Report that we're ready
    logger.info(s"Application ID: ${props.getProperty(StreamsConfig.APPLICATION_ID_CONFIG)}")
    logger.info(s"Listening on topic: $topicName")

    latch.await()
  } catch {
    case e: Exception =>
      logger.severe(s"Failed to start Kafka Streams: ${e.getMessage}")
      e.printStackTrace()
  }
}
