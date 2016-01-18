package API

import twitter4j._
import twitter4j.conf.ConfigurationBuilder

trait TwitterAPI {

  private val cb = new ConfigurationBuilder();
  cb.setDebugEnabled(true)
    .setOAuthConsumerKey(sys.env("TWITTER_CONSUMER_KEY"))
    .setOAuthConsumerSecret(sys.env("TWITTER_CONSUMER_SECRET"))
    .setOAuthAccessToken(sys.env("TWITTER_OAUTH_ACCESS_TOKEN"))
    .setOAuthAccessTokenSecret(sys.env("TWITTER_OAUTH_ACCESS_TOKEN_SECRET"));
  private val tf = new TwitterFactory(cb.build());
  val twitter = tf.getInstance();
}
