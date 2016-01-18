package API.model

import slick.driver.MySQLDriver.api._

object TweetModel {

  case class Tweet(
            id:       Option[Long],
            var text:     String,
            var author:   Long,
            var latitude: Double,
            var longitude:Double,
            var language: Option[String],
            var tension:  Int){

    def location: Option[Location] = Some(Location(latitude,longitude))
    def location_=(l: Option[Location]): Unit = l match {
      case Some(Location(la,lo)) => {latitude = la; longitude = lo}
      case None => {latitude = 0; longitude = 0}
    }
  }
  case class Location(latitude: Double, Longitude: Double)

  class Tweets(tag: Tag) extends Table[Tweet](tag, "Tweet"){
    def id = column[Long]("id",O.PrimaryKey,O.AutoInc)
    def text = column[String]("text")
    def author = column[Long]("author")
    def latitude = column[Double]("latitude")
    def longitude = column[Double]("longitude")
    def language = column[Option[String]]("language")
    def tension = column[Int]("tension")

    def * = (id.?,text,author,latitude,longitude,language,tension) <> (Tweet.tupled, Tweet.unapply)
  }

  val tweets = TableQuery[Tweets]
}

