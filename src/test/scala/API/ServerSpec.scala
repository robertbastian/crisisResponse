package API

import org.scalatra.test.specs2._
import org.specs2.matcher.JsonMatchers

class ServerSpec extends ScalatraSpec with JsonMatchers { def is = s2"""

  GET /tweet/1 should return first tweet from database as JSON $t1
  GET /tweet/0 should give 404 $t3
  GET /user/abcnews4jbruce should return his user info as JSON $t2
  GET /user/definitely-not-a-valid-username must give 404 $t4
  GET /collection $t5
  GET /collection/1 $t6
  GET /collection/0 $t7

  """

  addServlet(classOf[Server], "/*")

  def t1 = get("/tweet/1") {
    status must be equalTo 200
    body must /("id" -> 1)
    body must /("text" -> "@piersmorgan @Teddy_Jenkins very sad that #WalterScott was murdered by the very people sworn to protect him")
    body must /("author" -> "grantcoll")
    body must */(-4.29525899887085)
  }

  def t3 = get("/tweet/0"){
    status must be equalTo 404
  }

  def t2 = get("/user/abcnews4jbruce"){
    status must be equalTo 200
    body must /("Mount Pleasant, SC")
    body must /#(1)/("name" -> "abcnews4jbruce")
  }

  def t4 = get("/user/definitely-not-a-valid-username"){
    status must be equalTo 404
  }

  def t5 = get("/collection"){
    status must be equalTo 200
  }

  def t6 = get("/collection/1"){
    status must be equalTo 200
  }

  def t7 = get("/collection/0"){
    status must be equalTo 404
  }
}
