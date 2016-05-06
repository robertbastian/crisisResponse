package API.database

import API.database.DatabaseConnection.db
import API.model.{Tweet, Filter}
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.collection.mutable

object InteractionDao {
  private class InteractionTable(tag: Tag) extends Table[(String,String,Long)](tag, "Interactions"){
    def from = column[String]("from")
    def to = column[String]("to")
    def tweet = column[Long]("tweet")
    def * = (from,to,tweet)
  }

  private val interactions = TableQuery[InteractionTable]

  def save(set: mutable.Set[(String,String,Long)]): Future[Option[Int]] =
    db run (interactions ++= set.map{case (s1: String,s2: String,l: Long) => (s1.toLowerCase,s2.toLowerCase,l)}) thenLog s"Stored ${set.size} interactions"

  def getAll(f: Filter): Future[Seq[(String, String,Int)]] = db run
    (for {
      interaction <- interactions
      tweet <- TweetDao.filtered(f) if interaction.tweet === tweet.id
    } yield interaction)
    .groupBy(i => (i.from,i.to)).map{case ((from,to),a) => (from,to,a.length)}.result thenLog s"Getting interactions for $f"

  def getBy(s: String,f: Filter): Future[Seq[Tweet]] = db run (
    for {
      interaction <- interactions
      tweet <- TweetDao.filtered(f)
      if interaction.from === s.toLowerCase || interaction.to === s.toLowerCase
      if tweet.id === interaction.tweet
    } yield tweet).result thenLog s"Getting interactions by $s"

}
