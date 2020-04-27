import actor.{Order, OrderActor, Orders, OrderStatus}
import akka.actor.ActorRef
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ContentTypes, MessageEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import route.OrderRoutes

class OrderRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with OrderRoutes {

  override val orderActor: ActorRef = system.actorOf(OrderActor.props, "OrderActor")

  lazy val routes: Route = transferRoutes

  "OrderRoutes" should {
    /*
    "Should be able to get all orders (GET /orders)" in {
      val request = Get("/orders")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[Orders].orders.size should ===(2)
      }
    }
*/
    "Should be able to get a single order (GET /order/{id})" in {
      val request = Get("/order/0")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"id":"0","shippingDate":"2020-04-22T11:06:27.996849800","destinationAddress":"150 avenue de la victoire","customerName":"Dupont","status":"prepared"}""")
      }
    }

    /*
    "be able to add transfer (POST /transfers)" in {
      val transfer = Transfer(5, "account1", "account2", 100.0, "EUR", "pending")
      val userEntity = Marshal(transfer).to[MessageEntity].futureValue

      val request = Post("/transfers").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"description":"Transfer 5 created."}""")
      }
    }

    "be able to update transfer (PUT /transfers/{id}/status)" in {
      val transferStatus = OrderStatus("processing")
      val statusEntity = Marshal(transferStatus).to[MessageEntity].futureValue

      val request = Put("/transfers/1/status").withEntity(statusEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"description":"Transfer 1 updated with status 'processing'."}""")
      }
    }

    "be able to remove transfers (DELETE /transfers/{id})" in {
      val request = Delete(uri = "/transfers/1")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"description":"Transfer 1 deleted."}""")
      }
    }
    */
  }
}
