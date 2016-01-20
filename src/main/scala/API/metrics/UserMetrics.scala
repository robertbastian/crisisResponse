package API.metrics

import API.model.User

import scala.util.Random

object UserMetrics {

  val r = new Random()

  // TODO parallelize this
  def compute(u: User): Unit = {
    if (u.popularity.isEmpty)
      u.popularity = Some(popularity(u))

    if (u.competence.isEmpty)
      u.competence = Some(competence(u))
  }

  private def popularity(t: User): Double = r.nextGaussian()*2+5
  private def competence(t: User): Double = r.nextGaussian()*2+5
}
