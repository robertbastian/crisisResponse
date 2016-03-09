package API.model

import java.sql.Timestamp

case class Tweet (
   id: Option[Long],
   text: String,
   author: String,
   time: Timestamp,
   collection: Long,
   location: Option[Array[Double]] = None,
   sentiment: Option[Double] = None,
   recency: Option[Double] = None,
   corroboration: Option[Double] = None,
   proximity: Option[Double] = None
)