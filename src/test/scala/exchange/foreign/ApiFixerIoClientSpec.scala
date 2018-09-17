package exchange.foreign

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import exchange.foreign.client.FixerIoClient
import io.circe.generic.auto._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class ApiFixerIoClientSpec extends WordSpec
  with Matchers with ScalaFutures with ScalatestRouteTest with ApiRoute {

  implicit val timeout = 5.second
  implicit val ec = system.dispatcher

  val config: Config = ConfigFactory.load()
  val client = FixerIoClient(config.getConfig("app.client.fixerio"))(system, materializer)
  implicit val apiExchangeManager = system.actorOf(ExchangeManager.props(client))
  lazy val routes = apiRoutes()

  "ApiRoutes for fixer.io client" should {
    "return rates (GET /)" in {
      val request = HttpRequest(uri = "http://localhost/")

      implicit val timeout = RouteTestTimeout(2.seconds)
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        val response = entityAs[ApiRoute.SymbolsResponse]
        response.base should be("EUR")
        response.rates.size should be > 0
      }
    }

    "return zero values for unknown currencies (POST /)" in {
      val req = ApiRoute.ForeignExchangeRequest(List(ApiRoute.ConvertRequest("USB", "RUB", 100)))
      val entity = Marshal(req).to[RequestEntity].futureValue
      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = "http://localhost/",
        entity = entity
      )

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        val response = entityAs[ApiRoute.ForeignExchangeResponse]
        response.errorCode should be(0)
        response.errorMessage.length should be > 0
        response.data.head.valueTo should be(0)
      }
    }

    "return equal values for same currency (POST /)" in {
      val req = ApiRoute.ForeignExchangeRequest(List(ApiRoute.ConvertRequest("EUR", "EUR", 777)))
      val entity = Marshal(req).to[RequestEntity].futureValue
      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = "http://localhost/",
        entity = entity
      )

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        val response = entityAs[ApiRoute.ForeignExchangeResponse]
        response.errorCode should be(0)
        response.errorMessage.length should be > 0
        response.data.head.valueTo shouldBe req.data.head.valueFrom
      }
    }
  }

}
