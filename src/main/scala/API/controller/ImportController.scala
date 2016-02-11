package API.controller

import java.io.{StringReader, InputStream, InputStreamReader}
import java.sql.Timestamp
import java.util.Date

import API.data.{CollectionDao, TweetDao, UserDao}
import API.metrics.{TweetMetrics, UserMetrics}
import API.model.{Collection, Tweet, User}
import API.stuff.LoggableFuture._
import com.github.marklister.collections.io._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.language.postfixOps

object ImportController {

  // Dates, format 08/04/2015 13:41
  val sdf = new java.text.SimpleDateFormat("dd/MM/yy HH:mm")
  implicit val ymd = new GeneralConverter[Date](sdf.parse)
  case class ImportFormat(text: String, author: String, time: Date, latitude: Double, longitude: Double)

  def apply(input: InputStream, name: String): Future[Long] = {

    // Import, then create the collection, any failure in these two will fail f, which
    // we can report to the user
    val f = for {
          raw <- Future { CsvParser(ImportFormat).parse(new StringReader(Source.fromInputStream(input).getLines().mkString("\n")), hasHeader = true) } recover { case e => e.printStackTrace(); throw new IllegalArgumentException("Bad csv") }
      coll_id <- CollectionDao.save(Collection(None,name,false))
    } yield (raw,coll_id)


    // This will happen in the background, as we render the page once f finishes
    // TODO make this a communicating actor for status reports?
    f map { case (raw, coll_id) =>

      val users = raw.map(_.author.toLowerCase).distinct.map(User(_,coll_id))
      users.foreach(UserMetrics.compute)

      val tweets = raw.map(i => Tweet(None,i.text,i.author.toLowerCase,new Timestamp(i.time.getTime),coll_id,if (i.longitude == 0 && i.latitude == 0) None else Some(Array(i.longitude,i.latitude))))
      tweets.foreach(TweetMetrics.compute)

      (for {
        x <- UserDao.save(users)
        y <- TweetDao.save(tweets)
        z <- CollectionDao.ready(coll_id)
      } yield Unit) thenLog s"Saving new users & tweets into database, activating collection"
    }

    f map (_._2)
  }

}
