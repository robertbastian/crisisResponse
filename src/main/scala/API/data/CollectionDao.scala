package API.data

import API.data.DatabaseConnection.db
import API.model.{Collection, User}
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import API.stuff.LoggableFuture._

object CollectionDao {

  private class CollectionsTable(tag: Tag) extends Table[Collection](tag, "Collection"){
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def name = column[String]("name")
    def ready = column[Boolean]("ready")

    def * = (id.?,name,ready) <> (Collection.tupled, Collection.unapply)
  }

  private val collections = TableQuery[CollectionsTable]

  def get(i: Long): Future[Option[Collection]] = db run collections.filter(_.id === i).result.headOption thenLog s"Getting collection $i"

  def delete(i: Long): Future[Int] = db run collections.filter(_.id === i).delete thenLog s"Removing collection $i"

  def all: Future[Seq[Collection]] = db run collections.result thenLog s"Getting all collections"

  def save(c: Collection): Future[Long] = db run ((collections returning collections.map(_.id)) += c) thenLog s"Inserting collection ${c.name}"

  def ready(id: Long) = db run collections.filter(_.id === id).map(_.ready).update(true) thenLog s"Setting collection $id to ready"


}
