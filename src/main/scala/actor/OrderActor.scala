package actor

import akka.actor.{Actor, ActorLogging, Props, Status}
import service.DatabaseService
import spray.json.enrichAny
import util.JsonSupport

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.Try

final case class Order(id: Option[Int] = None, shippingDate: String, destinationAddress: String, customerName: String, status: Option[String] = Some("0"))
final case class OrderStatus(id: Int, description: String)
final case class Orders(orders: Seq[Order])

object OrderActor {
  final case class ActionPerformed(description: String)
  final case class GetOrders()
  final case class OrderCreated(order: Order)
  final case class OrderSetToPrepared(order: Order)
  final case class GetOrder(id: Int)
  final case class OrderStatusUpdatedToNextStep(order: Order)
  final case class OrderDeleted(order: Order)
  final case class OrderRestored(order: Int)

  def props: Props = Props[OrderActor]
}

class OrderActor extends Actor with ActorLogging with JsonSupport {
  import OrderActor._
  import service.KafkaProducerService

  override def preRestart(reason:Throwable, message:Option[Any]){
    super.preRestart(reason, message)
    sender() ! Status.Failure(reason)
  }

  def displayingStatus(order: Order): Option[Order] = {
    val orderStatus:OrderStatus = DatabaseService.getOrderStatus(order.status.getOrElse(0).asInstanceOf[String].toInt)
    orderStatus match {
      case _:OrderStatus => {
        Some(order.copy(status = Some(orderStatus.description)))
      }
      case _ => None
    }

    /*
    DatabaseService.getOrderStatus(order.status.getOrElse(0).asInstanceOf[String].toInt).description match {
      case result:String => Some(order.copy(status = Some(result)))
      case _ => None
    }
     */
    /* Without Some(Status) version
    DatabaseService.getOrderStatus(Integer.parseInt(order.status)).description match {
      case result:String => Some(order.copy(status = result))
      case _ => None
    }
     */
  }

  def receive: Receive = {
    case GetOrders =>
      sender() ! DatabaseService.getAllTransfers

    case GetOrder(id) => {
      DatabaseService.getOrderByID(id) match {
        case orderFounded:Order => {
          displayingStatus(orderFounded) match {
            case orderFormatted:Option[Order] => sender() ! orderFormatted
            case _ => sender() ! None
          }
        }
        case _ => sender() ! None
      }
    }

    case OrderSetToPrepared(order) =>
      KafkaProducerService.publish("OrderSetToPrepared",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} change his status to ${order.status}.")

    case OrderCreated(order) =>
      KafkaProducerService.publish("OrderCreated",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} created.")

    case OrderStatusUpdatedToNextStep(order) => {
      val statusId:Int = DatabaseService.getStatusIdFromDescription(order.status.getOrElse(0).toString)
      val newOrderStatus:OrderStatus = DatabaseService.getOrderStatus(statusId + 1)
      newOrderStatus match {
        case _:OrderStatus => {
          val orderModifier:Order = order.copy(status = Some(newOrderStatus.id.toString))
          KafkaProducerService.publish("OrderStatusUpdatedToNextStep", orderModifier.toJson.prettyPrint)
          sender() ! ActionPerformed(s"Order ${order.id} status updated to next step n° ${orderModifier.status}.")
        }
        case _ => sender() ! ActionPerformed("This order cannot increased his status.")
      }
    }

    case OrderDeleted(order) =>
      KafkaProducerService.publish("OrderDeleted",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} deleted.")

    case OrderRestored(orderId) =>
      KafkaProducerService.publish("OrderRestored",orderId.toString)
      sender() ! ActionPerformed(s"Order ${orderId} restored.")
  }

}
