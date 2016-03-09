package API.metrics

import API.model.User

import scala.util.Random

object UserMetrics {

  //TODO
  val r = new Random()

  def apply(user: String): User = {
    User(
      user,
      r.nextGaussian()*2+5,
      r.nextGaussian()*2+5
    )
  }
}
