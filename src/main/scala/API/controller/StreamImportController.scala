package API.controller

import API.model.Event
import API.remotes.Twitter
import twitter4j.{FilterQuery, Status}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class StreamImportController(e: Event) extends ImportController(e) {

  val filter = new FilterQuery()
  filter.language("en")
  filter.track(e.query.get.split(',') :_*)
  val queue = new scala.collection.mutable.HashSet[Status]
  val stream = Twitter.startStream(filter,(tweet: Status) => queue synchronized {queue.add(tweet)})

  StreamImportController.CURRENT = this

  protected def finish() {
    StreamImportController.CURRENT = null
    Twitter.endStream(stream)
    finishedCollecting(queue)
  }
}

object StreamImportController {
  def active: Future[Int] = Future {
    Thread.sleep(100)
    if (CURRENT == null)
      -1
    else {
      var size = 0
      CURRENT.queue synchronized {size = CURRENT.queue.size}
      size
    }
  }

  def finish() {
    CURRENT.finish()
    CURRENT = null
  }

  private var CURRENT: StreamImportController = null
}