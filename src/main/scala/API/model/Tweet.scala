package API.model

import java.sql.Timestamp

case class Tweet (
  id: Option[Long],
  text: String,
  author: String,
  time: Timestamp,
  collection: Long,
  location: Option[(Double,Double)] = None,
  var tension: Option[Double] = None,
  var recency: Option[Double] = None,
  var corroboration: Option[Double] = None,
  var proximity: Option[Double] = None
)