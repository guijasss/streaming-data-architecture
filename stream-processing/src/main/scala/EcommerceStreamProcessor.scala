package org.tcc2.streaming

package com.ecommerce.processor

import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.serialization.Serdes._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.state.{KeyValueStore, StoreBuilder, Stores}
import org.apache.kafka.streams.processor.{ProcessorContext, PunctuationType, Punctuator}

import java.time.{Duration, Instant}
import java.util.Properties
import java.time.temporal.ChronoUnit
import play.api.libs.json._

import scala.collection.mutable
import java.time.format.DateTimeFormatter

object EcommerceStreamProcessor {

  // Case class to represent user journey state
  case class UserJourneyState(
                               userId: Int,
                               adClicks: List[AdClickInfo] = List.empty,
                               purchases: List[PurchaseInfo] = List.empty,
                               lastUpdated: String = Instant.now().toString
                             )

  case class AdClickInfo(timestamp: String, campaignId: String)
  case class PurchaseInfo(timestamp: String, orderId: String)

  // JSON formatters for our state objects
  implicit val adClickInfoFormat: Format[AdClickInfo] = Json.format[AdClickInfo]
  implicit val purchaseInfoFormat: Format[PurchaseInfo] = Json.format[PurchaseInfo]
  implicit val userJourneyStateFormat: Format[UserJourneyState] = Json.format[UserJourneyState]

  // Notification for abandoned journey
  case class AbandonedJourneyNotification(
                                           userId: Int,
                                           campaignId: String,
                                           lastClickTime: String,
                                           notificationType: String = "abandoned_journey"
                                         )
  implicit val abandonedJourneyNotificationFormat: Format[AbandonedJourneyNotification] = Json.format[AbandonedJourneyNotification]

  private def createTopology(inputTopic: String, notificationTopic: String): StreamsBuilder = {
    val builder = new StreamsBuilder()

    // Create state store for maintaining user journey state
    val stateStoreBuilder: StoreBuilder[KeyValueStore[String, String]] =
      Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore("user-journey-store"),
        stringSerde,
        stringSerde
      )
    builder.addStateStore(stateStoreBuilder)

    // Read incoming events
    val ecommerceEvents: KStream[String, String] = builder.stream[String, String](inputTopic)

    // Process events and track user journeys
    ecommerceEvents
      .transformValues(
        () => new UserJourneyTracker(),
        "user-journey-store"
      )
      .filter((_, notification) => notification != null)
      .to(notificationTopic)

    builder
  }

  // Processor to track user journeys and detect abandoned carts
  private class UserJourneyTracker extends ValueTransformerWithKey[String, String, String] {
    private var context: ProcessorContext = unitialized
    private var stateStore: KeyValueStore[String, String] = unitialized
    private val abandonmentWindow = Duration.ofHours(1) // Time to wait before considering a journey abandoned

    override def init(context: ProcessorContext): Unit = {
      this.context = context
      this.stateStore = context.getStateStore("user-journey-store").asInstanceOf[KeyValueStore[String, String]]

      // Schedule periodic check for abandoned journeys
      context.schedule(
        Duration.ofMinutes(5),
        PunctuationType.WALL_CLOCK_TIME,
        new Punctuator {
          override def punctuate(timestamp: Long): Unit = checkForAbandonedJourneys()
        }
      )
    }

    override def transform(key: String, value: String): String = {
      try {
        val eventJson = Json.parse(value)
        val eventOpt = EventSerializers.readEvent(eventJson)

        eventOpt match {
          case Some(event) =>
            // Extract user ID from the event
            val userId = event.userId.toString

            // Get current journey state or create new one
            val journeyStateJson = Option(stateStore.get(userId))
            val journeyState = journeyStateJson match {
              case Some(json) => Json.parse(json).as[UserJourneyState]
              case None => UserJourneyState(event.userId)
            }

            // Update journey based on event type
            val updatedState = event match {
              case click: AdClickEvent =>
                val clickInfo = AdClickInfo(click.timestamp, click.campaignId)
                journeyState.copy(adClicks = journeyState.adClicks :+ clickInfo, lastUpdated = Instant.now().toString)

              case purchase: PurchaseEvent =>
                val purchaseInfo = PurchaseInfo(purchase.timestamp, purchase.orderId)
                journeyState.copy(purchases = journeyState.purchases :+ purchaseInfo, lastUpdated = Instant.now().toString)

              case _ => journeyState.copy(lastUpdated = Instant.now().toString)
            }

            // Save updated state
            stateStore.put(userId, Json.toJson(updatedState).toString)

            // Check if this is an abandoned journey that needs notification
            if (shouldSendAbandonedJourneyNotification(updatedState)) {
              val lastClick = updatedState.adClicks.last
              val notification = AbandonedJourneyNotification(
                userId = updatedState.userId,
                campaignId = lastClick.campaignId,
                lastClickTime = lastClick.timestamp
              )
              return Json.toJson(notification).toString
            }

            null // No notification needed

          case None =>
            println(s"Unknown event type: $value")
            null
        }
      } catch {
        case e: Exception =>
          println(s"Error processing event: ${e.getMessage}")
          null
      }
    }

    override def close(): Unit = {}

    // Check if user has clicked on ads but hasn't purchased within window
    private def shouldSendAbandonedJourneyNotification(state: UserJourneyState): Boolean = {
      if (state.adClicks.isEmpty || state.purchases.nonEmpty) {
        return false
      }

      // Get time of last ad click
      val lastClickTime = Instant.parse(state.adClicks.last.timestamp)
      val currentTime = Instant.now()

      // Check if abandonment window has passed
      val hoursSinceClick = ChronoUnit.HOURS.between(lastClickTime, currentTime)
      hoursSinceClick >= abandonmentWindow.toHours
    }

    // Periodically check all users for abandoned journeys
    private def checkForAbandonedJourneys(): Unit = {
      val iterator = stateStore.all()
      val usersToNotify = mutable.ArrayBuffer.empty[String]

      while (iterator.hasNext) {
        val entry = iterator.next()
        val userId = entry.key
        val state = Json.parse(entry.value).as[UserJourneyState]

        if (shouldSendAbandonedJourneyNotification(state)) {
          usersToNotify += userId
        }
      }
      iterator.close()

      // Process notifications for abandoned journeys
      usersToNotify.foreach { userId =>
        val state = Json.parse(stateStore.get(userId)).as[UserJourneyState]
        val lastClick = state.adClicks.last
        val notification = AbandonedJourneyNotification(
          userId = state.userId,
          campaignId = lastClick.campaignId,
          lastClickTime = lastClick.timestamp
        )

        // Forward notification to the output topic
        context.forward(userId, Json.toJson(notification).toString)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val props = new Properties()
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ecommerce-journey-processor")
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")

    val builder = createTopology("ecommerce-events", "abandoned-journey-notifications")
    val topology = builder.build()

    val streams = new KafkaStreams(topology, props)

    // Clean up state on shutdown
    Runtime.getRuntime.addShutdownHook(new Thread(() => streams.close()))

    streams.start()
  }
}