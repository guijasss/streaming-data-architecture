package org.tcc2.streaming

import play.api.libs.json.{JsObject, Json}

object Helpers {
  def flattenEvent(jsonStr: String): String = {
    val json = Json.parse(jsonStr).as[JsObject]

    val commonKeys = Set("event_id", "timestamp", "user_id", "event_type")
    val (commonFields, specificFields) = json.fields.partition { case (key, _) => commonKeys.contains(key) }

    val flattened = JsObject(commonFields) + ("event_data" -> JsObject(specificFields))
    Json.stringify(flattened)
  }
}
