package API.model

import java.sql.Timestamp

case class Filter(
                   collection: Long,
                   time: Option[Seq[Double]] = None,
                   sentiment: Option[Seq[Double]] = None,
                   popularity: Option[Seq[Double]] = None,
                   corroboration: Option[Seq[Double]] = None,
                   competence: Option[Seq[Double]] = None,
                   hasLocation: Boolean = false,
                   noRetweets: Boolean = false
                 )
