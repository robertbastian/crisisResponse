package API.model

case class User (
  name: String,
  collection: Long,
  var competence: Option[Double] = None,
  var popularity: Option[Double] = None
)