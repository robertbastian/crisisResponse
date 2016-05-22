package API.metrics

import API.remotes.GoogleMaps
import twitter4j.User
import API.model.{User => AUser}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Helpers._

object UserMetrics {

  def apply(users: Iterable[User]): Future[Map[Long,AUser]] = Future {

    def competence(user: User): Double = {
      if (isNewsAgency(user))
        10.0
      else if (isJournalist(user) && referencesNewsAgency(user) && user.isVerified)
        7.5
      else if (user.isVerified || isJournalist(user) || referencesNewsAgency(user))
        5.0
      else
        2.5
    }

    def popularity(user: User): Double = {
      val statuses = log(1.6299988975)(user.getStatusesCount) // [0,3.75]
      val followers = log(8.324526255)(user.getFollowersCount) // [0,6.25]
      statuses + followers
    }

    def location(user: User): (Option[Double],Option[Double]) = user.getLocation match {
      case "" => (None,None)
      case address => GoogleMaps.geocode(address) match {
        // Only consider valid if there's just one result and it's not at the poles
        case Array(loc) if Math.abs(loc.lat) < 70 => (
          // Add some randomness so markers don't overlap if people declare the same city
          Some(loc.lat + (Math.random()-.5)/100),
          Some(loc.lng + (Math.random()-.5)/500)
        )
        case _ => (None,None)
      }
    }

    users.map(user => {
      val (latitude, longitude) = location(user)
      user.getId -> AUser(
        user.getScreenName,
        restrictInterval(competence(user)),
        restrictInterval(popularity(user)),
        latitude,
        longitude
      )
    }).toMap
  }
}
