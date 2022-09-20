import akka.stream._
import akka.stream.scaladsl._

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

// Akka                                          // ZIO
//   Source - produces elements                  ZStream - produces elements
//   Flow - consumes and produces elements       ZPipeline - consumes and produces elements
//   Sink - consumes elements                    ZSink - consumes elements

object AkkaExample1 extends App {
  implicit val system: ActorSystem = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher

  val source: Source[Int, NotUsed] = Source(1 to 100)

  val done = source.runForeach(i => println(i))

  done.onComplete(_ => system.terminate())
}

object AkkaExample2 extends App {
  implicit val system: ActorSystem = ActorSystem("QuickStart")

  val source: Source[Int, NotUsed] = Source(1 to 100)

  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val result: Future[IOResult] =
    factorials
      .map(num => ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get("akkafactorials.txt")))
}

object AkkaExample3 extends App {

  final case class Author(handle: String)

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body
        .split(" ")
        .collect {
          case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
        }
        .toSet
  }

  val akkaTag = Hashtag("#akka")

  val tweets: Source[Tweet, NotUsed] = Source(
    Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
      Tweet(
        Author("ktosopl"),
        System.currentTimeMillis,
        "#akka on the rocks!"
      ) ::
      Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
      Tweet(
        Author("drama"),
        System.currentTimeMillis,
        "we compared #apples to #oranges!"
      ) ::
      Nil
  )

  implicit val system: ActorSystem = ActorSystem("reactive-tweets")

  tweets
    .filterNot(_.hashtags.contains(akkaTag))
    .map(_.hashtags)
    .reduce(_ ++ _)
    .mapConcat(identity)
    .map(_.name.toUpperCase)
    .runWith(Sink.foreach(println))
}

object AkkaExample4 extends App {
  implicit val system: ActorSystem = ActorSystem("QuickStart")

  val source: Source[Int, NotUsed] = Source(1 to 100)

  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  factorials.map(_.toString).runWith(lineSink("akkafactorial2.txt"))
}
