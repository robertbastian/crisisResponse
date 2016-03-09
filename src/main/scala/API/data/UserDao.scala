package API.data

import API.data.DatabaseConnection.db
import API.model.{Filter, User}
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions

object UserDao {

  private class UsersTable(tag: Tag) extends Table[User](tag, "User"){
    def name = column[String]("name")
    def competence = column[Double]("competence")
    def popularity = column[Double]("popularity")
    def * = (name,competence,popularity) <> (User.tupled, User.unapply)
  }

  private val users = TableQuery[UsersTable]

  def get(name: String): Future[Option[User]] = db run users.filter(_.name === name).result.headOption thenLog s"Getting user @$name"

  def inCollection(collection: Long): Future[Seq[String]] = db run TweetDao.filtered(Filter(collection = collection)).map(_.author).distinct.filterNot(_ in users.map(_.name)).result thenLog s"Getting user names for collection $collection"

  def save(us: Seq[User]): Future[Option[Int]] = db run (users ++= us) thenLog s"Stored ${us.size} new users"
}
