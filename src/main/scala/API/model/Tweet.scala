package API.model

case class Tweet (
   id: Option[Long],
   text: String,
   author: String,
   time: Long,
   collection: Long,
   latitude: Option[Double],
   longitude: Option[Double],
   sentiment: Double,
   recency: Double,
   corroboration: Double,
   proximity: Double
)