package API.remotes

import java.util
import java.util.Date

import twitter4j._
import twitter4j.conf.ConfigurationBuilder

import scala.collection.JavaConversions._

object Twitter {

  private val config = {
    val cb = new ConfigurationBuilder()
    cb.setOAuthConsumerKey(sys.env("TWITTER_CONSUMER_KEY"))
      .setOAuthConsumerSecret(sys.env("TWITTER_CONSUMER_SECRET"))
      .setOAuthAccessToken(sys.env("TWITTER_OAUTH_ACCESS_TOKEN"))
      .setOAuthAccessTokenSecret(sys.env("TWITTER_OAUTH_ACCESS_TOKEN_SECRET"))
    cb.build()
  }
  
  private val twitterFactory = new TwitterFactory(config)
  private val streamFactory = new TwitterStreamFactory(config)

  // THIS IS BLOCKING, ONLY CALLS IN FUTURES!
  def user(name: String): User = {
    try {
      twitterFactory.getInstance.users().showUser(name)
    }
    catch {
      case e: TwitterException if e.exceededRateLimitation => {
        println(s"RATE LIMIT EXCEEDED, SLEEPING ${e.getRateLimitStatus.getSecondsUntilReset} seconds")
        Thread.sleep(e.getRateLimitStatus.getSecondsUntilReset * 1000)
        user(name)
      }
      case _: Throwable => new User {
        override def getBiggerProfileImageURL: String = null

        override def isProtected: Boolean = false

        override def isTranslator: Boolean = false

        override def getProfileLinkColor: String = null

        override def getProfileImageURL: String = null

        override def getProfileBannerIPadRetinaURL: String = null

        override def getMiniProfileImageURLHttps: String = null

        override def getProfileSidebarFillColor: String = null

        override def getScreenName: String = "INACCESSIBLE"

        override def getListedCount: Int = 0

        override def getOriginalProfileImageURLHttps: String = null

        override def isProfileBackgroundTiled: Boolean = false

        override def isProfileUseBackgroundImage: Boolean = false

        override def getUtcOffset: Int = 0

        override def getProfileSidebarBorderColor: String = null

        override def isContributorsEnabled: Boolean = false

        override def getTimeZone: String = null

        override def getName: String = null

        override def getCreatedAt: Date = new Date()

        override def getDescriptionURLEntities: Array[URLEntity] = Array()

        override def getWithheldInCountries: Array[String] = Array()

        override def getURL: String = null

        override def getLang: String = null

        override def getId: Long = 0

        override def getProfileImageURLHttps: String = null

        override def getStatus: Status = null

        override def isDefaultProfileImage: Boolean = false

        override def getMiniProfileImageURL: String = null

        override def isDefaultProfile: Boolean = false

        override def getDescription: String = null

        override def getProfileBannerRetinaURL: String = null

        override def getFollowersCount: Int = 0

        override def isGeoEnabled: Boolean = false

        override def getURLEntity: URLEntity = null

        override def getProfileBackgroundColor: String = null

        override def isFollowRequestSent: Boolean = false

        override def getProfileBannerMobileURL: String = null

        override def getFavouritesCount: Int = 0

        override def getProfileBannerURL: String = null

        override def getProfileBackgroundImageUrlHttps: String = null

        override def getProfileBackgroundImageURL: String = null

        override def isVerified: Boolean = false

        override def getLocation: String = ""

        override def getFriendsCount: Int = 0

        override def getProfileBannerMobileRetinaURL: String = null

        override def getProfileTextColor: String = null

        override def getStatusesCount: Int = 0

        override def isShowAllInlineMedia: Boolean = false

        override def getProfileBannerIPadURL: String = null

        override def getOriginalProfileImageURL: String = null

        override def getBiggerProfileImageURLHttps: String = null

        override def compareTo(o: User): Int = 0

        override def getAccessLevel: Int = 0

        override def getRateLimitStatus: RateLimitStatus = null
      }
    }
  }


  def trending(woeid: Int): Seq[String] = {
    twitterFactory.getInstance.trends.getPlaceTrends(woeid).getTrends.map(_.getName)
  }

  def trendingOptions(): Seq[(String,Int)] = {
    twitterFactory.getInstance.trends().getAvailableTrends map {loc: Location => (loc.getName,loc.getWoeid)} sortBy(_._1)
  }


  def startStream(query: FilterQuery, callback: Status => Any): TwitterStream = {
    val tstream = streamFactory.getInstance

    tstream.addListener(new StatusListener(){
      override def onStatus(status: Status): Unit = callback(status)
      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {}
      override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {}
      override def onException(ex: Exception): Unit = ex.printStackTrace()
      override def onStallWarning(warning: StallWarning): Unit = {}
      override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {}
    })
    tstream.filter(query)
    tstream
  }

  def endStream(stream: TwitterStream): Unit = {
    stream.cleanUp()
  }
}
