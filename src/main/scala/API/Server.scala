package API

import java.io.FileInputStream

import API.controller.{FileImportController, StreamImportController}
import API.database.WordsDao.RichWord
import API.database._
import API.model.{Event, Filter}
import API.remotes.Twitter
import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra._
import org.scalatra.json._
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

class Server extends ScalatraServlet
  with ContentEncodingSupport with JacksonJsonSupport with FutureSupport with FileUploadSupport {

  before() {
    contentType = formats("json")
  }

  error {
    case _: SizeConstraintExceededException => RequestEntityTooLarge("10MB limit")
    case e: Throwable => println(e.getMessage); InternalServerError("Server error")
  }


  /** COLLECTION SCREEN **/

  get("/events") {
    async {
      EventDao.list
    }
  }

  get("/event/:id"){
    async {
      EventDao.get(params("id").toInt) map {
        case Some(event) => Ok(event)
        case None    => NonExistent()
      }
    }
  }

  delete("/event/:id") {
    async {
      EventDao.delete(Option(params("id").toLong)) map { _ => Ok(true) }
    }
  }

  post("/event/upload"){
    async {
      new FileImportController(parsedBody.extract[Event], fileParams("file").getInputStream).createdEvent map {Created(_)}
    }
  }

  post("/event/stream/start") {
    async {
      new StreamImportController(parsedBody.extract[Event]).createdEvent map {Created(_)}
    }
  }

  get("/event/stream/status"){
    async{
      StreamImportController.active map { ("count",_) }
    }
  }

  post("/event/stream/end"){
    StreamImportController.finish()
    true
  }

  get("/trends/:id"){
    params("id") match  {
      case "options" => Twitter.trendingOptions() map {e => ("name", e._1) ~ ("woeid", e._2)}
      case woeid: String => Twitter.trending(woeid.toInt)
    }
  }

  /** FILTER SCREEN **/

  post("/count"){
    async{
      TweetDao.count(parsedBody.extract[Filter]) map { case (selected,all) =>
        ("selected",selected) ~
        ("all",all)
      }
    }
  }

  post("/histogram/:selector"){
    async {
      TweetDao.histogram(parsedBody.extract[Filter],params("selector")) map { case (min, max, buckets) =>
        ("min", min) ~
        ("max", max) ~
        ("buckets", buckets)
      }
    }
  }

  /** VISUALISATION SCREEN **/

  post("/locations"){
    async {
      TweetDao.locations(parsedBody.extract[Filter]).map(_.map(r =>
          ("id" -> r._1.toString) ~ // Only way in JS to represent 53-bits or more
          ("location" -> List(r._2,r._3))
      ))
    }
  }

  get("/tweet/:id") {
    async {
      TweetDao.get(params("id").toLong) map {
        case Some((tweet,userOpt)) => Ok(Seq(tweet,userOpt))
        case None => NonExistent()
      }
    }
  }

  post("/words"){
    async {
      WordsDao.getAll(parsedBody.extract[Filter]) map {case Seq(hashtags,names,urls,rest) =>
        def transform(w: RichWord) = ("text" -> w.text) ~ ("weight" -> w.weight) ~ ("type" -> w.kind) ~ ("sentiments" -> w.sentiments) ~ ("trustworthinesses" -> w.trustworthinesses)
        ("hashtags" -> hashtags.map(transform)) ~
        ("names" -> names.map(transform)) ~
        ("urls" -> urls.map(transform)) ~
        ("words" -> rest.map(transform))
      }
    }
  }

  post("/interactions"){
    async {
      InteractionDao.createGraph(parsedBody.extract[Filter]) map { case (vertices,edges) =>
        ("edges" -> edges.groupBy(identity).map{
          case ((from,to),a) => ("source" -> from) ~ ("target" -> to) ~ ("value" -> a.size)
        }) ~
        ("nodes" -> vertices.map{
          case Left(user) => ("name" -> user.name) ~ ("popularity" -> user.popularity) ~ ("competence" -> user.competence)
          case Right(name) => ("name" -> name) ~ ("popularity" -> -1) ~ ("competence" -> -1)
        })
      }
    }
  }

  post("/interactions/:user"){
    val u = params("user")
    async {
      InteractionDao.withUser(u,parsedBody.extract[Filter]) map { tweets =>
        tweets.partition(_.author == u)
      }
    }
  }

  // JSON config
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  // 404 shortcut for non-existent objects
  private def NonExistent() = NotFound("Element does not exist")

  // Mapping future-errors to InternalServerError and wrapping everything up for Scalatra
  private def async(f: => Future[Any]) = new AsyncResult {val is = f }

  // Defining ExecutionContext, TODO use actor pool?
  override protected implicit def executor: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // Max file size: 10MB
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(10*1024*1024)))
}