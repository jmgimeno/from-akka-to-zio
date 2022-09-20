import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

// Today: We're (not) going to cover everything in the Akka ecosystem
//        Akka-tors
//        Akka Streams
//
// Future: Shard Cake? (Navid Suggests)

// This: Standard Zympo. Compare and Contrast, ZIO & Akka
// Next Week: Akka Panel with folks who've migrated from Akka to ZIO
//       + White Paper (fancy name for ebook :D)
//       + Free Adam and Kit (to help you migrate, and record some tutorial video)
//       + zio-http is now an official zio org project (and getting swarmed with PRs)

// Actor[T]
//  Accepts messages of type T
//  Processes those messages sequentially
//  We can reference other actors (ActorRef)
//  Actor encompasses mutable state

object HelloWorld {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String, from: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
    println(s"Hello ${message.whom}!")
    message.replyTo ! Greeted(message.whom, context.self)
    Behaviors.same
  }
}

object HelloWorldBot {

  def apply(max: Int): Behavior[HelloWorld.Greeted] = {
    bot(0, max)
  }

  private def bot(
      greetingCounter: Int,
      max: Int
  ): Behavior[HelloWorld.Greeted] =
    Behaviors.receive { (context, message) =>
      val n = greetingCounter + 1
      println(s"Greeting $n for ${message.whom}")
      if (n == max) {
        Behaviors.stopped
      } else {
        message.from ! HelloWorld.Greet(message.whom, context.self)
        bot(n, max)
      }
    }
}

object HelloWorldMain extends App {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      val greeter = context.spawn(HelloWorld(), "greeter")

      Behaviors.receiveMessage { message =>
        val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)
        greeter ! HelloWorld.Greet(message.name, replyTo)
        Behaviors.same
      }
    }

  val system: ActorSystem[HelloWorldMain.SayHello] =
    ActorSystem(HelloWorldMain(), "hello")

  system ! HelloWorldMain.SayHello("World")
  system ! HelloWorldMain.SayHello("Akka")

}
