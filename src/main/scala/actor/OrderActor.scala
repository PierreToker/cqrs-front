package actor

import akka.actor.{Actor, ActorLogging, Props, Status}
import org.mongodb.scala.model.Filters.equal
import service.DatabaseService
import service.DatabaseService.collection
import spray.json.enrichAny
import util.JsonSupport

import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

final case class Order(id: Int, shippingDate: String, destinationAddress: String, customerName: String, status: String)
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
  final case class OrderRestored(order: Order)

  def props: Props = Props[OrderActor]
}

class OrderActor extends Actor with ActorLogging with JsonSupport {
  import OrderActor._
  import service.KafkaProducerService

  override def preRestart(reason:Throwable, message:Option[Any]){
    super.preRestart(reason, message)
    sender() ! Status.Failure(reason)
  }

  def displayingStatus(order: Order): Order = {
    val stringDescription = DatabaseService.getOrderStatus(Integer.parseInt(order.status)).description
    order.copy(status = stringDescription)
  }

  def receive: Receive = {
    case GetOrders =>
      sender() ! DatabaseService.getAllTransfers

    case GetOrder(id) => {
      DatabaseService.getOrderByID(id) match {
        case orderFounded:Order => sender() ! orderFounded
        case _ => sender() ! None
      }
    }

    case OrderSetToPrepared(order) =>
      KafkaProducerService.publish("OrderSetToPrepared",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} change his status to ${order.status}.")

    case OrderCreated(order) =>
      KafkaProducerService.publish("OrderCreated",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} created.")

    case OrderStatusUpdatedToNextStep(order) =>
      val orderModifier = order.copy(status = (order.status.toInt + 1).toString)
      KafkaProducerService.publish("OrderStatusUpdatedToNextStep",orderModifier.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} status updated to next step n° ${orderModifier.status}.")

    case OrderDeleted(order) =>
      KafkaProducerService.publish("OrderDeleted",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} deleted.")

    case OrderRestored(order) =>
      KafkaProducerService.publish("OrderRestored",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} restored.")
  }

}
