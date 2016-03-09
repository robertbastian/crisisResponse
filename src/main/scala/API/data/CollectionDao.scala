package API.data

import API.data.DatabaseConnection.db
import API.model.Collection
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions

object CollectionDao {

  private class CollectionsTable(tag: Tag) extends Table[Collection](tag, "Collection"){
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def name = column[String]("name")
    def status = column[Int]("status")
    def * = (id.?,name,status) <> (Collection.tupled, Collection.unapply)
  }

  private val collections = TableQuery[CollectionsTable]

  def all: Future[Seq[Collection]] = db run collections.result thenLog s"Getting all collections"

  def create(name: String): Future[Long] = db run ((collections returning collections.map(_.id)) += Collection(None,name,0)) thenLog s"Inserting collection $name"

  def get(i: Long): Future[Option[Collection]] = db run collections.filter(_.id === i).result.headOption thenLog s"Getting collection $i"

  def delete(i: Long): Future[Int] = db run collections.filter(_.id === i).delete thenLog s"Removing collection $i"

  def step(collection: Long): Future[Int] = db run sqlu"""UPDATE Collection SET status = status + 1 WHERE id = ${collection}"""

}
