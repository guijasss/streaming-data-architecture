package org.tcc2.streaming

import play.api.libs.json._
import java.time.Instant

// Base trait for all events
sealed trait Event {
  def eventId: String
  def timestamp: String
  def userId: Int
  def eventType: String
}

// Ad Impression Event
case class AdImpressionEvent(
    eventId: String,
    timestamp: String,
    userId: Int,
    campaignId: String,
    adPlatform: String,
    device: String,
    eventType: String = "ad_impression"
) extends Event

// Ad Click Event
case class AdClickEvent(
   eventId: String,
   timestamp: String,
   userId: Int,
   campaignId: String,
   adPlatform: String,
   landingPage: String,
   eventType: String = "ad_click"
 ) extends Event

// Landing Page View Event
case class LandingPageViewEvent(
   eventId: String,
   timestamp: String,
   userId: Int,
   campaignId: String,
   landingPage: String,
   referrer: String,
   eventType: String = "landing_page_view"
 ) extends Event

// Add To Cart Event
case class AddToCartEvent(
   eventId: String,
   timestamp: String,
   userId: Int,
   productId: String,
   price: Double,
   currency: String,
   eventType: String = "add_to_cart"
 ) extends Event

// Checkout Start Event
case class CheckoutStartEvent(
   eventId: String,
   timestamp: String,
   userId: Int,
   cartItems: List[Map[String, Any]],
   totalAmount: Double,
   currency: String,
   paymentOptions: List[String],
   eventType: String = "checkout_start"
) extends Event

// Purchase Event
case class PurchaseEvent(
  eventId: String,
  timestamp: String,
  userId: Int,
  orderId: String,
  totalAmount: Double,
  currency: String,
  paymentMethod: String,
  eventType: String = "purchase"
) extends Event
