import javax.servlet.ServletContext

import API.ApiServer
import View.UiServer
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new UiServer, "/*")
    context.mount(new ApiServer, "/api/*")
  }
}
