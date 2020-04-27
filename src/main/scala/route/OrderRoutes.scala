package route

import java.time.LocalDateTime

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{delete, get, post, put}
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import actor.OrderActor._
import actor.Order
import service.DatabaseService
import util.JsonSupport
import scala.util.{Success => ScalaSuccess, Failure => ScalaFailure}
import scala.concurrent.Future
import scala.concurrent.duration._

trait OrderRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[OrderRoutes])

  def orderActor: ActorRef

  private implicit lazy val timeout: Timeout = Timeout(5, SECONDS)

  lazy val transferRoutes: Route =
    pathPrefix("order") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[Order]) {
                createdOrder =>
                  val order = Order(DatabaseService.getFreeId, LocalDateTime.now().toString, createdOrder.destinationAddress, createdOrder.customerName, "0")
                  val orderCreated: Future[ActionPerformed] = (orderActor ? OrderCreated(order)).mapTo[ActionPerformed]
                  onSuccess(orderCreated) { performed =>
                    log.info("Order created [{}]: {}", order.id, performed.description)
                    complete((StatusCodes.Created, performed))
                  }
              }
            }
          )
        },
        path(Segment) { id =>
          concat(
            get {
              onComplete((orderActor ? GetOrder(id.toInt)).mapTo[Order]) {
                case ScalaSuccess(value) => {
                  log.info("Order n° {} founded.", value.id)
                  complete(StatusCodes.OK, value)
                }
                case ScalaFailure(value) => {
                  log.info("Order n° {} wasn't founded.", id)
                  complete(StatusCodes.NotFound)
                }
              }
            },
            delete {
              onComplete((orderActor ? GetOrder(id.toInt)).mapTo[Order]) {
                case ScalaSuccess(value) => {
                  val orderFounded: Future[ActionPerformed] = (orderActor ? OrderDeleted(id.toInt)).mapTo[ActionPerformed]
                  onSuccess(orderFounded) { performed =>
                    log.info("Order n° {} deleted : {}", value.id, performed.description)
                    complete(StatusCodes.OK, performed)
                  }
                }
                case ScalaFailure(value) => {
                  log.info("Order n° {} wasn't founded. Operation aborted", id)
                  complete(StatusCodes.NotFound)
                }
              }
            },
            put {
              entity(as[Order]) {
                orderInput =>
                  val orderStatusUpdated: Future[ActionPerformed] = (orderActor ? OrderSetToPrepared(orderInput)).mapTo[ActionPerformed]
                  onSuccess(orderStatusUpdated) { performed =>
                    log.info("Order status updated [{}]: {}", orderInput.id, performed.description)
                    complete((StatusCodes.Created, performed))
                  }
              }
            }
          )
        },
        path(Segment / "forward") { id =>
          put {
            onComplete((orderActor ? GetOrder(id.toInt)).mapTo[Order]) {
              case ScalaSuccess(value) => {
                val orderFounded: Future[ActionPerformed] = (orderActor ? OrderStatusUpdatedToNextStep(value)).mapTo[ActionPerformed]
                onSuccess(orderFounded) { performed =>
                  log.info("Order n° {} status updated : {}", id, performed.description)
                  complete(StatusCodes.OK, performed)
                }
              }
              case ScalaFailure(value) => {
                log.info("Order n° {} wasn't founded. Operation aborted", id)
                complete(StatusCodes.NotFound)
              }
            }
          }
        }
      )
    }
}