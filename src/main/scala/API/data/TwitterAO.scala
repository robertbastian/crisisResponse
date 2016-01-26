package API.data

import API.model.User
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

object TwitterAO {

  private val cb = new ConfigurationBuilder();
  cb.setOAuthConsumerKey(sys.env("TWITTER_CONSUMER_KEY"))
    .setOAuthConsumerSecret(sys.env("TWITTER_CONSUMER_SECRET"))
    .setOAuthAccessToken(sys.env("TWITTER_OAUTH_ACCESS_TOKEN"))
    .setOAuthAccessTokenSecret(sys.env("TWITTER_OAUTH_ACCESS_TOKEN_SECRET"));

  private val factory = new TwitterFactory(cb.build())
  private def t = factory.getInstance()

  def location(user: User): String = t.users().showUser(user.name).getLocation

}
