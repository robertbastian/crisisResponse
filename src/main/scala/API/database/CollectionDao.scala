package API.database

import API.database.DatabaseConnection.db
import API.model.Collection
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global

object CollectionDao {

  private class CollectionsTable(tag: Tag) extends Table[Collection](tag, "Collection"){
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def name = column[String]("name")
    def status = column[Int]("status")
    def lon = column[Double]("longitude")
    def lat = column[Double]("latitude")
    def time = column[Long]("time")
    def query = column[Option[String]]("query")
    def * = (id.?,name,status,lon,lat,time,query) <> (Collection.tupled, Collection.unapply)
  }

  private val collections = TableQuery[CollectionsTable]

  def all: Future[Seq[Collection]] = db run collections.sortBy(_.id).result thenLog s"Getting all collections"

  def create(name: String, lon: Double, lat: Double, time: Long, query: Option[String]): Future[Collection] = {
    val c = Collection(None, name, 0,lon,lat,time, query)
    val insert = db run ((collections returning collections.map(_.id)) += c)
    insert map {id => c.copy(id = Some(id))}
  } thenLog s"Inserting collection $name"

  def get(i: Long): Future[Option[Collection]] = db run collections.filter(_.id === i).result.headOption thenLog s"Getting collection $i"

  def delete(c: Collection): Future[Int] = db run collections.filter(_.id === c.id).delete thenLog s"Removing collection $c"

  def step(c: Collection): Future[Int] = db run sqlu"""UPDATE "Collection" SET status = status + 1 WHERE id = ${c.id.get}"""
}
