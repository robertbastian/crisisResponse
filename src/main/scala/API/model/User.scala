package API.model

case class User (
  name: String,
  competence: Double,
  popularity: Double,
  latitude: Option[Double],
  longitude: Option[Double]
)