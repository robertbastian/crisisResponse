package API

import API.controller.ImportController
import API.data.{CollectionDao, TweetDao, TwitterConnection, UserDao}
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
    case e: Throwable => println(e.getMessage()); InternalServerError()
  }

  get("/tweet/:id") {
    async {
      TweetDao.get(params("id").toInt) map {
        case Some(t) => Ok(t)
        case None    => NonExistent()
      }
    }
  }

  get("/user/:name") {
    async {
      UserDao.get(params("name")) map {
        case Some(u) => TwitterConnection.getUser(u) map { _.getLocation :: List(u) }
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
      CollectionDao.delete(params("id").toInt) map { _ => Ok() }
    }
  }

  post("/collection/:name"){
    async {
      ImportController(fileParams("file").getInputStream, params("name")) map { id => Ok(id) }
    }
  }

  // JSON config
  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  // 404 shortcut for non-existent objects
  private def NonExistent() = NotFound("Element does not exist")

  // Mapping future-errors to InternalServerError and wrapping everything up for Scalatra
  private def async(f: => Future[Any]) = new AsyncResult {val is = f recover {case e => println(e.getMessage()); InternalServerError()}}

  // Defining ExecutionContext, TODO use actor pool?
  override protected implicit def executor: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  // Max file size: 10MB
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(10*1024*1024)))
}
