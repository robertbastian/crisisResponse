package API.metrics

import java.util.Properties

import API.model.Tweet
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import edu.stanford.nlp.util.CoreMap

import scala.collection.JavaConversions._
import scala.collection.mutable

object TweetMetrics {
  // TODO
  val r = new scala.util.Random()

  val props = new Properties()
  props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment")
  lazy val pipeline = new StanfordCoreNLP(props)

  val ignoreRegex = """rt|not|be|have|do|http.*"""

  def apply(tweet: Tweet, wordcounts: mutable.HashSet[(String, String, Int, Long)], userinteraction: mutable.HashSet[(String, String, Long)]): Tweet = {

    val text = pipeline.process(tweet.text)
    var totalSentiment = 0

    for (sentence: CoreMap <- text.get(classOf[SentencesAnnotation])) {
      val sentiment = RNNCoreAnnotations.getPredictedClass(sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree]))
      totalSentiment += sentiment
      for (token <- sentence.get(classOf[TokensAnnotation])){
        val word: String = token.get(classOf[TextAnnotation])
        val lemma: String = token.get(classOf[LemmaAnnotation])
        val pos: String = token.get(classOf[PartOfSpeechAnnotation])
        if (lemma.charAt(0) == '@'){
          userinteraction synchronized {userinteraction.add((tweet.author,lemma.substring(1).toLowerCase,tweet.id.get))}
        }
        else pos match {
          case "CD" | "PDT" //'amounts'
               | "FW" //foreign words
               | "JJ" | "JJR" | "JJS" | "RB" | "RBR" | "RBS" //adjectives and adverbs
               | "NN" | "NNP" | "NNPS" | "NNS"  //nouns
               | "VB" | "VBD" | "VBG" | "VBN" | "VBP" | "VBZ" //verbs
            if !lemma.matches(ignoreRegex) => wordcounts synchronized {wordcounts.add((lemma.toLowerCase,pos,sentiment,tweet.id.get))}
          case _ =>
        }
      }
    }

    tweet.copy(
      sentiment = Some(totalSentiment.toDouble / text.get(classOf[SentencesAnnotation]).size),
      recency = Some(r.nextGaussian()*2+5),
      corroboration = Some(r.nextGaussian()*2+5),
      proximity = Some(r.nextGaussian()*2+5)
    )
  }
}
