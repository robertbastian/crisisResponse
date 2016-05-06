package API.metrics

import API.remotes.GoogleMaps
import twitter4j.User
import API.model.{User => AUser}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Helpers._

class UserMetrics {

  def process(users: Set[User]): Future[Map[Long,AUser]] = Future {

    def competence(user: User): Double = {
      if (isNewsAgency(user))
        10.0
      else 5.0
    }

    def popularity(user: User): Double = {
      inInterval(user.getFollowersCount / 1000)
    }

    def location(user: User): (Option[Double],Option[Double]) = user.getLocation match {
      case "" => (None,None)
      case address => GoogleMaps.geocode(address) match {
        case None => (None,None)
        // Add some randomness so markers don't overlap
        case Some(loc) => (Some(loc.lat + (Math.random()-.5)/100),Some(loc.lng + (Math.random()-.5)/500))
      }
    }

    users.map(user => {
      val (latitude, longitude) = location(user)
      (user.getId, AUser(
        user.getScreenName,
        competence(user),
        popularity(user),
        latitude,
        longitude)
        )
    }).toMap
  }
}
