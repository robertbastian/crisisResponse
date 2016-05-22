package API.model

case class User (
  name: String,
  competence: Double,
  popularity: Double,
  latitude: Option[Double],
  longitude: Option[Double]
){
  def compare(that: Object) = that match {
    case u: User => u.name.compareToIgnoreCase(name)
    case _ => false
  }
}