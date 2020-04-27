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
import actor.{Order, OrderStatus, Orders}
import akka.actor.Status.Success
import service.DatabaseService
import util.JsonSupport

import scala.concurrent.Future
import scala.concurrent.duration._

trait OrderRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[OrderRoutes])

  def orderActor: ActorRef

  private implicit lazy val timeout: Timeout = Timeout(5, SECONDS)

  /*
  lazy val transferRoutes: Route =
    pathPrefix("orders") {
      concat(
        pathEnd {
          concat(
            get {
              val orders: Future[Orders] = (orderActor ? GetOrders).mapTo[Orders]
              complete(orders)
            },
            post {
              entity(as[Order]) {
                createdOrder =>
                  val order = Order(DatabaseService.getFreeId, LocalDateTime.now().toString, createdOrder.destinationAddress, createdOrder.customerName, "confirmed")
                  val orderCreated: Future[ActionPerformed] = (orderActor ? OrderCreated(order)).mapTo[ActionPerformed]
                  onSuccess(orderCreated) { performed =>
                    log.info("Created order [{}]: {}", order.id, performed.description)
                    complete((StatusCodes.Created, performed))
                  }
              }
            }
          )
        }
      )
    }
    pathPrefix("order") {
      concat(
        pathEnd {
          concat(
            get {
              val orders: Future[Orders] = (orderActor ? GetOrders).mapTo[Orders]
              complete(orders)
            },
            post {
                entity(as[Order]) {
                  createdOrder =>
                    val order = Order(DatabaseService.getFreeId, LocalDateTime.now().toString, createdOrder.destinationAddress, createdOrder.customerName, "confirmed")
                    val orderCreated: Future[ActionPerformed] = (orderActor ? OrderCreated(order)).mapTo[ActionPerformed]
                  onSuccess(orderCreated) { performed =>
                    log.info("Created order [{}]: {}", order.id, performed.description)
                    complete((StatusCodes.Created, performed))
                  }
                }
            }
          )
        },
        path(Segment) { id =>
          concat(
            get {
              val order: Future[Order] = (orderActor ? GetOrder(id.toInt)).mapTo[Order]
              rejectEmptyResponse {
                complete(order)
              }
            },
            delete {
              val userDeleted: Future[ActionPerformed] =
                (orderActor ? OrderDeleted(id.toInt)).mapTo[ActionPerformed]
              onSuccess(userDeleted) { performed =>
                log.info("Deleted user [{}]: {}", id, performed.description)
                complete((StatusCodes.OK, performed))
              }
            },
            post {
              entity(as[Order]) {
                transfer => val orderStatusUpdated: Future[ActionPerformed] = (orderActor ? OrderSetToPrepared(transfer)).mapTo[ActionPerformed]
                  onSuccess(orderStatusUpdated) { performed =>
                    log.info("Order status updated [{}]: {}", transfer.id, performed.description)
                    complete((StatusCodes.Created, performed))
                  }
              }
            }
          )
        },
        path(Segment / "status") { id =>
          put {
            entity(as[OrderStatus]) { status =>
              val transferUpdated: Future[ActionPerformed] =
                (orderActor ? UpdateTransferStatus(id.toInt, status)).mapTo[ActionPerformed]
              onSuccess(transferUpdated) { performed =>
                log.info("Updated transfer [{}]: {}", id, performed.description)
                complete((StatusCodes.OK, performed))
              }
            }
          }
        }
      )
    }

*/

  val transferRoutes: Route =

    pathPrefix("order" / IntNumber) { id =>
      pathEndOrSingleSlash {
        get {
          val order: Future[Order] = (orderActor ? GetOrder(id)).mapTo[Order]
          rejectEmptyResponse {
            complete(order)
          }
        }
      }
    }
}

/*
      get {
        pathPrefix("orders") {
           /*
            complete("OK")
            val orders: Future[Orders] = (orderActor ? GetOrders).mapTo[Orders]
            complete(orders)
          }*/

        path("order" / IntNumber) { id =>
          val order: Future[Order] = (orderActor ? GetOrder(id)).mapTo[Order]
          rejectEmptyResponse {
            complete(order)
          }
        }

      }

}

    pathPrefix("/") {
      path("orders") {
        get {
          pathEnd {
            complete("OK")
            val orders: Future[Orders] = (orderActor ? GetOrders).mapTo[Orders]
            complete(orders)
          }
        }
      }
      path("order" / IntNumber) { id =>
        concat(
          get {
            val order: Future[Order] = (orderActor ? GetOrder(id)).mapTo[Order]
            rejectEmptyResponse {
              complete(order)
            }
          }
        )
      }
      path("order") {
        concat(
          pathEnd {
            post {
              entity(as[Order]) {
                createdOrder =>
                  val order = Order(DatabaseService.getFreeId, LocalDateTime.now().toString, createdOrder.destinationAddress, createdOrder.customerName, "confirmed")
                  val orderCreated: Future[ActionPerformed] = (orderActor ? OrderCreated(order)).mapTo[ActionPerformed]
                  onSuccess(orderCreated) { performed =>
                    log.info("Created order [{}]: {}", order.id, performed.description)
                    complete((StatusCodes.Created, performed))
                  }
              }
            }
          }
        )
      }
    }
}
*/