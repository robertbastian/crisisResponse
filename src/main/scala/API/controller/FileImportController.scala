package API.controller

import java.io.{InputStream, StringReader}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentLinkedQueue
import API.stuff.FutureImplicits._
import API.controller.FileImportController.ImportFormat
import API.database._
import API.model.{Event, Tweet}
import API.remotes.Twitter
import com.github.marklister.collections.io._
import twitter4j._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.language.postfixOps

class FileImportController(e: Event,input: InputStream) extends ImportController(e) {
  createdEvent map { event =>

    var id = event.id.get << 32 // 2^32 tweets per import should be enough, Twitter API ids start at 2<<53
    val tweets = FileImportController.parse(Source.fromInputStream(input).getLines.mkString("\n")) map {
      case (text, author, time, lat, long) => id += 1; ImportFormat(id,text,author,time,lat,long)
    }
        finishedCollecting(tweets)

  } onFailure {case _ => createdEvent map {e => EventDao.delete(e.id) thenLog "Invalid CSV...deleting"}}
}

object FileImportController {
  // Dates, format 08/04/2015 13:41
  private val smd = new SimpleDateFormat("dd/MM/yy HH:mm")
  private implicit val ymd = new GeneralConverter[Date](smd.parse)
  case class ImportFormat(private val id: Long, private val text: String, private val author: String, private val time: Date, private val latitude: Double, private val longitude: Double) extends Status {

    override def getQuotedStatus: Status = null

    override def getPlace: Place = null

    override def isRetweet: Boolean = text.startsWith("RT")

    override def isFavorited: Boolean = false

    override def getFavoriteCount: Int = 0

    override def getCreatedAt: Date = new Date(time.getTime)

    override def getWithheldInCountries: Array[String] = Array.empty[String]

    override lazy val getUser: twitter4j.User = API.remotes.Twitter.user(author)

    override def getContributors: Array[Long] = Array.empty[Long]

    override def getRetweetedStatus: Status = null

    override def getInReplyToScreenName: String = null

    override def getLang: String = "en"

    override def isTruncated: Boolean = false

    override def getId: Long = id

    override def isRetweeted: Boolean = false

    override def getCurrentUserRetweetId: Long = -1

    override def isPossiblySensitive: Boolean = false

    override def getRetweetCount: Int = 0

    override def getGeoLocation: GeoLocation = if (latitude != 0) new GeoLocation(latitude,longitude) else null

    override def getInReplyToUserId: Long = -1

    override def getSource: String = "web"

    override def getText: String = text

    override def getInReplyToStatusId: Long = -1

    override def getScopes: Scopes = null

    override def isRetweetedByMe: Boolean = false

    override def getQuotedStatusId: Long = -1

    override def getHashtagEntities: Array[HashtagEntity] = Array.empty[HashtagEntity]

    override def getURLEntities: Array[URLEntity] = Array.empty[URLEntity]

    override def getSymbolEntities: Array[SymbolEntity] = Array.empty[SymbolEntity]

    override def getMediaEntities: Array[MediaEntity] = Array.empty[MediaEntity]

    override def getUserMentionEntities: Array[UserMentionEntity] = Array.empty[UserMentionEntity]

    override def getExtendedMediaEntities: Array[ExtendedMediaEntity] = Array.empty[ExtendedMediaEntity]

    override def compareTo(o: Status): Int = text compare o.getText

    override def getAccessLevel: Int = 0

    override def getRateLimitStatus: RateLimitStatus = null
  }
  def parse(s: String) = CsvParser[String,String,Date,Double,Double].parse(new StringReader(s), hasHeader = true)
}
