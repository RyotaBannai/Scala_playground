import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn

object HttpServerRoutingMinimal {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val port          = 8083
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)
    system.log.info(s"Server online at http://localhost:$port/\nPress RETURN on stop...")
    StdIn.readLine() // let it run until user presses return

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  //#start-http-server
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "my-system")

    val routes = path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

    startHttpServer(routes)(system)
    //#server-bootstrapping
  }
} // end of object HttpServerRoutingMinimal

/*
import akka.http.scaladsl.model._  // for HttpMethods
import HttpMethods._               // for Uri
import Uri._                       // for Query
 */
