package API.metrics

import API.model.Tweet

import scala.util.Random

object TweetMetrics {

  val r = new Random()

  def compute(t: Tweet): Unit = {

    // TODO parallelize this

    if (t.recency.isEmpty)
      t.recency = Some(recency(t))

    if (t.corroboration.isEmpty)
      t.corroboration = Some(corroboration(t))

    if (t.proximity.isEmpty)
      t.proximity = Some(proximity(t))
  }

  private def recency(t: Tweet): Double = r.nextGaussian()*2+5
  private def corroboration(t: Tweet): Double = r.nextGaussian()*2+5
  private def proximity(t: Tweet): Double = r.nextGaussian()*2+5
}
