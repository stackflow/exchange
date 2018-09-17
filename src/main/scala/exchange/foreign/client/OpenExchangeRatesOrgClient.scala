package exchange.foreign.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.Future

object OpenExchangeRatesOrgClient {

  case class ConvertRequest(from: String,
                            to: String,
                            amount: Double,
                            query: String)

  case class Convert(request: ConvertRequest,
                     response: Double)

  case class Currencies(base: String, rates: Map[String, Double])

  def apply(config: Config)(implicit system: ActorSystem, m: Materializer): OpenExchangeRatesOrgClient = {
    new OpenExchangeRatesOrgClient(config)
  }

}

class OpenExchangeRatesOrgClient(config: Config)(implicit system: ActorSystem, m: Materializer) extends ClientLike with LazyLogging {

  import system.dispatcher

  val baseUrl: String = config.getString("base_url")

  val appId: String = config.getString("app_id")

  def request(endpoint: String): Future[HttpResponse] = {
    val uri = baseUrl + endpoint + s"?app_id=$appId"
    logger.info(s"Client request: $uri")
    Http().singleRequest(HttpRequest(uri = uri))
  }

  override def rates(): Future[Client.Currencies] = {
    request("latest.json")
      .flatMap(Unmarshal(_).to[Client.Currencies])
      .map { response =>
        response.copy(rates = response.rates + (response.base -> 1))
      }
  }

  override def convert(convert: Client.Convert): Future[Client.Convert] = {
    request(s"convert/${convert.amount}/${convert.from}/${convert.to}")
      .flatMap(Unmarshal(_).to[OpenExchangeRatesOrgClient.Convert])
      .map { result =>
        convert.copy(result = Some(result.response))
      }
  }

}
