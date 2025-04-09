  package org.tcc2.streaming

  import org.apache.kafka.common.serialization.{Deserializer, Serde, Serdes, Serializer}
  import play.api.libs.json.{Format, Json}

  object JsonSerdes {
    def serdeFor[T >: Null](using format: Format[T]): Serde[T] = {
      val serializer = new Serializer[T] {
        override def serialize(topic: String, data: T): Array[Byte] =
          Option(data).map(d => Json.toJson(d).toString().getBytes("UTF-8")).orNull
      }

      val deserializer = new Deserializer[T] {
        override def deserialize(topic: String, data: Array[Byte]): T =
          Option(data)
            .map(d => Json.parse(d).as[T])
            .orNull
      }

      Serdes.serdeFrom(serializer, deserializer)
    }
  }
