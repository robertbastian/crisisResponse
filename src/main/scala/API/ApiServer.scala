package API

import API.model.TweetModel._
import API.model.UserModel._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json._
import slick.driver.MySQLDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ApiServer extends ScalatraServlet with ContentEncodingSupport with JacksonJsonSupport with Database with TwitterAPI {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  // Everything the API sends is JSON
  before() {
    contentType = formats("json")
  }

  get("/tweet"){
    Await.result(db.run(tweets.filter(_.id === 1L).result), Duration.Inf).head
  }

  get("/user"){
    val query = tweets.filter(_.id === 1L).joinLeft(users).on(_.author === _.id)
    val user: Option[User] = Await.result(db.run(query.result), Duration.Inf).head._2
    Map("info" -> user, "location" -> twitter.users().showUser(user.get.name).getLocation())
  }

  override def error(handler: ErrorHandler): Unit = Unit
}
