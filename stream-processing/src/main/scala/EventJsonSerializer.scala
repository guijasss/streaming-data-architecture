package org.tcc2.streaming

import play.api.libs.json.*
import play.api.libs.json.JsonNaming.SnakeCase

object EventJsonSerializer:
  // Configure JSON to use snake_case
  given JsonConfiguration = JsonConfiguration(SnakeCase)

  implicit val adImpressionFormat: OFormat[AdImpressionEvent] = Json.format[AdImpressionEvent]
  implicit val adClickFormat: OFormat[AdClickEvent] = Json.format[AdClickEvent]
  implicit val landingPageViewFormat: OFormat[LandingPageViewEvent] = Json.format[LandingPageViewEvent]
  implicit val addToCartFormat: OFormat[AddToCartEvent] = Json.format[AddToCartEvent]
  implicit val checkoutStartFormat: OFormat[CheckoutStartEvent] = Json.format[CheckoutStartEvent]
  implicit val purchaseFormat: OFormat[PurchaseEvent] = Json.format[PurchaseEvent]


  // Parses a JSON string into an Event
  def parseEvent(jsonString: String): Option[Event] =
    try
      val json = Json.parse(jsonString)
      (json \ "event_type").asOpt[String] match
        case Some("ad_impression") => json.asOpt[AdImpressionEvent]
        case Some("ad_click") => json.asOpt[AdClickEvent]
        case Some("landing_page_view") => json.asOpt[LandingPageViewEvent]
        case Some("add_to_cart") => json.asOpt[AddToCartEvent]
        case Some("checkout_start") => json.asOpt[CheckoutStartEvent]
        case Some("purchase") => json.asOpt[PurchaseEvent]
        case _ => None
    catch
      case e: Exception =>
        println(s"Error parsing JSON: ${e.getMessage}")
        None

  // Implicit Reads for the Event trait
  given Reads[Event] with
    def reads(json: JsValue): JsResult[Event] =
      (json \ "event_type").asOpt[String] match
        case Some("ad_impression") => json.validate[AdImpressionEvent]
        case Some("ad_click") => json.validate[AdClickEvent]
        case Some("landing_page_view") => json.validate[LandingPageViewEvent]
        case Some("add_to_cart") => json.validate[AddToCartEvent]
        case Some("checkout_start") => json.validate[CheckoutStartEvent]
        case Some("purchase") => json.validate[PurchaseEvent]
        case _ => JsError("Unknown event type")
