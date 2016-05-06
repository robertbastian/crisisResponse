package API.model

case class Collection(
  id: Option[Long],
  name: String,
  status: Int,
  lon: Double,
  lat: Double,
  time: Long,
  query: Option[String]
)