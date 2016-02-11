package API.data

import java.sql.Timestamp

import API.data.DatabaseConnection.db
import API.model.{Tweet, User}
import API.stuff.LoggableFuture._
import slick.driver.MySQLDriver.api._
import sun.reflect.annotation.TypeAnnotation.LocationInfo

import scala.concurrent.Future
import scala.language.implicitConversions

object TweetDao {

  private class TweetTable(tag: Tag) extends Table[Tweet](tag, "Tweet") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def text = column[String]("text")
    def author = column[String]("author")
    def time = column[Timestamp]("time")
    def collection = column[Long]("collection")
    def latitude = column[Double]("latitude")
    def longitude = column[Double]("longitude")
    def tension = column[Option[Double]]("tension")
    def recency = column[Option[Double]]("recency")
    def corroboration = column[Option[Double]]("corroboration")
    def proximity = column[Option[Double]]("proximity")

    def toTweet(id: Option[Long], tx: String, at: String, ti: Timestamp, lo: Double, la: Double, te: Option[Double], re: Option[Double], co: Option[Double], pr: Option[Double], cl: Long): Tweet =
      Tweet(id, tx, at, ti,cl, if (la == 0 && lo == 0) None else Some(Array(lo, la)), te, re, co, pr)
    def toTuple(t: Tweet) = Some((t.id,t.text,t.author,t.time,t.location.map(_.apply(0)).getOrElse(0.0),t.location.map(_.apply(1)).getOrElse(0.0),t.tension,t.recency,t.corroboration,t.proximity,t.collection))

    def * = (id.?, text, author, time, longitude, latitude, tension, recency, corroboration, proximity,collection).shaped <> ((toTweet _).tupled, (toTuple _))
  }

  private val tweets = TableQuery[TweetTable]

  def get(id: Long): Future[Option[Tweet]] = db run tweets.filter(_.id === id).result.headOption thenLog s"Getting tweet $id"

  def tweetsBy(user: User): Future[Seq[Tweet]] = db run tweets.filter(_.author === user.name).result thenLog s"Getting tweets by '${user.name}'"

  def all: Future[Seq[Tweet]] = db run tweets.result thenLog s"Getting all tweets"

  def save(t: Tweet): Future[Int] = db run (tweets += t) thenLog s"Inserting one tweet into the database"

  def save(ts: Seq[Tweet]): Future[Option[Int]] = db run (tweets ++= ts) thenLog s"Inserting ${ts.size} tweets into the database"

  def locations(collection: Long): Future[Seq[(Long, Double, Double)]] = db run tweets.filter(t => t.collection === collection && t.longitude =!= 0.0 && t.latitude =!= 0.0).map(t => (t.id,t.longitude,t.latitude)).result thenLog s"Filtering tweets for collection ${collection}"

}
