package API.data

import API.data.DatabaseConnection.db
import API.model.{Filter, Collection}
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Set

object WordsDao {

  case class Word(text:String, kind: String, weight:Int, sentiment: Int)

  private class WordsTable(tag: Tag) extends Table[(String,String,Int,Long)](tag, "Words"){
    def word = column[String]("word")
    def kind = column[String]("type")
    def sentiment = column[Int]("sentiment")
    def tweet = column[Long]("tweet")
    def * = (word,kind,sentiment,tweet)
  }

  private val words = TableQuery[WordsTable]

  def save(set: Set[(String,String,Int,Long)]): Future[Option[Int]] = db run (words  ++= set) thenLog s"Stored word counts"

  def getAll(filter: Filter): Future[(Seq[Word],Seq[Word],Seq[Word])] = {
    val query = (for {
      word <- words
      tweet <- TweetDao.filtered(filter)
      if word.tweet === tweet.id
    } yield word).groupBy(w => w.word)
      .map{case (w,a) => (w,a.map(_.kind).max.get,a.length, a.map(_.sentiment).avg.get)}

    val hashtags = Seq.newBuilder[Word]
    val users = Seq.newBuilder[Word]
    val words_ = Seq.newBuilder[Word]

    db.stream(query.result).mapResult(Word.tupled).foreach(word => {
      word.text.charAt(0) match {
        case '@' => users += word
        case '#' => hashtags += word
        case _ => words_ += word
      }
    }) map { _ =>
      (hashtags.result,users.result,words_.result)
    }
  }
}
