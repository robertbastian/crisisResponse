package API.model

case class Collection(
  id: Option[Long],
  name: String,
  ready: Boolean
)