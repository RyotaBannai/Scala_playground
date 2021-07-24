import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.sse.ServerSentEvent
import scala.concurrent.duration._

import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME

import scala.io.StdIn
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

object SSEExpr {

  /** Server-side usage with marshalling. */
  import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
  def route: Route = {

    path("events") {
      get {
        complete {
          Source
            .tick(2.seconds, 2.seconds, NotUsed)
            .map(_ => LocalTime.now())
            .map(time => ServerSentEvent(ISO_LOCAL_TIME.format(time)))
            .keepAlive(1.second, () => ServerSentEvent.heartbeat)
        }
      }
    }
  }

  implicit val system           = ActorSystem(Behaviors.empty, "my-system")
  implicit val executionContext = system.executionContext // for flatMap and onComplete

  /** Client-side usage with unmarshalling. */
  // val port = 8000;
  // val bindingFuture = Http()
  //   .singleRequest(HttpRequest(uri = s"http://localhost:$port/events")) // Get helper for test
  //   .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
  //   .foreach(_.runForeach(println))

  // system.log.info(s"Server online at http://localhost:$port/\nPress RETURN on stop...")
  // StdIn.readLine() // let it run until user presses return

  // bindingFuture
  //   .flatMap(_.unbind())
  //   .onComplete(_ => system.terminate())
}
