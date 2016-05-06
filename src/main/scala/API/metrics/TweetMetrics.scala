package API.metrics

import java.sql.Timestamp

import API.metrics.Helpers._
import API.model.{Collection, Tweet, User}
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree
import edu.stanford.nlp.util.CoreMap
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm
import org.carrot2.core.{ControllerFactory, Document}
import twitter4j.Status
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Future

class TweetMetrics(collection: Collection, wordcounts: mutable.HashSet[(String,String,Long)], userinteraction: mutable.HashSet[(String,String,Long)]) {

  def process(tweets: Iterable[Status], users: Map[Long, User]): Future[Iterable[Tweet]] = Future {

    val corroboration = {
      try {
        val results = ControllerFactory.createSimple().process(tweets.map(tweet => new Document(tweet.getText, null, tweet.getId.toString)).toList, collection.query.orNull, classOf[LingoClusteringAlgorithm])
        println(results.getClusters)
        var biggestCluster = results.getClusters.map(_.size).reduce(Math.max)
        val map = (for {
          cluster <- results.getClusters
          document <- cluster.getAllDocuments
        } yield (document.getContentUrl.toLong, 10.0 * cluster.size / biggestCluster)).toMap
        (t: Status) => map.getOrElse(t.getId, 0.0)
      }
      catch {
        case e: Throwable => e.printStackTrace(); (t: Status) => 0.0
      }
    }

    def proximity(tweet: Status): Double = {
      if (tweet.getGeoLocation != null) {
        val d = distance(tweet.getGeoLocation.getLatitude,collection.lat,tweet.getGeoLocation.getLongitude,collection.lon)
        inInterval(10 - Math.log(d)/20 / Math.log(3.51))
      }
      else users.get(tweet.getUser.getId) match {
        case Some(u) if u.latitude.isDefined =>
          val d = distance(u.latitude.get,collection.lat,u.longitude.get,collection.lon)
          inInterval(10 - Math.log(d*4)/20 / Math.log(3.51))
        case _ => 0.0
      }
    }

    def recency(tweet: Status): Double = inInterval(Math.log((tweet.getCreatedAt.getTime - collection.time) / 1000 / 60 / 86400)/ Math.log(0.3769))

    val sentiment = {
      val map = tweets.map(tweet => tweet.getId -> wordAnalysis(tweet)).toMap
      (t: Status) => map.get(t.getId) match {
          // Sentiment of longest sentence
//        case Some(sents) => sents.reduce((s1,s2) => if (s1._1 > s2._1) s1 else s2)._2.toDouble * 3
          // Weighted average
        case Some(sents) =>
          val (weightedSum, count) = sents.foldLeft((0.0,0)) {case ((ws, c), (l,s)) => (ws + l * s,c + l)}
          weightedSum / count * 5
        case None => 0.0
      }
    }

    for (tweet <- tweets) yield Tweet(
      Some(tweet.getId),
      tweet.getText,
      tweet.getUser.getScreenName,
      new Timestamp(tweet.getCreatedAt.getTime),
      collection.id.get,
      Option(tweet.getGeoLocation).map(_.getLatitude),
      Option(tweet.getGeoLocation).map(_.getLongitude),
      sentiment(tweet),
      recency(tweet),
      corroboration(tweet),
      proximity(tweet)
    )
  }

  private def wordAnalysis(tweet: Status): Seq[(Int,Int)] = {
    val tree = pipeline.process(tweet.getText)

    for (sentence: CoreMap <- tree.get(classOf[SentencesAnnotation])) yield {
      for (token <- sentence.get(classOf[TokensAnnotation])) {
        // Raw word
        val word: String = token.get(classOf[TextAnnotation])
        // Word in its base form (i.e. infinitive, lowercase?)
        val lemma: String = token.get(classOf[LemmaAnnotation])
        // Type of word
        val pos: String = token.get(classOf[PartOfSpeechAnnotation])
        // Named entity classification
        val ne: String = token.get(classOf[NamedEntityTagAnnotation])

        if (lemma.charAt(0) == '@' && lemma.length > 1) {
          userinteraction synchronized {
            userinteraction.add((tweet.getUser.getScreenName, lemma.substring(1).toLowerCase, tweet.getId))
          }
        }
        else if (lemma.charAt(0) == '#') {
          wordcounts synchronized {
            wordcounts.add((lemma, "HASHTAG", tweet.getId))
          }
        }
        else if (word.matches(URL_REGEX.regex)) {
          val resolved = resolveUrl(word)
          if (!resolved.matches("""https?:\/\/(www\.)?(twitter.com|t.co).*""")){
            wordcounts synchronized {
              wordcounts.add((resolved, "URL", tweet.getId))
            }
          }
        }
        else if (ne == "LOCATION" || ne == "PERSON" || ne == "ORGANIZATION") {
          wordcounts synchronized {
            wordcounts.add((lemma, ne, tweet.getId))
          }
        }
        else pos match {
          case "CD" | "PDT" //'amounts'
               | "FW" //foreign words
               | "JJ" | "JJR" | "JJS" | "RB" | "RBR" | "RBS" //adjectives and adverbs
               | "NN" | "NNP" | "NNPS" | "NNS" //nouns
               | "VB" | "VBD" | "VBG" | "VBN" | "VBP" | "VBZ" //verbs
            if !isStopWord(lemma) => wordcounts synchronized {
            wordcounts.add((lemma.toLowerCase, pos, tweet.getId))
          }
          case _ =>
        }
      }
      (sentence.toString.length,RNNCoreAnnotations.getPredictedClass(sentence.get(classOf[SentimentAnnotatedTree])))
    }
  }
}