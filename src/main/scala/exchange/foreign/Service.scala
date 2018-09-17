package exchange.foreign

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import exchange.foreign.client.{ClientLike, OpenExchangeRatesOrgClient, FixerIoClient, StubClient}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import net.ceedubs.ficus.Ficus._

object Service extends App with ApiRoute with LazyLogging {

  implicit val system: ActorSystem = ActorSystem("ExchangeHttpServer")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()

  val config: Config = ConfigFactory.load()
  val foreignClient: ClientLike = config.as[Option[String]]("app.interface").getOrElse("stub") match {
    case "fixerio" =>
      FixerIoClient(config.getConfig("app.client.fixerio"))

    case "openexchangeratesorg" =>
      OpenExchangeRatesOrgClient(config.getConfig("app.client.openexchangeratesorg"))

    case _ =>
      StubClient()
  }
  implicit val apiExchangeManager: ActorRef = system.actorOf(ExchangeManager.props(foreignClient))

  implicit val timeout: Timeout = 5.second

  val interface: String = config.as[Option[String]]("app.interface").getOrElse("localhost")
  val port: Int = config.as[Option[Int]]("app.port").getOrElse(8080)

  lazy val routes = apiRoutes()

  Http().bindAndHandle(routes, interface, port)

  logger.info(s"Server online at http://$interface:$port/")

  Await.result(system.whenTerminated, Duration.Inf)

}
