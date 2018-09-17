package exchange.foreign

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import exchange.foreign.client.StubClient
import io.circe.generic.auto._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._


class ApiStubClientSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest with ApiRoute {

  implicit val ec = system.dispatcher
  implicit val timeout = 5.second
  implicit val apiExchangeManager = system.actorOf(ExchangeManager.props(StubClient()))

  "ApiRoutes for stub client" should {
    "return rates (GET /)" in {
      val request = HttpRequest(uri = "http://localhost/")

      request ~> apiRoutes() ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        val response = entityAs[ApiRoute.SymbolsResponse]
        response.base should be("USD")
        response.rates.size should be > 1
      }
    }

    "return zero values for unknown currencies (POST /)" in {
      val req = ApiRoute.ForeignExchangeRequest(List(ApiRoute.ConvertRequest("USD", "", 100)))
      val entity = Marshal(req).to[RequestEntity].futureValue
      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = "http://localhost/",
        entity = entity
      )

      request ~> apiRoutes() ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        val response = entityAs[ApiRoute.ForeignExchangeResponse]
        response.errorCode should be(0)
        response.errorMessage.length should be > 0
        response.data.head.valueTo shouldBe 0
      }
    }

    "return equal values for same currency (POST /)" in {
      val req = ApiRoute.ForeignExchangeRequest(List(ApiRoute.ConvertRequest("USD", "USD", 1001)))
      val entity = Marshal(req).to[RequestEntity].futureValue
      val request = HttpRequest(
        method = HttpMethods.POST,
        uri = "http://localhost/",
        entity = entity
      )

      request ~> apiRoutes() ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
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
