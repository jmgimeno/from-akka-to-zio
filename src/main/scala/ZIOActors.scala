
import zio._
import zio.stream._

object ZIOExample1 extends ZIOAppDefault {

  // queues
  // refs
  // STM???

  // Approaches
  // 1. How do we replicate actors with ZIO machinery
  // 2. How do rearchitect this ignoring actors altogether

  trait Actor[-In] {
    def send(in: In): UIO[Any]
  }

  object Actor {
    def make[In](makeHandler: UIO[In => UIO[Any]]): UIO[Actor[In]] =
      for {
        inbox <- Queue.unbounded[In]
        handler <- makeHandler
        fiber <- inbox.take.flatMap(handler).forever.forkDaemon
      } yield new Actor[In] {
        def send(in: In): UIO[Any] =
          inbox.offer(in)
      }
  }

  sealed trait CounterMessage

  case object Increment extends CounterMessage

  val makeCounterActor: UIO[Actor[CounterMessage]] =
    Actor.make[CounterMessage](Ref.make(0).map { ref =>
      { case Increment =>
        ref.updateAndGet(_ + 1).debug
      }
    })

  def bot(n: Int, name: String, greeter: String => UIO[String]): UIO[Any] = {
    if (n <= 0) ZIO.unit
    else
      Console.printLine(s"Greeting $n $name").orDie *> greeter(name) *> bot(
        n - 1,
        name,
        greeter
      )
  }

  def greeter(name: String): UIO[String] = {
    val greeting = s"Hello $name!"
    Console.printLine(greeting).as(greeting).orDie
  }

  val run =
    bot(3, "World", greeter) zipPar bot(3, "ZIO", greeter)
}

object ZIOExampleStream extends ZIOAppDefault {

  // ZIO.acquireRelease

  // ZStream.acquireRelease

  // Scope

  val source: ZStream[Any, Nothing, Int] =
    ZStream.fromIterable(1 to 100)

  val factorials = source.scan(BigInt(1))(_ * _)

  val run =
    factorials
      .map(num => Chunk.fromArray(s"$num\n".getBytes))
      .flattenChunks
      .run(ZSink.fromFileName("ziofactorials.txt"))
}
