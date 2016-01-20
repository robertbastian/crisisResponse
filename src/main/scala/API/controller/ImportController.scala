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

    val file = Source.fromInputStream(input).getLines().mkString("\n")
    val string = "Tweet Text,Account,Timestamp,Latitude,Longitude\n@piersmorgan @Teddy_Jenkins very sad that #WalterScott was murdered by the very people sworn to protect him,GrantColl,08/04/2015 13:41,55.844828,-4.295259\nI hope this isn't just a formality for a NOT GUILTY!!! The family needs a wrongful death lawsuit as well  https://t.co/Kl5opUK7c8,TriceAngela,08/04/2015 13:41,0,0\n\"RT @ajc: ATL attorney representing family of #WalterScott, shown in video being shot by S.C. officer. http://t.co/dmOfvI5TOz http://t.co/2S?\",MicSeanNYC,08/04/2015 13:41,0,0\n\"RT @TheObamaDiary: While the media scramble for mugshots of #WalterScott, this is him.\n\nRest in peace.\n\nhttp://t.co/XiDGjxFord http://t.co?\",_ItsTaylorTime,08/04/2015 13:41,0,0\n\"Disaster sex in a hospital\nWatch the video here\nhttp://t.co/2fJw71rDTc\n#WalterScott\n1042 http://t.co/ivt0QCXtH8\",CowpAlfonsa,08/04/2015 13:41,0,0\nRT @Bipartisanism: #FoxNews and #CNN are covering the  #WalterScott murder in very different ways: http://t.co/Z2Z9AK2DUT,Safri_mista,08/04/2015 13:41,0,0\nRT @halseymusic: Our society is on its way to being desensitized to the murder of innocent people by the law. There's a human behind the ha?,Heartbreaklz,08/04/2015 13:41,0,0\nRT @Bipartisanism: THIS FRESH PRINCE EPISODE AIRED 23 YEARS AGO. 23 YEARS AND NOTHING HAS CHANGED #WalterScott http://t.co/fkTYOU7Ctm,Fear_Of_God_27,08/04/2015 13:41,0,0\nThe #WalterScott murder proves that racist cops have one thing in mind and that's the extinction of the Black man,TheHOodRept____,08/04/2015 13:41,0,0\n\"RT @Bipartisanism: How to survive a police encounter:\n #WalterScott http://t.co/iVxR15u25K\",syedhonda,08/04/2015 13:41,0,0\n\"RT @JohnnyHeldt: Officer Michael Slager, charged in death of #WalterSCott, denied bond tonight http://t.co/MutjSZW5jK http://t.co/KU937tsis?\",xcrystalssx,08/04/2015 13:41,0,0\n@CharlieRoseShow Full HD VIDEO #WalterScott Shot 8x IN FUCKING BACK By SC Armed Pig #MichaelSlager #FTP #Ferguson https://t.co/5LOapBA2Qt,Marland_X,08/04/2015 13:41,0,0\nRT @km_mixedlove: The cop knew he was going to kill #WalterScott &amp; no remorse after he did it. He was more worried about not getting caught.,with_da_fro,08/04/2015 13:41,0,0\n\"RT @SavageComedy: Unarmed. Back turned. Still killed.\n\nDon't let anyone not see this tonight.\n\n#WalterScott http://t.co/PkUONbnC52\",charlesdd_,08/04/2015 13:41,0,0\n\"RT @FearDept: Judging by the video, everything was done by the book:\n? Suspect eliminated\n? Evidence planted\n? Police reports falsified\n#Wa?\",polyglot84,08/04/2015 13:41,0,0\n\"RT @TheObamaDiary: While the media scramble for mugshots of #WalterScott, this is him.\n\nRest in peace.\n\nhttp://t.co/XiDGjxFord http://t.co?\",lisadfwu,08/04/2015 13:41,0,0\n@ShaneClaiborne thank you for posting this picture. #WalterScott,tarshelb,08/04/2015 13:41,0,0\n"
    val inputReader = new StringReader(file)
    println((file diff string).getBytes("UTF-8").map("%02X" format _).mkString)

    // Import, then create the collection, any failure in these two will make the f, which
    // we can report to the user
    val f = for {
          raw <- Future { CsvParser(ImportFormat).parse(new StringReader(file), hasHeader = true) }
      coll_id <- CollectionDao.save(Collection(None,name,false))
    } yield (raw,coll_id)


    // This will happen in the background, as we render the page once f finishes
    // TODO make this a communicating actor for status reports
    f map { case (raw, coll_id) =>

      val users = raw.map(_.author).distinct.map(User(_,coll_id))
      users.foreach(UserMetrics.compute)

      val tweets = raw.map(i => Tweet(None,i.text,i.author,new Timestamp(i.time.getTime),coll_id,if (i.longitude == 0 && i.latitude == 0) None else Some(i.latitude,i.longitude)))
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
