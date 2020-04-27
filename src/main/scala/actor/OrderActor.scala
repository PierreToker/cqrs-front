package actor

import akka.actor.Status.{Failure, Success}
import akka.actor.{Actor, ActorLogging, Props, Status}
import javafx.css.ParsedValue
import org.mongodb.scala.bson.annotations.BsonProperty
import service.DatabaseService
import spray.json.enrichAny
import util.JsonSupport

import scala.concurrent.Future

final case class Order(id: Int, shippingDate: String, destinationAddress: String, customerName: String, status: String)
final case  class OrderStatus(id: Int, description: String)
final case class Orders(orders: Seq[Order])
//final case class OrderStatus(value: String)

/*
final case class TransferUpdateEvent(transfer: Order, newStatus: OrderStatus)
final case class TransferDeleteEvent(transfer: Order)
 */

object OrderActor {
  final case class ActionPerformed(description: String)
  final case class GetOrders()
  final case class OrderCreated(order: Order)
  final case class OrderSetToPrepared(order: Order)
  final case class GetOrder(id: Int)
  final case class OrderStatusUpdatedToNextStep(order: Order)
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

    case GetOrder(id) => {
      val orderInit = DatabaseService.getOrderByID(id)
      val stringDescription = DatabaseService.getOrderStatus(Integer.parseInt(orderInit.status)).description
      val orderParsed = orderInit.copy(status = stringDescription)
      sender() ! orderParsed
    }

    case OrderSetToPrepared(order) =>
      KafkaProducerService.publish("OrderSetToPrepared",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} change his status to ${order.status}.")

    case OrderCreated(order) =>
      KafkaProducerService.publish("OrderCreated",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} created.")

    case OrderStatusUpdatedToNextStep(order) =>
      KafkaProducerService.publish("OrderStatusUpdatedToNextStep",order.toJson.prettyPrint)
      sender() ! ActionPerformed(s"Order ${order.id} status updated to next step ${order.status}.")

    case OrderDeleted(id) =>
      KafkaProducerService.publish("OrderDeleted",id.toString)
      sender() ! ActionPerformed(s"Order $id deleted.")

  }

}
