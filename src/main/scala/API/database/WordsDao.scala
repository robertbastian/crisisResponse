package API.database

import API.database.DatabaseConnection.db
import API.database.WordsDao.Word
import API.model.Filter
import API.stuff.LoggableFuture._
import slick.driver.PostgresDriver.api._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

object WordsDao {

  case class Word(text:String, kind: String, weight:Int, sentiments: Seq[Double], trustworthinesses: Seq[Double])

  private class WordsTable(tag: Tag) extends Table[(String,String,Long)](tag, "Words"){
    def word = column[String]("word")
    def kind = column[String]("type")
    def tweet = column[Long]("tweet")
    def * = (word,kind,tweet)
  }

  private val words = TableQuery[WordsTable]

  def save(set: mutable.Set[(String,String,Long)]): Future[Option[Int]] = db run (words  ++= set)

  def getAll(filter: Filter): Future[Seq[Seq[Word]]] = {
    val query = for {
      word <- words
      tweet <- TweetDao.filtered(filter)
      if word.tweet === tweet.id
    } yield (word.word,word.kind,(tweet.sentiment,tweet.proximity))

    db.run(query.result) map { selected  =>

      val names = Seq.newBuilder[Word]
      val hashtags = Seq.newBuilder[Word]
      val urls = Seq.newBuilder[Word]
      val rest = Seq.newBuilder[Word]

      def makeGradient(values: Seq[Double]): Seq[Double] = {
        val total = values.size.toDouble
        Seq(
          values.count(_ < 3.33) / total,
          values.count(_ > 6.66) / total
        )
      }

      val groupedWords = selected groupBy(_._1) map { case (word, aggregates) =>
        val (_,kinds,metrics) = aggregates.unzip3
        val kind = kinds.sortBy(-_.length).head // Shortest 'kind' dominates
        val sentiments = makeGradient(metrics.map(_._1))
        val trustworthinesses  = makeGradient(metrics.map(_._2))
        (kind match {
          case "LOCATION" | "ORGANIZATION" | "PERSON" => names
          case "HASHTAG" => hashtags
          case "URL" => urls
          case _ => rest
        }) += Word(word,kind,aggregates.length,sentiments,trustworthinesses)
      }
      Seq(hashtags.result,names.result,urls.result,rest.result)
    }
  }
}
