package exchange.foreign.client
import scala.concurrent.{ExecutionContext, Future}

case class StubClient()(implicit val ec: ExecutionContext) extends ClientLike {

  override def convert(c: Client.Convert): Future[Client.Convert] = {
    Future.successful(c)
  }

  override def rates(): Future[Client.Currencies] = {
    Future.successful(Client.Currencies(base = "USD", rates = Map("USD" -> 1, "RUB" -> 70, "EUR" -> 1.2)))
  }

}
