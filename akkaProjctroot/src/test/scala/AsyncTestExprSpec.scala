import akka.util.Timeout

import akka.actor.typed._
import akka.actor.typed.scaladsl._
// import akka.pattern.ask
import akka.actor.typed.scaladsl.AskPattern._

import akka.actor.testkit.typed.scaladsl.ActorTestKit

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Success, Try}

object AsyncTestExprSpec {
  object Echo {
    case class Ping(message: String, response: ActorRef[Pong])
    case class Pong(message: String)

    def apply(): Behavior[Ping] = Behaviors.receiveMessage { case Ping(m, replyTo) =>
      replyTo ! Pong(m)
      Behaviors.same
    }
  }

  case class Message(i: Int, replyTo: ActorRef[Try[Int]])
  /*
  The ActorSystem facility for scheduling tasks.
  For scheduling within actors Behaviors.withTimers should be preferred
   */
  class Producer(publisher: ActorRef[Message])(implicit scheduler: Scheduler) {
    def produce(messages: Int)(implicit timeout: Timeout): Unit = {
      (0 until messages).foreach(publish)
    }

    private def publish(i: Int)(implicit timeout: Timeout): Future[Try[Int]] = {
      /*
      In Akka, ask is a pattern and involves Actors as well as Futures.
      Ask is used to send a message asynchronously and it returns a Future which represents a possible reply.
      If the actor does not reply and complete the future, it will expire after the timeout period.
      After the timeout period, it throws a TimeoutException.
      https://blog.knoldus.com/what-is-ask-pattern-in-akka/
       */
      publisher ask { Message(i, _) }
    }
  }
}

class AsyncTestExprSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  import AsyncTestExprSpec._ // Echo
  val testKit = ActorTestKit()

  "A testKit" must {
    "support verifying a response" in {
      val pinger = testKit.spawn(Echo(), "ping") // Actors can also be spawned anonymously.
      val probe  = testKit.createTestProbe[Echo.Pong]()

      pinger ! Echo.Ping("Ok", probe.ref)
      probe.expectMessage(Echo.Pong("Ok"))
    }

    "support observing mocked behavior" in {
      import testKit._
      val mockedBehavior = Behaviors.receiveMessage[Message] { msg =>
        msg.replyTo ! Success(msg.i)
        Behaviors.same
      }

      /*
       monitor: Behavior decorator that copies all received message to the designated monitor
       akka.actor.typed.ActorRef before invoking the wrapped behavior.
       This is not used as a result checking, but used for capturing messages sent between actors interaction.
       In the example below, capturing message from publish ask to mockedBehavior.
       */
      val probe           = createTestProbe[Message]()
      val mockedPublisher = spawn(Behaviors.monitor(probe.ref, mockedBehavior))

      val producer = new Producer(mockedPublisher)
      val messages = 3
      producer.produce(messages)

      for (i <- 0 until messages) {
        /*
        expectMessageType: used for async testing.
        If you add one to messages, it will cause Timeout(3 seconds) during expectMessageClass waiting for ...
        because the fourth message would never come.
         */
        val msg = probe.expectMessageType[Message]
        msg.i shouldBe i
      }
    }
  }

  // Your test is responsible for shutting down the ActorSystem e.g. using BeforeAndAfterAll when using ScalaTest
  override def afterAll(): Unit = testKit.shutdownTestKit()
}
