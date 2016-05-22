package API.metrics

import API.metrics.Helpers._
import API.model.Types.{Interaction, Word}
import API.model.{Event, Tweet, User}
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree
import edu.stanford.nlp.util.CoreMap
import twitter4j.Status

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TweetMetrics {
  type Metric = Status => Double

  def apply(event: Event, tweets: Iterable[Status], analysedUsers: Map[Long, User]):
    Future[(Iterable[Tweet],Set[User],Iterable[Word],Iterable[Interaction])] = Future {

    val corroboration: Metric = {
      val clusters = cluster(tweets,event.query)
      val div = clusters.map(_.size).reduce(Math.max) / 10.0
      val map = {
        for (cluster <- clusters; document <- cluster.getAllDocuments)
          yield document.getContentUrl.toLong -> cluster.size / div
      }.toMap
      t => map.getOrElse(t.getId, 0.0)
    }

    val proximity: Metric = tweet => analysedUsers.get(tweet.getUser.getId) match {
      case _ if tweet.getGeoLocation != null =>
        10 - 1.08574 * Math.log(distance(tweet.getGeoLocation,event))
      case Some(user) if user.latitude.isDefined =>
        5 - 0.54287 * Math.log(distance(user,event))
      case _ => 0.0
    }

    val recency: Metric = tweet =>
      log(0.3769)((tweet.getCreatedAt.getTime/1000 - event.time) / (60*86400))

    val words = new mutable.ListBuffer[Word]
    val interactions = new mutable.ListBuffer[Interaction]
    def wordAnalysisAndSentiments(tweet: Status): Seq[(Int,Int)] = {
      for (sentence: CoreMap <- pipeline.process(tweet.getText).get(classOf[SentencesAnnotation])) yield {
        val sentiment = RNNCoreAnnotations.getPredictedClass(sentence.get(classOf[SentimentAnnotatedTree]))
        for (token <- sentence.get(classOf[TokensAnnotation])) {
          val rawWord: String = token.get(classOf[TextAnnotation])
          val normalisedWord: String = token.get(classOf[LemmaAnnotation])
          val partOfSpeech: String = token.get(classOf[PartOfSpeechAnnotation])
          val namedEntity: String = token.get(classOf[NamedEntityTagAnnotation])

          if (rawWord.charAt(0) == '@' && rawWord.length > 1)
            interactions += ((tweet.getUser.getScreenName.toLowerCase, rawWord.substring(1).toLowerCase, tweet.getId))
          else if (rawWord.charAt(0) == '#')
            words += ((rawWord, "HASHTAG", tweet.getId))
          else if (rawWord.matches(URL_REGEX.regex)) {
            val resolved = resolveUrl(rawWord)
            if (!isTwitterURL(resolved))
              words += ((resolved, "URL", tweet.getId))
          }
          else if (namedEntity == "LOCATION" || namedEntity == "PERSON" || namedEntity == "ORGANIZATION")
            words += ((normalisedWord, namedEntity, tweet.getId))
          else partOfSpeech match {
            case "NN" | "NNP" | "NNPS" | "NNS" //nouns
              | "JJ" | "JJR" | "JJS" | "RB" | "RBR" | "RBS" //adjectives and adverbs
              | "VB" | "VBD" | "VBG" | "VBN" | "VBP" | "VBZ" //verbs
              | "CD" | "PDT" | "FW" //misc
            if !isStopWord(normalisedWord) => words += ((normalisedWord.toLowerCase, partOfSpeech, tweet.getId))
            case _ =>
          }
        }
        (sentence.toString.length,sentiment)
      }
    }

    val sentiment: Metric = {
      val sentiments = tweets.map(tweet => tweet.getId -> weightedAverage(wordAnalysisAndSentiments(tweet)) * 2.5).toMap
      t => sentiments.getOrElse(t.getId,0.0)
    }

    val processedTweets = for (tweet <- tweets) yield Tweet(
      Some(tweet.getId),
      tweet.getText,
      tweet.getUser.getScreenName,
      tweet.getCreatedAt.getTime/1000,
      event.id.get,
      Option(tweet.getGeoLocation).map(_.getLatitude),
      Option(tweet.getGeoLocation).map(_.getLongitude),
      restrictInterval(sentiment(tweet)),
      restrictInterval(recency(tweet)),
      restrictInterval(corroboration(tweet)),
      restrictInterval(proximity(tweet))
    )
    (processedTweets,analysedUsers.values.toSet,words,interactions)
  }
}