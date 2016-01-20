package API.data

import API.model.User
import twitter4j.{TwitterFactory, User => TwitterUser}
import twitter4j.conf.ConfigurationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import API.stuff.LoggableFuture._

object TwitterConnection {

  private val cb = new ConfigurationBuilder();
  cb.setOAuthConsumerKey(sys.env("TWITTER_CONSUMER_KEY"))
    .setOAuthConsumerSecret(sys.env("TWITTER_CONSUMER_SECRET"))
    .setOAuthAccessToken(sys.env("TWITTER_OAUTH_ACCESS_TOKEN"))
    .setOAuthAccessTokenSecret(sys.env("TWITTER_OAUTH_ACCESS_TOKEN_SECRET"));

  private val factory = new TwitterFactory(cb.build())
  private def t = factory.getInstance()

  def getUser(user: User): Future[TwitterUser] = Future {
    t.users().showUser(user.name)
  } thenLog s"Getting ${user.name}'s twitter information"
}
