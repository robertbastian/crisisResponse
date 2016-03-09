package API.data

import java.sql.Timestamp
import java.util.Date

import API.data.DatabaseConnection.db
import API.model.{Filter, Tweet, User}
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._
import slick.lifted.Query

import scala.collection.immutable.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

object TweetDao {

  class TweetTable(tag: Tag) extends Table[Tweet](tag, "Tweet") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def text = column[String]("text")
    def author = column[String]("author")
    def time = column[Timestamp]("time")
    def collection = column[Long]("collection")
    def latitude = column[Double]("latitude")
    def longitude = column[Double]("longitude")
    def sentiment = column[Option[Double]]("sentiment")
    def recency = column[Option[Double]]("recency")
    def corroboration = column[Option[Double]]("corroboration")
    def proximity = column[Option[Double]]("proximity")

    def toTweet(id: Option[Long], tx: String, at: String, ti: Timestamp, lo: Double, la: Double, te: Option[Double], re: Option[Double], co: Option[Double], pr: Option[Double], cl: Long): Tweet =
      Tweet(id, tx, at, ti,cl, if (la == 0 && lo == 0) None else Some(Array(lo, la)), te, re, co, pr)
    def toTuple(t: Tweet) = Some((t.id,t.text,t.author,t.time,t.location.map(_.apply(0)).getOrElse(0.0),t.location.map(_.apply(1)).getOrElse(0.0),t.sentiment,t.recency,t.corroboration,t.proximity,t.collection))

    def * = (id.?, text, author, time, longitude, latitude, sentiment, recency, corroboration, proximity,collection).shaped <> ((toTweet _).tupled, (toTuple _))
  }

  val tweets = TableQuery[TweetTable]

  def filtered(filter: Filter): Query[TweetTable, Tweet, Seq] = {
    var query = tweets.filter(_.collection === filter.collection)
    if (filter.hasLocation)
      query = query.filter(t => t.longitude =!= 0.0 && t.latitude =!= 0.0)
    if (filter.noRetweets)
      query = query.filterNot(t => t.text like "RT%")
    if (filter.sentiment.isDefined)
      query = query.filter(t => t.sentiment >= filter.sentiment.get.head).filter(t => t.sentiment < filter.sentiment.get.apply(1))
    if (filter.corroboration.isDefined)
      query = query.filter(t => t.corroboration >= filter.corroboration.get.head).filter(t => t.corroboration < filter.corroboration.get.apply(1))
//    if (filter.popularity.isDefined)
//      query = query.filter(t => t.popularity >= filter.popularity.get.head).filter(t => t.popularity < filter.popularity.get.apply(1))
//    if (filter.competence.isDefined)
//      query = query.filter(t => t.competence >= filter.competence.get.head).filter(t => t.competence < filter.competence.get.apply(1))
//    if (filter.time.isDefined)
//      query = query.filter(t => t.time >= filter.time.get.head*1000).filter(t => t.time < filter.time.get.apply(1)*1000)
    query
  }

  def get(id: Long): Future[Option[Tweet]] = db run tweets.filter(_.id === id).result.headOption thenLog s"Getting tweet $id"

  def tweetsBy(user: String): Future[Seq[Tweet]] = db run tweets.filter(_.author === user).result thenLog s"Getting tweets by '$user'"

  def filteredTweets(filter: Filter): Future[Seq[Tweet]] = db run filtered(filter).result thenLog s"Getting all tweets with $filter"

  def update(t: Tweet): Future[Int] = db run tweets.filter(_.id === t.id).update(t)

  def create(ts: Seq[Tweet]): Future[Option[Int]] = db run (tweets ++= ts) thenLog s"Inserting ${ts.size} tweets into the database"

  def locations(f: Filter): Future[Seq[(Long, Double, Double)]] = db run filtered(f.copy(hasLocation = true)).map(t => (t.id,t.longitude,t.latitude)).result thenLog s"Locations for ${f}"

  def count(filter: Filter): Future[(Int,Int)] = db run (
    for {
      selected <- filtered(filter).length.result
      all <- filtered(Filter(filter.collection)).length.result
    } yield (selected, all)
  )

  def histogram(filter: Filter, attribute: String, buckets: Int): Future[(Double,Double,Seq[Int])] =
    db run sql"""SELECT MIN(#$attribute), MAX(#$attribute) FROM "Tweet" t, "User" u WHERE collection = ${filter.collection} AND t.author = u.name;""".as[(Double,Double)] flatMap { case Vector((min, max)) => {val buckets = Math.ceil(max).toInt-Math.floor(min).toInt
      db run sql"""SELECT width_bucket(#$attribute,$min,${max+0.000000001},$buckets) as bucket, COUNT(*)
                  FROM "Tweet" t, "User" u
                  WHERE collection = 4 AND t.author = u.name
                  GROUP BY bucket;""".as[(Int,Int)] map {raw => (min,max,(1 to buckets).map{i => raw.toMap.getOrElse(i, 0)})}}
    }

  def histogramT(filter: Filter, attribute: String, buckets: Int): Future[(Double,Double,Seq[Int])] =
    db run sql"""SELECT extract(epoch from MIN(#$attribute)), extract(epoch from MAX(#$attribute)) FROM "Tweet" t, "User" u WHERE collection = ${filter.collection} AND t.author = u.name;""".as[(Double,Double)] flatMap { case Vector((min, max)) =>
      db run sql"""SELECT width_bucket(extract(epoch from #$attribute),$min,${max+1},$buckets) as bucket, COUNT(*)
                  FROM "Tweet" t, "User" u
                  WHERE collection = 4 AND t.author = u.name
                  GROUP BY bucket;""".as[(Int,Int)] map {raw => (min,max,(1 to buckets).map{i => raw.toMap.getOrElse(i, 0)})}
    }
}
