package API.controller

import API.data._
import API.metrics.{TweetMetrics, UserMetrics}

import API.model.{Filter, Tweet}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AnalyticsController {

  def apply(collection: Long): Future[Unit] = Future {

    val wordcounts = new mutable.HashSet[(String,String,Int,Long)]
    val userinteraction = new mutable.HashSet[(String,String,Long)]

    val processTweets = TweetDao.filteredTweets(Filter(collection)) map { tweets: Seq[Tweet] => tweets foreach {TweetDao update TweetMetrics(_,wordcounts,userinteraction)}}
    processTweets onFailure {case e: Throwable => e.printStackTrace()}
    processTweets onSuccess {case _ => {
      CollectionDao.step(collection)
      wordcounts synchronized {WordsDao.save(wordcounts).onComplete(_ => CollectionDao.step(collection))}
      userinteraction synchronized {InteractionDao.save(userinteraction).onComplete(_ => CollectionDao.step(collection))}
    }}

    val processUsers = UserDao.inCollection(collection) map {users: Seq[String] => UserDao.save(users map UserMetrics.apply)}
    processUsers onSuccess {case _ => CollectionDao.step(collection)}
  }
}
