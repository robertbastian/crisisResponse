package API.model

case class Filter(
   event: Long,
   time: Option[Seq[Long]] = None, // Unix MINUTES
   sentiment: Option[Seq[Double]] = None,
   popularity: Option[Seq[Double]] = None,
   corroboration: Option[Seq[Double]] = None,
   competence: Option[Seq[Double]] = None,
   locationBehaviour: Option[String] = None,
   noRetweets: Boolean = false
 )
