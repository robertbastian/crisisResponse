package View

import org.scalatra._
import org.scalatra.scalate.ScalateSupport

import scala.collection.JavaConversions._

class UiServer extends ScalatraServlet with ScalateSupport {

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

  get("/"){
    contentType = "text/html"
    layoutTemplate(
      "/WEB-INF/default.jade",
      "js" -> servletContext.getResourcePaths("/js").toList,
      "css" -> servletContext.getResourcePaths("styles").toList
    )
  }
}
