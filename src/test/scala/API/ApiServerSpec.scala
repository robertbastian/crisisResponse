package API

import org.scalatra.test.specs2._

class ApiServerSpec extends ScalatraSpec { def is = s2"""

  GET /tweet should return first tweet from database as JSON  $t1

  """

  addServlet(classOf[ApiServer], "/*")

  def t1 = get("/tweet") {
    body must be equalTo "[{\"id\":1,\"text\":\"@piersmorgan @Teddy_Jenkins very sad that #WalterScott was murdered by the very people sworn to protect him\"}]"
  }
}
