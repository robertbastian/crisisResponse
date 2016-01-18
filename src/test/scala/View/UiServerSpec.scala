package View

import org.scalatra.test.specs2._
import org.specs2.Specification

class UiServerSpec extends Specification with BaseScalatraSpec { def is = s2"""

  GET / on UiServer should give 200        $t0
  GET / must contain assets                $t2
  GET /js/app.js should give 200           $t1

  """

  addServlet(classOf[UiServer], "/*")

  def t0 = get("/") {
    status should be equalTo 200
  }

  def t1 = get("/js/app.js") {
    status should be equalTo 200
  }

  def t2 = get("/"){
    body must contain("""src="/js/app.js"""")
  }
}
