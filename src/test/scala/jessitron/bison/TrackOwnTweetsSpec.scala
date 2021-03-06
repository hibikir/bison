package jessitron.bison

import scalaz.stream._

import org.scalacheck._
import Prop._

object TrackOwnTweetsSpec extends Properties("track what I am tweeting") {
  import Common._

  property("TweetThis gets added to the queue") = forAll(assortedMessages){
    messages: List[Message] =>
      val (queue, readProcess) = async.queue[Message]
      val subject = TrackOwnTweets.enqueueTweets(queue) _
      val output :Seq[Message]= subject(Process(messages:_*).toSource).runLog.run

      val queueContents = readProcess.runLog.run

      val tweeted = messages collect { case t: TweetThis => t}

      (output ?= messages.toSeq) :| "All messages should pass through" &&
      (tweeted forall { queueContents.contains(_)}) :| "all tweeted are queued" &&
      (tweeted.size =? queueContents.size) :| "nothing else is queued"
  }

  property("Forgetting own tweets removes TweetThis") = forAll(assortedMessages) {
    messages: List[Message] =>
      // Generify name to avoidLoops?
    val subject = TrackOwnTweets.dropTweeteds

    val p = Process(messages:_*) |> subject
    val output = p.toList

    val shouldBeExcluded: Message => Boolean = {m => m.isInstanceOf[TweetThis]}
    val shouldComeThrough = messages filterNot shouldBeExcluded
    val snuckThrough = output filter shouldBeExcluded

    (output.size ?= shouldComeThrough.size) :| "other messages should come through" &&
    (snuckThrough.size =? 0) :| "no excluded messages should come through"
  }
}
