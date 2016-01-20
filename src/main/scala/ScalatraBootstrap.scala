import javax.servlet.ServletContext
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new View.Server, "/*")
    context.mount(new API.Server, "/api/*")
  }

  override def destroy(context: ServletContext): Unit = {
    API.data.DatabaseConnection.close
    super.destroy(context)
  }
}
