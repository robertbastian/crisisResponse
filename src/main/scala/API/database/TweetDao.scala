package API.database

import java.sql.Timestamp

import API.database.DatabaseConnection._
import UserDao._
import API.model.{User, Filter, Tweet}
import API.stuff.FutureImplicits._
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
    def time = column[Long]("time")
    def event = column[Long]("event")
    def latitude = column[Option[Double]]("latitude")
    def longitude = column[Option[Double]]("longitude")
    def sentiment = column[Double]("sentiment")
    def recency = column[Double]("recency")
    def corroboration = column[Double]("corroboration")
    def proximity = column[Double]("proximity")

    def * = (id.?, text, author, time, event, latitude, longitude, sentiment, recency, corroboration, proximity) <> (Tweet.tupled, Tweet.unapply)
  }

  val tweets = TableQuery[TweetTable]

  def filtered(filter: Filter): Query[TweetTable, Tweet, Seq] = {
    def limit[A, B <: Table[A]](query: Query[B,A,Seq], selector: B => Rep[Double], limits: Seq[Double]): Query[B,A,Seq] =
      query.filter(c => selector(c) between(limits(0),limits(1)))
    // The Rep type doesn't play nicely with generics :(
    def limitL[A, B <: Table[A]](query: Query[B,A,Seq], selector: B => Rep[Long], limits: Seq[Long]): Query[B,A,Seq] =
      query.filter(c => selector(c) between(limits(0),limits(1)))

    var query = tweets.filter(_.event === filter.event)

    if (filter.locationBehaviour.isDefined && filter.locationBehaviour.get == "exclude")
      query = query.filter(t => t.longitude.isDefined)

    if (filter.noRetweets)
      query = query.filterNot(t => t.text like "RT%")

    if (filter.sentiment.isDefined)
      query = limit(query,(_:TweetTable).sentiment,filter.sentiment.get)

    if (filter.corroboration.isDefined)
      query = limit(query,(_:TweetTable).corroboration,filter.corroboration.get)

    if (filter.popularity.isDefined)
      query = query join limit(users,(_:UsersTable).popularity,filter.popularity.get) on (_.author === _.name) map {_._1}

    if (filter.competence.isDefined)
      query = query join limit(users,(_:UsersTable).competence,filter.competence.get) on (_.author === _.name) map {_._1}

    if (filter.time.isDefined)
      query = limitL(query,(_:TweetTable).time,filter.time.get.map(_ * 60))
    query
  }

  def get(id: Long): Future[Option[(Tweet, Option[User])]] = db run (tweets.filter(_.id === id) joinLeft UserDao.users on (_.author like _.name)).result.headOption thenLog s"Getting tweet $id"

  def get(filter: Filter): Future[Seq[Tweet]] = db run filtered(filter).result thenLog s"Getting all tweets with $filter"

  def byUser(user: String): Future[Seq[Tweet]] = db run tweets.filter(_.author === user).result thenLog s"Getting tweets by '$user'"

  def save(ts: Iterable[Tweet]): Future[Option[Int]] = db run (tweets ++= ts) thenLog s"Inserting ${ts.size} tweets into the database"

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
      all <- filtered(Filter(filter.event)).length.result
    } yield (selected, all)
  )

  def histogram(filter: Filter, attribute: String, buckets: Int = 10): Future[(Long,Long,Seq[Int])] = if (attribute == "time") histogramT(filter) else
    db run sql"""SELECT width_bucket(#$attribute,0.0,10.000000001,$buckets) as bucket, COUNT(*)
                  FROM "Tweet" t, "User" u
                  WHERE event = ${filter.event} AND t.author = u.name
                  GROUP BY bucket;""".as[(Int,Int)] map {raw => (0L,10L,(1 to buckets).map{i => raw.toMap.getOrElse(i, 0)})}

  def histogramT(filter: Filter): Future[(Long,Long,Seq[Int])] =
    db run sql"""SELECT MIN(time), MAX(time)+60 FROM "Tweet" t WHERE event = ${filter.event}""".as[(Long,Long)] flatMap { case Vector((min, max)) =>
      val buckets = ((max-min)/60).toInt
      db run sql"""SELECT width_bucket(time,$min,$max,$buckets) as bucket, COUNT(*)
                  FROM "Tweet" t, "User" u
                  WHERE event = ${filter.event} AND t.author = u.name
                  GROUP BY bucket;""".as[(Int,Int)] map {raw => (min.toLong/60,max.toLong/60,(1 to buckets).map{i => raw.toMap.getOrElse(i, 0)})}
    }
}
