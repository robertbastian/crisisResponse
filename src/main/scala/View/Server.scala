package View

import java.io.File

import org.scalatra._

class Server extends ScalatraServlet {

  notFound {
    serveStaticResource() getOrElse resourceNotFound()
  }

  get("/"){
    contentType = "text/html"
    new File(servletContext.getRealPath("/views/index.html"))
  }
}
