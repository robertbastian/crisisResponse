package API.model

import java.sql.Timestamp

case class Tweet (
   id: Option[Long],
   text: String,
   author: String,
   time: Timestamp,
   collection: Long,
   latitude: Option[Double],
   longitude: Option[Double],
   sentiment: Double,
   recency: Double,
   corroboration: Double,
   proximity: Double
)