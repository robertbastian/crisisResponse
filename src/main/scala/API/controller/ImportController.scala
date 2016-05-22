package API.controller

import API.database._
import API.database.EventDao.updateStatus
import API.metrics.{TweetMetrics, UserMetrics}
import API.model.{Types, Event, Tweet, User}
import API.remotes.Twitter
import API.stuff.FutureImplicits._
import Types.{Interaction, Word}
import twitter4j.Status

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ImportController(e: Event) {

  val createdEvent = EventDao.save(e)

  private def process(tweets: Iterable[Status], event: Event): Future[Int] = {
    val analyse = for {
      usermap <- UserMetrics(tweets.map(_.getUser))
      (tweets,users,words,interactions) <- TweetMetrics(event,tweets,usermap)
      additionalUsers <- UserMetrics(Twitter.users(interactions.map(_._2).filterNot(users.map(_.name).contains)))
    } yield (users ++ additionalUsers.values, tweets, words, interactions)

    val store = (r: (Iterable[User],Iterable[Tweet],Iterable[Word],Iterable[Interaction])) =>
      UserDao.save(r._1) >> TweetDao.save(r._2) >>  WordsDao.save(r._3) >> InteractionDao.save(r._4)

    (analyse >>= store) >> updateStatus(event)
  }

  protected def finishedCollecting(tweets: Iterable[Status]): Unit = createdEvent map { event =>
    updateStatus(event)
    process(tweets,event) onFailure {
      case e: Throwable => e.printStackTrace(); EventDao.delete(event.id) thenLog s"Failure analysing event ${event.name}...deleted"
    }
  }
}
