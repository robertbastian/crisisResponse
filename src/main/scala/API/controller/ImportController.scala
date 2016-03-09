package API.controller

import java.io.{InputStream, StringReader}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date

import API.data._
import API.model.{Tweet, User}
import com.github.marklister.collections.io._

import scala.concurrent.Future
import scala.io.Source
import scala.language.postfixOps

object ImportController {

  // Dates, format 08/04/2015 13:41
  private val smd = new SimpleDateFormat("dd/MM/yy HH:mm")
  private implicit val ymd = new GeneralConverter[Date](smd.parse)

  case class ImportFormat(text: String, author: String, time: Date, latitude: Double, longitude: Double)

  def apply(input: InputStream, collection: Long): Option[Future[Option[Int]]] =
    try {
      val raw = CsvParser(ImportFormat).parse(
        new StringReader(
          Source.fromInputStream(input).getLines().mkString("\n")),
        hasHeader = true
      )
      Some(TweetDao.create(
        raw.filter(_.text.nonEmpty)
        .map(i => Tweet(
          None,
          i.text,
          i.author.toLowerCase,
          new Timestamp(i.time.getTime),
          collection,
          if (i.longitude == 0 && i.latitude == 0) None else Some(Array(i.longitude, i.latitude))
        ))
      ))
    }
    catch {
      case e: Throwable => println(e); None
    }
}
