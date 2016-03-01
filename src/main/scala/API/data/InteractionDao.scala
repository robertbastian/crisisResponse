package API.data

import API.data.DatabaseConnection.db
import API.model.Collection
import API.stuff.LoggableFuture._
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions

object InteractionDao {

  case class Word(text:String, weight:Int)

  private class WordsTable(tag: Tag) extends Table[(Long,String,Int,Long)](tag, "Words"){
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def word = column[String]("word")
    def count = column[Int]("count")
    def collection = column[Long]("collection")
    def * = (id,word,count,collection)
  }

  private val words = TableQuery[WordsTable]

  def save(collection: Long, map: Map[String,Int]): Future[Option[Int]] = db run (words  ++= map.map(e => (0L,e._1, e._2, collection))) thenLog s"Stored word counts for collection ${collection}"

  def getAll(collection: Long): Future[(Seq[Word],Seq[Word],Seq[Word])] = {
    db run words.filter(_.collection == collection).map(e => (e.word,e.count)).result map { tuples =>
      val hashtags = Seq.newBuilder[Word]
      val users = Seq.newBuilder[Word]
      val words = Seq.newBuilder[Word]
      for (tuple <- tuples){
        if (tuple._1.charAt(0) == '@')
          users += Word(tuple._1,tuple._2)
        else if (tuple._1.charAt(0) == '#')
          hashtags += Word(tuple._1,tuple._2)
        else
          words += Word(tuple._1,tuple._2)

      }
      (hashtags.result,users.result,words.result)
    }
  }
}
