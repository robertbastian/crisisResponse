package API.database

import API.database.DatabaseConnection.db
import API.model.Event
import API.stuff.FutureImplicits._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global

object EventDao {

  private class EventsTable(tag: Tag) extends Table[Event](tag, "Event"){
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def name = column[String]("name")
    def status = column[Int]("status")
    def lon = column[Double]("longitude")
    def lat = column[Double]("latitude")
    def time = column[Long]("time")
    def query = column[Option[String]]("query")
    def * = (id.?,name,lon,lat,time,query,status) <> (Event.tupled, Event.unapply)
  }

  private val events = TableQuery[EventsTable]

  def list: Future[Seq[Event]] = db run events.sortBy(_.id).result thenLog s"Getting all events"

  def save(e: Event): Future[Event] = db run ((events returning events.map(_.id)) += e) map { id => e.copy(id = Some(id))} thenLog s"Inserting event ${e.name}"

  def get(i: Long): Future[Option[Event]] = db run events.filter(_.id === i).result.headOption thenLog s"Getting event $i"

  def delete(id: Option[Long]): Future[Int] = db run events.filter(_.id === id).delete thenLog s"Removing event $id"

  def updateStatus(e: Event): Future[Int] = db run sqlu"""UPDATE "Event" SET status = status + 1 WHERE id = ${e.id.get}"""
}
