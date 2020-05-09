package route

import actor.OrderActor._
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import util.JsonSupport

import scala.util.{Failure => ScalaFailure, Success => ScalaSuccess}
import scala.concurrent.duration._

trait OrdersRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val logs = Logging(system, classOf[OrdersRoutes])

  def orderActor: ActorRef

  private implicit lazy val timeout: Timeout = Timeout(5, SECONDS)

  val ordersRoutes: Route =
    pathPrefix("orders") {
      concat(
        pathEnd {
          concat(
            get {
              onComplete(orderActor ? GetOrders) {
                case ScalaSuccess(value) =>
                  value match {
                    case Seq() => {
                      logs.info("No order found in the database")
                      complete(StatusCodes.OK,"No order found because the database is empty.")
                    }
                    case first +: tail => {
                      //TODO [FIXME] return value must be a formatted view of list (JSON) not the list himself
                      logs.info("At least one order was found. Displaying.")
                      complete(StatusCodes.OK,value.toString)
                    }
                    case _ => {
                      logs.error("Something wrong during getOrders process")
                      complete(StatusCodes.NotFound)
                    }
                  }
                case ScalaFailure(value) => {
                  logs.error("Something wrong during getOrders process",value)
                  complete(StatusCodes.NotFound)
                }
              }
            }
          )
        }
      )
    }
}