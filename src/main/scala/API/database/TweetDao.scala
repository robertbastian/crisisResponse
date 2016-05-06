package API.database

import java.sql.Timestamp

import API.database.DatabaseConnection.db
import API.model.{Filter, Tweet}
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._
import slick.lifted.Query

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

object TweetDao {

  class TweetTable(tag: Tag) extends Table[Tweet](tag, "Tweet") {
    def id = column[Long]("id", O.PrimaryKey)
    def text = column[String]("text")
    def author = column[String]("author")
    def time = column[Timestamp]("time")
    def collection = column[Long]("collection")
    def latitude = column[Option[Double]]("latitude")
    def longitude = column[Option[Double]]("longitude")
    def sentiment = column[Double]("sentiment")
    def recency = column[Double]("recency")
    def corroboration = column[Double]("corroboration")
    def proximity = column[Double]("proximity")

    def * = (id.?, text, author, time, collection, longitude, latitude, sentiment, recency, corroboration, proximity).shaped <> (Tweet.tupled, Tweet.unapply)
  }

  val tweets = TableQuery[TweetTable]

  def filtered(filter: Filter): Query[TweetTable, Tweet, Seq] = {
    var query = tweets.filter(_.collection === filter.collection)
    if (filter.locationBehaviour.isDefined && filter.locationBehaviour.get == "exclude")
      query = query.filter(t => t.longitude.isDefined)
    if (filter.noRetweets)
      query = query.filterNot(t => t.text like "RT%")
    if (filter.sentiment.isDefined)
      query = query.filter(t => t.sentiment >= filter.sentiment.get.head).filter(t => t.sentiment < filter.sentiment.get.apply(1))
    if (filter.corroboration.isDefined)
      query = query.filter(t => t.corroboration >= filter.corroboration.get.head).filter(t => t.corroboration < filter.corroboration.get.apply(1))
    if (filter.popularity.isDefined){
      query = for {
        ts <- query
        us <- UserDao.users.filter(u => u.popularity >= filter.popularity.get.head).filter(u => u.popularity < filter.popularity.get.apply(1))
        if ts.author === us.name
      } yield ts
    }
    if (filter.competence.isDefined){
      query = for {
        ts <- query
        us <- UserDao.users.filter(u => u.competence >= filter.competence.get.head).filter(u => u.competence < filter.competence.get.apply(1))
        if ts.author === us.name
      } yield ts
    }
    if (filter.time.isDefined)
      query = query.filter(t => t.time >= new Timestamp(filter.time.get.head.toLong*60000L))
                   .filter(t => t.time < new Timestamp(filter.time.get.apply(1).toLong*60000L))
    query
  }

  def get(id: Long): Future[Option[Tweet]] = db run tweets.filter(_.id === id).result.headOption thenLog s"Getting tweet $id"

  def tweetsBy(user: String): Future[Seq[Tweet]] = db run tweets.filter(_.author === user).result thenLog s"Getting tweets by '$user'"

  def filteredTweets(filter: Filter): Future[Seq[Tweet]] = db run filtered(filter).result thenLog s"Getting all tweets with $filter"

  def create(ts: Iterable[Tweet]): Future[Option[Int]] = db run (tweets ++= ts) thenLog s"Inserting ${ts.size} tweets into the database"

  def update(ts: Seq[Tweet]) = db run DBIO.sequence(ts map {t => tweets.filter(_.id === t.id) update t})

  def locations(f: Filter): Future[Seq[(Long, Double, Double)]] = f.locationBehaviour match {
    case Some("fromUser") => {
      val action = for {
        ts <- filtered(f)
        us <- UserDao.users
        if ts.author === us.name
        if ts.latitude.isDefined || us.latitude.isDefined
      } yield (ts,us)
      val result = db run action.result thenLog s"Locations for $f"
      result map { seq => seq map {case (t,u) => (t.id.get,t.longitude.getOrElse(u.longitude.get),t.latitude.getOrElse(u.latitude.get))}}
    }
    case _ => db run filtered(f.copy(locationBehaviour = Some("exclude"))).map({ t => (t.id, t.longitude.get, t.latitude.get) }).result thenLog s"Locations for $f"
  }

  def count(filter: Filter): Future[(Int,Int)] = db run (
    for {
      selected <- filtered(filter).length.result
      all <- filtered(Filter(filter.collection)).length.result
    } yield (selected, all)
  )

  def histogram(filter: Filter, attribute: String, buckets: Int): Future[(Long,Long,Seq[Int])] = {
    val dom: Seq[Long] = Array(0L,10L)
    db run sql"""SELECT width_bucket(#$attribute,${dom(0)},${dom(1)+.000000001},$buckets) as bucket, COUNT(*)
                  FROM "Tweet" t, "User" u
                  WHERE collection = ${filter.collection} AND t.author = u.name
                  GROUP BY bucket;""".as[(Int,Int)] map {raw => (dom(0),dom(1),(1 to buckets).map{i => raw.toMap.getOrElse(i, 0)})}
  }

  def histogramT(filter: Filter): Future[(Long,Long,Seq[Int])] =
    db run sql"""SELECT extract(epoch from MIN(time)), extract(epoch from MAX(time))+60 FROM "Tweet" t WHERE collection = ${filter.collection}""".as[(Long,Long)] flatMap { case Vector((min, max)) =>
      val buckets = ((max-min)/60).toInt
      db run sql"""SELECT width_bucket(extract(epoch from time),$min,$max,$buckets) as bucket, COUNT(*)
                  FROM "Tweet" t, "User" u
                  WHERE collection = ${filter.collection} AND t.author = u.name
                  GROUP BY bucket;""".as[(Int,Int)] map {raw => (min.toLong/60,max.toLong/60,(1 to buckets).map{i => raw.toMap.getOrElse(i, 0)})}
    }
}
