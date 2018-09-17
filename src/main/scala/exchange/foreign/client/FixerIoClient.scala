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

object FixerIoClient {

  def apply(config: Config)(implicit system: ActorSystem, m: Materializer): FixerIoClient = {
    new FixerIoClient(config)
  }

  case class Symbols(success: Boolean,
                     symbols: Map[String, String])

  case class Query(from: String,
                   to: String,
                   amount: Double)

  case class Info(timestamp: Long,
                  rate: Double)

  case class Convert(success: Boolean,
                     query: Query,
                     info: Info,
                     result: Double)

  case class Latest(success: Boolean,
                    base: String,
                    rates: Map[String, Double])

}

class FixerIoClient(config: Config)(implicit system: ActorSystem, m: Materializer) extends ClientLike with LazyLogging {

  import system.dispatcher

  val baseUrl: String = config.getString("base_url")

  val accessKey: String = config.getString("access_key")

  def request(endpoint: String, params: Map[String, String] = Map.empty): Future[HttpResponse] = {
    val uri = params.foldLeft(baseUrl + endpoint + s"?access_key=$accessKey") { case (acc, param) =>
      acc + s"&${param._1}=${param._2}"
    }
    Http().singleRequest(HttpRequest(uri = uri))
  }

  override def rates(): Future[Client.Currencies] = {
    request("latest").flatMap(Unmarshal(_).to[Client.Currencies]) map { response =>
      response.copy(rates = response.rates + (response.base -> 1))
    }
  }

  override def convert(convert: Client.Convert): Future[Client.Convert] = {
    val params = Map(
      "from" -> convert.from,
      "to" -> convert.to,
      "amount" -> convert.amount.toString
    )
    request("convert", params).flatMap(Unmarshal(_).to[FixerIoClient.Convert]) map { response =>
      convert.copy(result = Some(response.result))
    }
  }

}
