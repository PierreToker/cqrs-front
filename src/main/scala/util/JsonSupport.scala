package util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import actor.OrderActor.ActionPerformed
import actor._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport {
  import DefaultJsonProtocol._

  implicit val orderJsonFormat: RootJsonFormat[Order] = jsonFormat5(Order)
  implicit val ordersJsonFormat: RootJsonFormat[Orders] = jsonFormat1(Orders)
  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed] = jsonFormat1(ActionPerformed)

}

