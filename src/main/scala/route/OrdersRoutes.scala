package route

import actor.OrderActor._
import actor.{Order, Orders}
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import util.JsonSupport

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait OrdersRoutes extends JsonSupport {

  implicit def system: ActorSystem

  //lazy val log = Logging(system, classOf[OrdersRoutes])

  def orderActor: ActorRef

  private implicit lazy val timeout: Timeout = Timeout(5, SECONDS)

  val ordersRoutes: Route =
    pathPrefix("orders") {
      pathEndOrSingleSlash {
        get {
          val orders: Future[Orders] = (orderActor ? GetOrders).mapTo[Orders]
          complete(orders)
        }
      }
    }
}