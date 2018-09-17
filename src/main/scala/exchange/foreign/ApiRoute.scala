package exchange.foreign

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object ApiRoute {

  case object SymbolsRequest

  case class SymbolsResponse(base: String,
                             rates: Map[String, Double])

  case class ConvertRequest(currencyFrom: String,
                            currencyTo: String,
                            valueFrom: Double)

  case class ForeignExchangeRequest(data: Seq[ConvertRequest])

  case class ConvertResponse(currencyFrom: String,
                             currencyTo: String,
                             valueFrom: Double,
                             valueTo: Double)

  case class ForeignExchangeResponse(data: Seq[ConvertResponse],
                                     errorCode: Int,
                                     errorMessage: String)

}

trait ApiRoute extends LazyLogging {

  import CustomDirectives._

  implicit val ec: ExecutionContext

  implicit val timeout: Timeout

  implicit val apiExchangeManager: ActorRef

  def timeRequest(ctx: RequestContext): Try[RouteResult] => Unit = {
    val start = System.currentTimeMillis()

    {
      result =>
        val d = System.currentTimeMillis() - start
        result match {
          case Success(Complete(_)) =>
            logger.info(s"[SUCCESS] ${ctx.request.method.name} ${ctx.request.uri} took: ${d}ms")
          case Success(Rejected(_)) =>
            logger.warn(s"[REJECTED] ${ctx.request.method.name} ${ctx.request.uri} took: ${d}ms")
          case Failure(_) =>
            logger.error(s"[FAILED] ${ctx.request.method.name} ${ctx.request.uri} took: ${d}ms")
        }
    }
  }

  def apiRoutes(): Route = aroundRequest(timeRequest)(ec) {
    pathEndOrSingleSlash {
      get {
        logger.debug("GET request")
        onSuccess(apiExchangeManager ? ApiRoute.SymbolsRequest) {
          case response: ApiRoute.SymbolsResponse =>
            logger.debug(s"GET response: ${response.asJson.noSpaces}")
            complete(response)
        }
      } ~ post {
        entity(as[ApiRoute.ForeignExchangeRequest]) { forExRequest =>
          logger.debug(s"POST request: ${forExRequest.asJson.noSpaces}")
          onComplete(apiExchangeManager ? forExRequest) {
            case Success(response: ApiRoute.ForeignExchangeResponse) =>
              logger.debug(s"POST response: ${response.asJson.noSpaces}")
              complete(response)
            case _ =>
              val response = ApiRoute.ForeignExchangeResponse(
                data = Nil,
                errorCode = 1,
                errorMessage = "Unexpected error"
              )
              logger.debug(s"POST response: ${response.asJson.noSpaces}")
              complete(StatusCodes.InternalServerError, response)
          }
        }
      }
    }
  }

}
