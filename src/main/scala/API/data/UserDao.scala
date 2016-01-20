package API.data

import API.data.DatabaseConnection.db
import API.model.User
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import API.stuff.LoggableFuture._

object UserDao {

  private class UsersTable(tag: Tag) extends Table[User](tag, "User"){
    def name = column[String]("name")
    def collection = column[Long]("collection")
    def competence = column[Option[Double]]("competence")
    def popularity = column[Option[Double]]("popularity")

    def * = (name,collection,competence,popularity) <> (User.tupled, User.unapply)
  }

  private val users = TableQuery[UsersTable]

  def get(name: String): Future[Option[User]] = db run users.filter(_.name === name).result.headOption thenLog s"Getting user '$name'"

  def save(u: User): Future[Int] = db run (users += u) thenLog s"Inserting one user into the database"

  def save(us: Seq[User]): Future[Option[Int]] = db run (users ++= us) thenLog s"Inserting ${us.size} users into the database"
}
