package actor

import akka.actor.{Actor, ActorLogging, Props, Status}
import service.DatabaseService
import spray.json.enrichAny
import util.JsonSupport

final case class Order(id: Int, shippingDate: String, destinationAddress: String, customerName: String, status: String)
final case class Orders(orders: Seq[Order])
final case class OrderStatus(value: String)

/*
final case class TransferUpdateEvent(transfer: Order, newStatus: OrderStatus)
final case class TransferDeleteEvent(transfer: Order)
 */

object OrderActor {
  final case class ActionPerformed(description: String)
  final case object GetOrders
  final case class OrderCreated(order: Order)
  final case class OrderSetToPrepared(order: Order)
  final case class GetOrder(id: Int)
  final case class UpdateTransferStatus(id: Int, status: OrderStatus)
  final case class OrderDeleted(id: Int)

  def props: Props = Props[OrderActor]
}

class OrderActor extends Actor with ActorLogging with JsonSupport {
  import OrderActor._
  import service.KafkaProducerService

  override def preRestart(reason:Throwable, message:Option[Any]){
    super.preRestart(reason, message)
    sender() ! Status.Failure(reason)
  }

  def receive: Receive = {

    case GetOrders =>
      sender() ! DatabaseService.getAllTransfers

    case GetOrder(id) =>
      sender() ! DatabaseService.getOrderByID(id)

    case OrderSetToPrepared(order) =>
      KafkaProducerService.publish("OrderSetToPrepared",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} change his status to ${order.status}.")

    case OrderCreated(order) =>
      KafkaProducerService.publish("OrderCreated",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Transfer ${order.id} created.")

    case UpdateTransferStatus(id, status) =>
      KafkaProducerService.publish("update",status.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Transfer $id updated with status '${status.value}'.")

    case OrderDeleted(id) =>
      KafkaProducerService.publish("OrderDeleted",id.toString)
      sender() ! ActionPerformed(s"Transfer $id deleted.")

  }

}
