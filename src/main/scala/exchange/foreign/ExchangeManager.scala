package exchange.foreign

import akka.actor.{Actor, Props, Stash}
import com.typesafe.scalalogging.LazyLogging
import exchange.foreign.client.{Client, ClientLike}

import scala.concurrent.Future

object ExchangeManager {

  def props(foreignClient: ClientLike): Props = {
    Props(classOf[ExchangeManager], foreignClient)
  }

  case class Currencies(base: String, rates: Map[String, Double])

}

class ExchangeManager(foreignClient: ClientLike) extends Actor with Stash with LazyLogging {

  import context.dispatcher

  override def preStart(): Unit = {
    foreignClient.rates() map { c =>
      self ! ExchangeManager.Currencies(base = c.base, rates = c.rates)
    }
    super.preStart()
  }

  override def receive: Receive = stashReceive

  def stashReceive: Receive = {
    case c: ExchangeManager.Currencies =>
      context.become(cachedCurrencies(c))
      unstashAll()

    case _ =>
      stash()
  }

  def cachedCurrencies(currencies: ExchangeManager.Currencies): Receive = {
    case currencies: ExchangeManager.Currencies =>
      context.become(cachedCurrencies(currencies))

    case ApiRoute.SymbolsRequest =>
      val lastSender = sender()
      lastSender ! ApiRoute.SymbolsResponse(base = currencies.base, rates = currencies.rates)

    case forExRequest: ApiRoute.ForeignExchangeRequest =>
      val lastSender = sender()
      val data = forExRequest.data map { exchange =>
//        val convert = Client.Convert(
//          from = exchange.currencyFrom,
//          to = exchange.currencyTo,
//          amount = exchange.valueFrom
//        )
//        foreignClient.convert(convert).recover { case _ =>
//          convert
//        }
        Future {
          Client.Convert(
            from = exchange.currencyFrom,
            to = exchange.currencyTo,
            amount = exchange.valueFrom,
            result = currencies.rates.get(exchange.currencyFrom) flatMap { from =>
              currencies.rates.get(exchange.currencyTo) map { to =>
                exchange.valueFrom * to / from
              }
            }
          )
        }
      }
      for {
        converts <- Future.sequence(data)
      } yield {
        lastSender ! ApiRoute.ForeignExchangeResponse(
          data = converts.map { c =>
            ApiRoute.ConvertResponse(
              currencyFrom = c.from,
              currencyTo = c.to,
              valueFrom = c.amount,
              valueTo = c.result.getOrElse(0)
            )
          },
          errorCode = 0,
          errorMessage = "No errors"
        )
      }
  }

}
