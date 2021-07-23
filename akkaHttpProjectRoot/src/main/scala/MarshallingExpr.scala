import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.Done
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes

// for JSON serialization/deserialization following dependency is required:
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.io.StdIn
import scala.concurrent.Future

object SprayJsonExample {

  implicit val system           = ActorSystem(Behaviors.empty, "SprayExample")
  implicit val executionContext = system.executionContext

  // domain model
  final case class Item(name: String, id: Long)
  final case class Order(items: List[Item])

  // formats for unmarshalling and marshalling
  implicit val itemFormat  = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)

  // (fake) DB
  var orders: List[Item] = Nil
  // (fake) async database query api
  def fetchItem(itemId: Long): Future[Option[Item]] = Future {
    orders.find(o => o.id == itemId)
  }

  def saveOrder(order: Order): Future[Done] = {
    orders = order match {
      case Order(items) => items ::: orders
      case _            => orders
    }
    Future { Done }
  }

  /*
  Transforming request and response bodies between over-the-wire formats
  and objects to be used in your application is done separately from the route declarations,
  in marshallers, which are pulled in implicitly using the “magnet” pattern.

  This means that you can complete a request with any kind of object
  as long as there is an implicit marshaller available in scope.

  In this case, itemFormat and orderFormat are.
  Default marshallers are also provided for simple objects like String or ByteString.
   */
  def main(args: Array[String]): Unit = {
    val route: Route = concat(
      get {
        pathPrefix("item" / LongNumber) { id =>
          val maybeItem: Future[Option[Item]] = fetchItem(id)
          onSuccess(maybeItem) {
            case Some(item) => complete(item)
            case None       => complete(StatusCodes.NotFound)
          }
        }
      },
      post {
        path("create-order") {
          entity(as[Order]) { order =>
            val saved: Future[Done] = saveOrder(order)
            onSuccess(saved) { _ =>
              // we are not interested in the result value `Done` bu only in the fact that it was successful
              complete("order created")
            }
          }
        }
      }
    )

    val port          = 8080
    val bindingFuture = Http().newServerAt("localhost", port).bind(route)
    system.log.info(s"Server online at http://localhost:$port/\nPress RETURN on stop...")
    StdIn.readLine() // let it run until user presses return

    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
/*
create order:
curl -H "Content-Type: application/json" -X POST -d '{"items":[{"name":"hhgtg","id":43}]}' http://localhost:8080/create-order

get order:
curl -H "Content-tion/json" -X GET http://localhost:8080/item/42
 */
