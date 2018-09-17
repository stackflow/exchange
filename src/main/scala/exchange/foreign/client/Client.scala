package exchange.foreign.client

import scala.concurrent.Future

object Client {

  sealed trait Entity

  final case class Convert(from: String,
                           to: String,
                           amount: Double,
                           result: Option[Double] = Option(0)) extends Entity

  final case class Currencies(base: String, rates: Map[String, Double]) extends Entity

}

trait ClientLike {

  def rates(): Future[Client.Currencies]

  def convert(c: Client.Convert): Future[Client.Convert]

}
