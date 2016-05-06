package API.model

import java.sql.Timestamp

case class Filter(
                   collection: Long,
                   // Unix MINUTES
                   time: Option[Seq[Long]] = None,
                   sentiment: Option[Seq[Double]] = None,
                   popularity: Option[Seq[Double]] = None,
                   corroboration: Option[Seq[Double]] = None,
                   competence: Option[Seq[Double]] = None,
                   locationBehaviour: Option[String] = None,
                   noRetweets: Boolean = false
                 )
