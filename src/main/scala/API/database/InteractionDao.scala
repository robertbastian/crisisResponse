package API.database

import API.database.DatabaseConnection.db
import API.model.Types.Interaction
import API.model.{Filter, Tweet, Types, User}
import API.stuff.FutureImplicits._
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.language.implicitConversions

object InteractionDao {
  class InteractionTable(tag: Tag) extends Table[Interaction](tag, "Interactions"){
    // USER NAMES ARE LOWER CASE ONLY!!1!
    def from = column[String]("from")
    def to = column[String]("to")
    def tweet = column[Long]("tweet")
    def * = (from,to,tweet)
  }

  val interactions = TableQuery[InteractionTable]

  def save(is: Iterable[(String,String,Long)]): Future[Option[Int]] = db run (interactions ++= is) thenLog s"Stored ${is.size} interactions"

  def unanlysedUsers: Future[Seq[String]] =  db run interactions.map(_.to).filterNot(_ in UserDao.users.map(_.name)).result

  def createGraph(f: Filter): Future[(Seq[Either[User, String]], Seq[(Int,Int)])] = db run interactions.filter(_.tweet in TweetDao.filtered(f).map(_.id)).result flatMap { ixs =>
    val userNames = (ixs.map(_._1) ++ ixs.map(_._2)).distinct
    db run UserDao.users.filter(_.name.toLowerCase inSet userNames).result map {users =>
      val userMap = users.map(u => u.name.toLowerCase -> u)(collection.breakOut).toMap
      val userIndices = userNames.zipWithIndex.toMap
      (
        userNames.map(n => userMap.get(n).toLeft(n)),
        ixs.map{ case (from,to,_) => (userIndices.getOrElse(from,-1),userIndices.getOrElse(to,-1))}
      )
    }
  } thenLog s"Getting interactions for $f"

  def withUser(u: String, f: Filter): Future[Seq[Tweet]] = db run (for {
    tweet <- TweetDao.filtered(f)
    interaction <- interactions if tweet.id === interaction.tweet
    if (interaction.from.toLowerCase === u.toLowerCase) || (interaction.to.toLowerCase === u.toLowerCase)
    } yield tweet).result thenLog s"Getting interactions by $u"

}
