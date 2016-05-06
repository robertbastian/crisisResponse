package API.controller

import API.database._
import API.metrics.{TweetMetrics, Helpers, UserMetrics}
import twitter4j.{Status, User}
import API.stuff.LoggableFuture._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ImportController(name: String, lon: Double, lat: Double, time: Long, query: Option[String]) {

  val collection = CollectionDao.create(name,lon,lat,time,query)

  protected def analyze(tweets: Iterable[Status]): Future[Unit] = collection flatMap {collection =>
    CollectionDao.step(collection)

    val users = tweets.map(_.getUser).filterNot(_ == null).toSet

    val wordcounts = new mutable.HashSet[(String,String,Long)]
    val userinteraction = new mutable.HashSet[(String,String,Long)]
    val tweetMetrics = new TweetMetrics(collection,wordcounts,userinteraction)
    val userMetrics = new UserMetrics

    val analyzing = for {
      users <- userMetrics.process(users) thenLog s"Analysed users"
      _ <- UserDao.save(users.values) thenLog s"Stored users"
      tweets <- tweetMetrics.process(tweets, users) thenLog s"Analysed ${tweets.size} tweets"
      _ <- TweetDao.create(tweets) flatMap {_ => for (_ <- WordsDao.save(wordcounts);_ <- InteractionDao.save(userinteraction)) yield ()} thenLog s"Stored tweets, words and interactions"
    } yield ()

    analyzing map { case () => CollectionDao.step(collection); println(s"Done analysing collection $collection")
    } recover { case e: Throwable => CollectionDao.delete(collection); e.printStackTrace(); println(s"Failure analysing collection $collection...deleting")}
  }
}
