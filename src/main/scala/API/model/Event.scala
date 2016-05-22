package API.model

case class Event(
  id: Option[Long],
  name: String,
  lon: Double,
  lat: Double,
  time: Long,
  query: Option[String],
  status: Int = 0
)