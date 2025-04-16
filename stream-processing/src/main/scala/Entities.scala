package org.tcc2.streaming

import play.api.libs.json.JsObject

trait Event {
  def eventId: String
  def timestamp: String
  def userId: Int
  def eventType: String
}

case class AdImpressionEvent(
  eventId: String,
  timestamp: String,
  userId: Int,
  campaignId: String,
  adPlatform: String,
  device: String,
  eventType: String = "ad_impression"
) extends Event

case class AdClickEvent(
  eventId: String,
  timestamp: String,
  userId: Int,
  campaignId: String,
  adPlatform: String,
  landingPage: String,
  eventType: String = "ad_click"
 ) extends Event

case class LandingPageViewEvent(
  eventId: String,
  timestamp: String,
  userId: Int,
  campaignId: String,
  landingPage: String,
  referrer: String,
  eventType: String = "landing_page_view"
) extends Event

case class AddToCartEvent(
  eventId: String,
  timestamp: String,
  userId: Int,
  productId: String,
  eventType: String = "add_to_cart"
) extends Event

case class CheckoutStartEvent(
  eventId: String,
  timestamp: String,
  userId: Int,
  cartItems: Array[String],
  eventType: String = "checkout_start",
  email: Option[String]
) extends Event

case class PurchaseEvent(
  eventId: String,
  timestamp: String,
  userId: Int,
  orderId: String,
  eventType: String = "purchase"
) extends Event

case class FlattenedEvent(
   event_id: String,
   timestamp: String,
   user_id: Int,
   event_type: String,
   event_data: JsObject
 )
