package API.controller

import java.util.concurrent.ConcurrentLinkedQueue

import API.remotes.Twitter
import twitter4j.{FilterQuery, Status}
import scala.collection.JavaConversions._
import scala.collection.parallel.mutable
import scala.collection.parallel.mutable.ParHashMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
class StreamImportController  (name: String, keywords: Seq[String], lon: Double, lat: Double, time: Long) extends ImportController(name,lon,lat,time,Some(keywords.mkString(","))) {

  val filter = new FilterQuery()
  filter.language("en")
  filter.track(keywords :_*)
  val queue = new scala.collection.mutable.HashSet[Status]
  val stream = Twitter.startStream(filter,(tweet: Status) => queue synchronized {queue.add(tweet)})

  StreamImportController.CURRENT = this

  protected def finish() {
    StreamImportController.CURRENT = null
    Twitter.endStream(stream)
    analyze(queue)
  }
}

object StreamImportController {
  def active: Future[Int] = Future {
    Thread.sleep(100)
    if (CURRENT == null)
      -1
    else {
      var size = 0
      println("trying to get size")
      CURRENT.queue synchronized {size = CURRENT.queue.size}
      println("size = " + size)
      size
    }
  }

  def finish() {
    CURRENT.finish()
    CURRENT = null
  }

  private var CURRENT: StreamImportController = null
}