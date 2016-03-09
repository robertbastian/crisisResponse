package API

import API.controller.{AnalyticsController, ImportController}
import API.data.WordsDao.Word
import API.data._
import API.model.{Collection, Filter}
import org.json4s._
import org.scalatra._
import org.scalatra.json._
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

class Server extends ScalatraServlet
  with ContentEncodingSupport with JacksonJsonSupport with FutureSupport with FileUploadSupport {

  before() {
    contentType = formats("json")
  }

  error {
    case _: SizeConstraintExceededException => RequestEntityTooLarge("10MB limit")
    case e: Throwable => println(e.getMessage()); InternalServerError("Server error")
  }

  get("/tweet/:id") {
    async {
      TweetDao.get(params("id").toInt) map {
        case Some(t) => UserDao.get(t.author) map {
          case Some(u) => Ok(Seq(t,u))
          case None => InternalServerError("DB inconsistent")
        }
        case None    => NonExistent()
      }
    }
  }

  get("/user/:name") {
    async {
      UserDao.get(params("name")) map {
        case Some(u) => Ok(u)
        case None    => NonExistent()
      }
    }
  }

  get("/collection/?") {
    async {
      CollectionDao.all
    }
  }

  get("/collection/:id"){
    async {
      CollectionDao.get(params("id").toInt) map {
        case Some(c) => Ok(c)
        case None    => NonExistent()
      }
    }
  }

  delete("/collection/:id") {
    async {
      CollectionDao.delete(params("id").toInt) map { _ => Ok(true) }
    }
  }

  post("/collection/:name"){
    val input = fileParams("file").getInputStream
    async {
      CollectionDao.create(params("name")) map { collection =>
        ImportController(input, collection) match {
          case None => CollectionDao.delete(collection); BadRequest("Bad csv")
          case Some(f) => for { _ <- f; _ <- AnalyticsController(collection)} yield Unit; Created(collection)
        }
      }
    }
  }

  post("/count"){
    async{
      val filter = parsedBody.extract[Filter]
      TweetDao.count(filter) map { case (selected,all) =>
        ("selected",selected) ~
        ("all",all)
      }
    }
  }

  post("/histogram/:selector/:buckets"){
    async {
      val filter = parsedBody.extract[Filter]
      val x = params("selector") match {
        case "time" => TweetDao.histogramT(filter, params("selector"), params("buckets").toInt)
        case _ => TweetDao.histogram(filter, params("selector"), params("buckets").toInt)
      }
      x map { case (min, max, buckets) =>
        ("min", min) ~
          ("max", max) ~
          ("buckets", buckets)
      }
    }
  }

  post("/locations"){
    async {
      val filter = parsedBody.extract[Filter]
      TweetDao.locations(filter).map(_.map(r =>
          ("id" -> r._1) ~
          ("location" -> List(r._2,r._3))
      ))
    }
  }

  post("/wordcounts"){
    async {
      val filter = parsedBody.extract[Filter]
      WordsDao.getAll(filter) map {case (hashtags,users,words) =>
        def transform(w: Word) = ("text" -> w.text) ~ ("weight" -> w.weight) ~ ("html" -> ("class" -> w.kind) ~ ("sentiment" -> w.sentiment))

        ("hashtags" -> hashtags.map(transform)) ~
        ("users" -> users.map(transform)) ~
        ("words" -> words.map(transform))
      }
    }
  }

  post("/interactions"){
    val filter = parsedBody.extract[Filter]
    async {
      InteractionDao.getAll(filter) map { case interactions =>
        val allUsers = (interactions.map(_._1) ++ interactions.map(_._2)).distinct
        val nodes = Map(allUsers.zipWithIndex : _*)
        ("nodes" -> allUsers.map("name" -> _)) ~
        ("edges" -> interactions.map(t =>
            ("source" -> nodes.get(t._1).get) ~
            ("target" -> nodes.get(t._2).get) ~
            ("value" -> t._3))
        )
      }
    }
  }

  post("/interactions/:user"){
    val filter = parsedBody.extract[Filter]
    val u = params("user")
    async {
      InteractionDao.getBy(u,filter) map {tweets =>
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
