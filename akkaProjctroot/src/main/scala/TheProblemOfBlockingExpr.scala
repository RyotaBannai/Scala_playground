package theProblemOfBlockingExpr

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import scala.concurrent.{ExecutionContext, Future}

object PrintActor {
  def apply(): Behavior[Int] = Behaviors.setup { ctx =>
    println(ctx.self.path.name)
    Behaviors.empty
  }
} // end of PrintActor object

object BlockingFutureActor {
  def apply(): Behavior[Int] = Behaviors.setup { ctx =>
    // the key problematic line
    /*
    Using context.executionContext as the dispatcher on which the blocking Future executes can still be a problem,
    since this dispatcher is by default used for all other actor processing unless you set up a separate dispatcher for the actor.
     */
    implicit val executionContext: ExecutionContext = ctx.executionContext

    Behaviors.receiveMessage[Int] { i =>
      triggerFutureBlockingOps(i)
      Behaviors.same
    }
  }

  def triggerFutureBlockingOps(i: Int)(implicit ec: ExecutionContext): Future[Unit] = {
    println(s"Calling blocking future $i")
    Future {
      Thread.sleep(5000) // block for 5 seconds
      println(s"Blocking future finished $i")
    }
  }
} // end of object BlockingFutureActor

object SeparateDispatcherFutureActor {
  def apply(): Behavior[Int] = Behaviors.setup { ctx =>
    /*
    When blocking operations are run on the my-blocking-dispatcher,
    it uses the threads (up to the configured limit) to handle these operations.
    The sleeping in this case is nicely isolated to just this dispatcher, and the default one remains unaffected,
    allowing the rest of the application to proceed as if nothing bad was happening.
     */
    implicit val executionContext: ExecutionContext =
      ctx.system.dispatchers.lookup(DispatcherSelector.fromConfig("my-blocking-dispatcher"))

    Behaviors.receiveMessage[Int] { i =>
      triggerFutureBlockingOps(i)
      Behaviors.same
    }
  }

  def triggerFutureBlockingOps(i: Int)(implicit ec: ExecutionContext): Future[Unit] = {
    println(s"Calling blocking future $i")
    Future {
      Thread.sleep(5000) // block for 5 seconds
      println(s"Blocking future finished $i")
    }
  }
} // end of object SeparateDispatcherFutureActor

object TheProblemOfBlockingExpr {
  def main(args: Array[String]): Unit = {
    val root = Behaviors.setup[Nothing] { ctx =>
      for (i <- 1 to 100) {
        ctx.spawn(SeparateDispatcherFutureActor(), s"futureActor-$i") ! i
        ctx.spawn(PrintActor(), s"printActor-$i") ! i
      }
      Behaviors.empty
    }

    val system = ActorSystem[Nothing](root, "TheProblemOfBlockingExpr")
  }
} // end of object TheProblemOfBlockingExpr
