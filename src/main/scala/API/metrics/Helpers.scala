package API.metrics

import java.net.{URL, HttpURLConnection}
import java.util.Properties

import edu.stanford.nlp.pipeline.StanfordCoreNLP
import twitter4j.User

object Helpers {
  // NLP setup
  val pipeline = {
    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, sentiment")
    props.put("pos.model","edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger")
    props.put("parse.model","edu/stanford/nlp/models/lexparser/englishPCFG.caseless.ser.gz" )
    props.put("ner.model.3class","edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz")
    props.put("ner.model.7class","edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz")
    props.put("ner.model.MISCclass","edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz")
    new StanfordCoreNLP(props)
  }

  //Stopwords by http://www.ranks.nl/stopwords
  def isStopWord(s: String): Boolean = s.matches("""a|about|above|after|again|against|all|am|an|and|any|are|aren't|as|at|be|because|been|before|being|below|between|both|but|by|can't|cannot|could|couldn't|did|didn't|do|does|doesn't|doing|don't|down|during|each|few|for|from|further|had|hadn't|has|hasn't|have|haven't|having|he|he'd|he'll|he's|her|here|here's|hers|herself|him|himself|his|how|how's|i|i'd|i'll|i'm|i've|if|in|into|is|isn't|it|it's|its|itself|let's|me|more|most|mustn't|my|myself|no|nor|not|of|off|on|once|only|or|other|ought|our|ours|ourselves|out|over|own|same|shan't|she|she'd|she'll|she's|should|shouldn't|so|some|such|than|that|that's|the|their|theirs|them|themselves|then|there|there's|these|they|they'd|they'll|they're|they've|this|those|through|to|too|under|until|up|very|was|wasn't|we|we'd|we'll|we're|we've|were|weren't|what|what's|when|when's|where|where's|which|while|who|who's|whom|why|why's|with|won't|would|wouldn't|you|you'd|you'll|you're|you've|your|yours|yourself|yourselves|rt|http.*""")

  // URL resolution
  // http://stackoverflow.com/questions/3809401/what-is-a-good-regular-expression-to-match-a-url
  HttpURLConnection setFollowRedirects false
  val URL_REGEX = """https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]+\.[a-z]{2,4}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)""".r
  def resolveUrl(urlv: String): String = {
    try {
      var url = urlv
      var d = url
      do {
        url = d
        d = new URL(url).openConnection().getHeaderField("Location")
      } while (d != null)
      url
    }
    catch {
      case e: Exception => urlv
    }
  }

  // http://www.movable-type.co.uk/scripts/latlong.html
  def distance(lat1: Double, lat2: Double, lon1: Double, lon2: Double): Double = {
    val R = 6371; // km
    var φ1 = lat1.toRadians
    var φ2 = lat2.toRadians
    var Δφ = (lat2-lat1).toRadians
    var Δλ = (lon2-lon1).toRadians

    var a = Math.sin(Δφ/2) * Math.sin(Δφ/2) + Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ/2) * Math.sin(Δλ/2)

    R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
  }

  def inInterval(x: Double, lower: Int = 0, upper: Int = 10): Double = Math.min(10,Math.max(0,x))

  def isNewsAgency(user: User): Boolean = newsAgencyIds.contains(user.getId)
  private val newsAgencyIds = Set[Long](
    742143, // BBC World
    8839632, // NBC Nightly News
    428333, // CNN Breaking News
    28785486, // ABC
    16374678, // ABC 7
    1367531, // FOX News
    2836421, // MSNBC
    51241574, // AP
    19038934, // CBC Alerts
    612473, // BBC News
    5402612, // BBC Breaking News
    7587032, // Sky news
    2467791, // Washington Post
    3108351, // Wall Street Journal
    1652541, // Reuters
    14173315, // NBC News
    759251, // CNN
    2097571, // CNN International
    2768501, // ABC News
    15012486, // CBS News
    807095, // New York Times
    14293310, // TIME
    14511951, // Huffington Post
    6433472, // CBC News
    1877831, // NY Times World
    69329527, // BBC News US
    87416722, // Sky News Breaking
    380285402, // Daily Mail
    42958829 // CBS Evening News
  )

  private val newsAgencyMatcher = ".*(" + Set[String](
    "BBC","CNN","FOX","ABC","MSNBC", "AP","CBC","Sky","Washington Post","Wall Street Journal","WSJ","Reuters","NBC","New York Times","NY Times", "TIME","Huffington Post","CBS","Daily Mail"
  ).mkString("|") + ").*"
  def referencesNewsAgency(user: User): Boolean = user.getDescription matches newsAgencyMatcher

  private val journalismMatcher = ".*(" + Set[String](
    "reporter","editor","journalist","correspondent","news presenter"
  ).mkString("|") + ").*"
  def isJournalist(user: User) = user.getDescription.toLowerCase matches journalismMatcher
}
