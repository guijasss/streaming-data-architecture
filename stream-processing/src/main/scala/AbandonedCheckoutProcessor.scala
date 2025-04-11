package org.tcc2.streaming

import scala.compiletime.uninitialized
import org.apache.kafka.streams.processor.api.{Processor, ProcessorContext, Record}
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore

import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, LocalDateTime, ZoneOffset}
import java.util.logging.Logger


class AbandonedCheckoutProcessor extends Processor[String, CheckoutStartEvent, String, CheckoutStartEvent] {
  private var context: ProcessorContext[String, CheckoutStartEvent] = uninitialized
  private var store: KeyValueStore[String, CheckoutStartEvent] = uninitialized
  private val pendingCheckoutsStoreName = "pending-checkouts-store"

  override def init(context: ProcessorContext[String, CheckoutStartEvent]): Unit = {
    this.context = context
    this.store = context.getStateStore(pendingCheckoutsStoreName).asInstanceOf[KeyValueStore[String, CheckoutStartEvent]]

    // Schedule punctuation every 30 seconds
    context.schedule(Duration.ofSeconds(5), PunctuationType.WALL_CLOCK_TIME, timestamp => {
      val checkoutIterator = store.all()
      try {
        while (checkoutIterator.hasNext) {
          val entry = checkoutIterator.next()
          val checkout = entry.value
          val checkoutTime: Instant = Instant.parse(checkout.timestamp)
          val currentTime: Instant = Instant.now().minus(Duration.ofHours(3))
          val timeSinceCheckout = java.time.Duration.between(checkoutTime, currentTime)

          val logger: Logger = Logger.getLogger(getClass.getName)
          logger.info(s"Checkout time: $checkoutTime, Current time: $currentTime, Elapsed: ${timeSinceCheckout.toSeconds} seconds")

          // If more than 30 minutes passed, forward as abandoned and remove from store
          if (timeSinceCheckout.toSeconds >= 30) {
            context.forward(new Record(entry.key, checkout, timestamp))
            store.delete(entry.key)
          }
        }
      } finally {
        checkoutIterator.close()
      }
    })
  }

  override def process(record: Record[String, CheckoutStartEvent]): Unit = {
    // Store the checkout event with a key that includes user id
    val key = record.key()
    store.put(key, record.value())
  }

  override def close(): Unit = {}
}
