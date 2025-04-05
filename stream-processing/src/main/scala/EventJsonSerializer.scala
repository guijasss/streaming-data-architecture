package org.tcc2.streaming

import play.api.libs.json.{Json, JsError, JsValue, JsonConfiguration, OFormat, Reads, Writes}
import play.api.libs.json.JsonNaming.SnakeCase

object EventJsonSerializer {

  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit val adImpressionFormat: OFormat[AdImpressionEvent] = Json.format[AdImpressionEvent]
  implicit val adClickFormat: OFormat[AdClickEvent] = Json.format[AdClickEvent]
  implicit val landingPageViewFormat: OFormat[LandingPageViewEvent] = Json.format[LandingPageViewEvent]
  implicit val addToCartFormat: OFormat[AddToCartEvent] = Json.format[AddToCartEvent]
  implicit val purchaseFormat: OFormat[PurchaseEvent] = Json.format[PurchaseEvent]
  implicit val checkoutStartFormat: OFormat[CheckoutStartEvent] = Json.format[CheckoutStartEvent]

  // 🔹 Custom Reads/Writes for CheckoutStartEvent
  // Custom Reads: Deserialize JSON to correct case class
  implicit val eventReads: Reads[Event] = (json: JsValue) => {
    (json \ "event_type").asOpt[String] match {
      case Some("ad_impression")    => json.validate[AdImpressionEvent]
      case Some("ad_click")         => json.validate[AdClickEvent]
      case Some("landing_page_view") => json.validate[LandingPageViewEvent]
      case Some("add_to_cart")      => json.validate[AddToCartEvent]
      case Some("checkout_start")   => json.validate[CheckoutStartEvent]
      case Some("purchase")         => json.validate[PurchaseEvent]
      case _                        => JsError("Unknown event type")
    }
  }

  // Custom Writes: Serialize case classes into JSON
  implicit val eventWrites: Writes[Event] = Writes {
    case e: AdImpressionEvent    => Json.toJson(e)(adImpressionFormat)
    case e: AdClickEvent         => Json.toJson(e)(adClickFormat)
    case e: LandingPageViewEvent => Json.toJson(e)(landingPageViewFormat)
    case e: AddToCartEvent       => Json.toJson(e)(addToCartFormat)
    case e: CheckoutStartEvent   => Json.toJson(e)(checkoutStartFormat)
    case e: PurchaseEvent        => Json.toJson(e)(purchaseFormat)
  }
}
