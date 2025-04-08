package org.tcc2.streaming

import play.api.libs.json.{JsError, JsResult, JsValue, Json, JsonConfiguration, OFormat, Reads, Writes}
import play.api.libs.json.JsonNaming.SnakeCase


object EventJsonSerializer:
  given JsonConfiguration = JsonConfiguration(SnakeCase)
  given OFormat[AdImpressionEvent] = Json.format[AdImpressionEvent]
  given OFormat[AdClickEvent] = Json.format[AdClickEvent]
  given OFormat[LandingPageViewEvent] = Json.format[LandingPageViewEvent]
  given OFormat[AddToCartEvent] = Json.format[AddToCartEvent]
  given OFormat[PurchaseEvent] = Json.format[PurchaseEvent]
  given OFormat[CheckoutStartEvent] = Json.format[CheckoutStartEvent]

  // Explicitly define parseEvent method
  def parseEvent(jsonString: String): Option[Event] =
    try
      val json = Json.parse(jsonString)

      // Manually parse the event based on event_type
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

  // Updated Reads implementation for Scala 3
  given Reads[Event] with
    def reads(json: JsValue): JsResult[Event] =
      (json \ "event_type").asOpt[String] match
        case Some("ad_impression")    => json.validate[AdImpressionEvent]
        case Some("ad_click")         => json.validate[AdClickEvent]
        case Some("landing_page_view") => json.validate[LandingPageViewEvent]
        case Some("add_to_cart")      => json.validate[AddToCartEvent]
        case Some("checkout_start")   => json.validate[CheckoutStartEvent]
        case Some("purchase")         => json.validate[PurchaseEvent]
        case _                        => JsError("Unknown event type")

  // Fixed Writes implementation for Scala 3
  given Writes[Event] with
    def writes(event: Event): JsValue = event match
      case e: AdImpressionEvent    => Json.toJson(e)(summon[OFormat[AdImpressionEvent]])
      case e: AdClickEvent         => Json.toJson(e)(summon[OFormat[AdClickEvent]])
      case e: LandingPageViewEvent => Json.toJson(e)(summon[OFormat[LandingPageViewEvent]])
      case e: AddToCartEvent       => Json.toJson(e)(summon[OFormat[AddToCartEvent]])
      case e: CheckoutStartEvent   => Json.toJson(e)(summon[OFormat[CheckoutStartEvent]])
      case e: PurchaseEvent        => Json.toJson(e)(summon[OFormat[PurchaseEvent]])
