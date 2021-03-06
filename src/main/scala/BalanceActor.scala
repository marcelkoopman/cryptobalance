import java.time.LocalDateTime

import BalanceActor._
import HttpActor.{BtcBalance, BtcEUR, EthBalance, EthEUR}
import akka.actor.{Actor, ActorLogging, Props}

/**
  * Created by marcel on 26-05-17.
  */

object BalanceActor {
  def props: Props = Props[BalanceActor]

  case class RetrieveBalance()

  case class ReceiveEthEurPrice(price: BigDecimal)

  case class ReceiveBtcEurPrice(price: BigDecimal)

  case class ReceiveEthBalance(price: BigDecimal)

  case class ReceiveBtcBalance(price: BigDecimal)

}

class BalanceActor extends Actor with ActorLogging {

  private val httpActor = context.actorOf(HttpActor.props, "httpActor")

  import scala.collection.mutable.ListBuffer

  private val memoryDump = new ListBuffer[String]()
  private var balanceMap = scala.collection.mutable.Map[String, BigDecimal]()
  private var priceHistory = scala.collection.mutable.Map[LocalDateTime, BigDecimal]()

  def receive = {
    case RetrieveBalance() =>
      httpActor ! EthEUR
      httpActor ! BtcEUR

    case ReceiveEthEurPrice(price: BigDecimal) =>
      httpActor ! EthBalance("0xbd31fa88f89699ff2eb3d66b449de77e79bb2053", price)

    case msg: ReceiveBtcEurPrice =>
      httpActor ! BtcBalance("1DqdB2Ce8hpkX2XsU9aBmn2ZC2WW2UEXXL", msg.price)

    case ReceiveEthBalance(price: BigDecimal) =>
      balanceMap += ("ETH" -> price)
      printTotalBalance

    case ReceiveBtcBalance(price: BigDecimal) =>
      balanceMap += ("BTC" -> price)
      printTotalBalance
  }

  private def printTotalBalance(): Unit = {
    if (balanceMap.size == 2) {
      memoryDump += memoryDump + "" + balanceMap.size
      print(balanceMap)
      val newPrice = balanceMap.values.sum
      if (priceHistory.isEmpty) {
        //
      } else {
        val high = priceHistory.values.max
        val diff = newPrice - high
        if (diff == 0) {
          //
        } else if (diff < 0) {
          print("DOWN", diff)
        } else {
          print("UP", diff)
        }
      }
      priceHistory += (LocalDateTime.now() -> newPrice)
      balanceMap.clear()
      log.info("----------------")
    }
  }

  val euroFormat = java.text.NumberFormat.getCurrencyInstance
  euroFormat.setMaximumFractionDigits(4)

  private def print(balanceMap: scala.collection.mutable.Map[String, BigDecimal]) = {
    balanceMap.map {
      value => log.info("{} {}", value._1, euroFormat.format(value._2))
    }
    log.info("Total {}", euroFormat.format(balanceMap.values.sum))
  }

  private def print(text: String, value: BigDecimal) = log.info("{} {}", text, euroFormat.format(value))
}