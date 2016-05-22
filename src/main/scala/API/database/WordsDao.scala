package API.database

import API.database.DatabaseConnection.db
import API.model.Filter
import API.model.Types.Word
import slick.driver.PostgresDriver.api._
import UserDao.UsersTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

object WordsDao {

  private class WordsTable(tag: Tag) extends Table[Word](tag, "Words"){
    def word = column[String]("word")
    def kind = column[String]("type")
    def tweet = column[Long]("tweet")
    def * = (word,kind,tweet)
  }

  private val words = TableQuery[WordsTable]

  def save(ws: Iterable[(String,String,Long)]): Future[Option[Int]] = db run (words ++= ws)

  case class RichWord(text:String, kind: String, weight:Int, sentiments: Seq[Double], trustworthinesses: Seq[Double])
  def getAll(filter: Filter): Future[Seq[Seq[RichWord]]] = {
    val query = for {
      word <- words
      (tweet,user) <- TweetDao.filtered(filter) joinLeft UserDao.users on (_.author === _.name)
      if word.tweet === tweet.id
    } yield (word.word,word.kind,(tweet,user))

    db.run(query.result) map { selected  =>

      val names = Seq.newBuilder[RichWord]
      val hashtags = Seq.newBuilder[RichWord]
      val urls = Seq.newBuilder[RichWord]
      val rest = Seq.newBuilder[RichWord]

      def makeGradient(values: Seq[Double]): Seq[Double] = {
        val total = values.size.toDouble
        Seq(
          values.count(_ < 3.33) / total,
          values.count(_ > 6.66) / total
        )
      }

      val groupedWords = selected groupBy(_._1.toLowerCase) map { case (_, aggregates) =>
        val (_,kinds,tweetsAndUsers) = aggregates.unzip3
        val kind = kinds.sortBy(-_.length).head // Shortest 'kind' dominates
        val sentiments = makeGradient(tweetsAndUsers.map{case (tweet,_) => tweet.sentiment})
        val trustworthinesses  = makeGradient(tweetsAndUsers.map { case (tweet, user) =>
//          (tweet.corroboration + tweet.recency + tweet.proximity + user.map(_.popularity).getOrElse(0.0) + user.map(_.competence).getOrElse(0.0)) / 5
          (tweet.corroboration + tweet.recency + user.map(_.popularity).getOrElse(0.0) + user.map(_.competence).getOrElse(0.0)) / 4
        })
        (kind match {
          case "LOCATION" | "ORGANIZATION" | "PERSON" => names
          case "HASHTAG" => hashtags
          case "URL" => urls
          case _ => rest
        }) += RichWord(aggregates.head._1,kind,aggregates.length,sentiments,trustworthinesses)
      }
      Seq(hashtags.result,names.result,urls.result,rest.result)
    }
  }
}
