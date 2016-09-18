package com.github.corney.matching.trade

import akka.actor.{ActorLogging, FSM}
import com.github.corney.matching.domain.BidType.BidType
import com.github.corney.matching.domain.Finance.Finance
import com.github.corney.matching.domain.{BidType, Client, Order}
import com.github.corney.matching.trade.RequestMessages._
import com.github.corney.matching.trade.ResponseMessages._


/**
  * Created by corney on 17.09.16.
  */
class TradeMachine extends FSM[TradeState, Data] with ActorLogging {

  startWith(WaitingForClient, Clients(Map.empty))

  whenUnhandled {
    case Event(CheckBalance, data) =>
      log.debug("Checking balance")
      stay using data replying Balance(data.clients.values.toSeq)
    case Event(Stop, _) =>
      log.debug("Stop message received, terminating...")
      context.system.terminate()
      stop replying OK
    case Event(msg, _) =>
      log.info("Unknown message received: %s".format(msg))
      stay replying UnexpectedCommand
  }

  when(WaitingForClient) {
    case Event(SendClient(client), data) =>
      log.debug("Client(%s) added".format(client.name))
      stay using Clients(data.clients + (client.name -> client))
    case Event(AllClientsTransferred, data) =>
      log.debug("All clients are received, now waiting for orders...")
      goto(WaitingForOrder) using ClientsAndBids(data.clients, Map.empty) replying OK
  }

  when(WaitingForOrder) {
    case Event(SendOrder(order), ClientsAndBids(clients, orders)) =>
      log.debug("Order received: %s".format(order))
      clients.get(order.clientName) match {
        case None =>
          // Мы не можем добавить заказ от несуществующего клиента
          log.warning("Client(%s) does not exist".format(order.clientName))
          stay replying ClientNotFound
        case Some(client) =>
          if (isOrderValid(order, client)) {
            val peerKey = OrderKey.peer(order)
            orders.get(peerKey) match {
              case Some(Nil) | None =>
                // Совпадения не найдено, добавляем
                // Добавляем заказ в очередь
                log.debug("Order is added to queue")
                queueOrder(order, clients, orders)
              case Some(peers) =>
                // Среди всех совпадающих заказов надо найти такой, чтобы был валиден для данной операции
                // То есть, на счету должно быть достаточно средств для покупки
                // Или должно быть достаточное количество ценных бумаг для продажи
                // Кроме того, клиент не должен продавать бумаги сам себе
                peers.map(o => (o, clients(o.clientName))).find(t => client != t._2 && isOrderValid(t._1, t._2)) match {
                  case Some((peerOrder, peerClient)) =>
                    // Совпадение обнаружено, вычисляем новое состояние
                    val rest = peers.filter(o => peerOrder != o)

                    val transfer1 = makeTransfer(client, order)
                    val transfer2 = makeTransfer(peerClient, peerOrder)

                    (transfer1, transfer2) match {
                      case (Left(c1), Left(c2)) =>
                        log.debug("Making transfer between %s and %s".format(client.name, peerClient.name))
                        stay using ClientsAndBids(
                          clients = clients +(c1.name -> c1, c2.name -> c2),
                          orders = orders + (peerKey -> rest)
                        )

                      case (Right(failure), _) =>
                        // Сначала проверяем на ошибку одного клиента
                        log.debug("Not enough resources to process order")
                        stay
                      case (_, Right(failure)) =>
                        // Потом - второго
                        // Этого не может произойти
                        log.warning("Not enough resources to process order")
                        stay

                    }
                  case None =>
                    // В списке заказов не найдено подходящего, добавляем в очередь для последущей обработки
                    log.debug("Order is added to queue")
                    queueOrder(order, clients, orders)

                }

            }
          } else {
            // Мы не можем выполнить этот заказ, так как для него не хватает ресурсов
            log.debug("Order is not valid")
            stay
          }
      }
  }

  def isOrderValid(order: Order, client: Client): Boolean = {
    order.bidType match {
      case BidType.Sell =>
        client.amounts.getOrElse(order.finance, BigInt(0)) >= order.amount
      case BidType.Buy =>
        order.amount * order.price <= client.balance
    }
  }

  def queueOrder(order: Order, clients: Map[String, Client], orders: Map[OrderKey, List[Order]]): State = {
    val key = OrderKey(order)
    val updated = orders.get(key) match {
      case Some(list) =>
        list :+ order
      case None =>
        List(order)
    }
    stay using ClientsAndBids(clients, orders + (key -> updated))
  }

  def makeTransfer(client: Client, order: Order): Either[Client, ResponseFailure] = {
    if (client.name != order.clientName) {
      Right(ClientNotMatched)
    } else {
      order.bidType match {
        case BidType.Sell =>
          // Клиент продает
          val oldAmount: BigInt = client.amounts.getOrElse(order.finance, BigInt(0))
          if (oldAmount < order.amount) {
            Right(LackOfResources)
          } else {
            val newAmount = oldAmount - order.amount
            Left(client.copy(
              balance = client.balance + order.amount * order.price,
              amounts = client.amounts + (order.finance -> newAmount)
            ))
          }
        case BidType.Buy =>
          // Клиент покупает
          val cost = order.amount * order.price
          if (cost > client.balance) {
            Right(LackOfResources)
          } else {
            val newAmount = client.amounts.get(order.finance) match {
              case None =>
                order.amount
              case Some(oldAmount) =>
                oldAmount + order.amount
            }
            Left(
              client.copy(
                balance = client.balance - cost,
                amounts = client.amounts + (order.finance -> newAmount)
              )
            )
          }
      }
    }
  }
}

sealed trait TradeState

case object WaitingForClient extends TradeState

case object WaitingForOrder extends TradeState

object OrderKey {
  def apply(order: Order): OrderKey = {
    OrderKey(
      finance = order.finance,
      bidType = order.bidType,
      price = order.price,
      amount = order.amount
    )
  }

  def peer(order: Order): OrderKey = {
    OrderKey(
      finance = order.finance,
      bidType = if (order.bidType == BidType.Sell) BidType.Buy else BidType.Sell,
      price = order.price,
      amount = order.amount
    )
  }
}

case class OrderKey(finance: Finance, bidType: BidType, price: BigInt, amount: BigInt)

sealed trait Data {
  def clients: Map[String, Client]
}

case class Clients(clients: Map[String, Client]) extends Data

case class ClientsAndBids(clients: Map[String, Client], orders: Map[OrderKey, List[Order]]) extends Data
