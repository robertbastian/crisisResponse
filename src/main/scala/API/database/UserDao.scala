package API.database

import API.database.DatabaseConnection.db
import API.model.{Filter, User}
import API.stuff.LoggableFuture._
import slick.ast.ColumnOption.PrimaryKey
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions

object UserDao {

  class UsersTable(tag: Tag) extends Table[User](tag, "User"){
    def name = column[String]("name", PrimaryKey)
    def competence = column[Double]("competence")
    def popularity = column[Double]("popularity")
    def latitude = column[Option[Double]]("latitude")
    def longitude = column[Option[Double]]("longitude")
    def * = (name,competence,popularity,latitude,longitude) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[UsersTable]

  def get(name: String): Future[Option[User]] = db run users.filter(_.name === name).result.headOption thenLog s"Getting user @$name"

//  def notProcessedForCollection(collection: Long): Future[Seq[String]] = db run TweetDao.filtered(Filter(collection = collection)).map(_.author).distinct.filterNot(_ in users.map(_.name)).result thenLog s"Getting user names for collection $collection"

  def save(us: Iterable[User]): Future[Iterable[Int]] = db run DBIO.sequence(us.map(users.insertOrUpdate)) thenLog s"Stored ${us.size} new users"
}
