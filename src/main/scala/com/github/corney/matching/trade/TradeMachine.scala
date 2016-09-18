package com.github.corney.matching.trade

import akka.actor.{ActorLogging, FSM}
import com.github.corney.matching.domain.{BidType, Client, Finance, Order}
import BidType.BidType
import Finance.Finance
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
      stop replying Processed
    case Event(msg, _) =>
      log.info("Unknown message received: %s".format(msg))
      stay replying UnexpectedCommand
  }

  when(WaitingForClient) {
    case Event(SendClient(client), data) =>
      log.debug("Client(%s) added".format(client.name))
      stay using Clients(data.clients + (client.name -> client)) replying Processed
    case Event(AllClientsTransferred, data) =>
      log.debug("All clients are received, now waiting for orders...")
      goto(WaitingForOrder) using ClientsAndBids(data.clients, Map.empty) replying Processed
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
          val reverse = OrderKey.reverse(order)
          orders.get(reverse) match {
            case Some(Nil) | None =>
              // Совпадения не найдено, добавляем
              // Добавляем заказ в очередь
              if (isOrderValid(order, client)) {
                log.debug("Order is added to queue")
                queueOrder(order, clients, orders)
              } else {
                log.debug("Order is not valid")
                stay replying LackOfResources
              }

            case Some(reverseOrder :: tail) =>
              // Совпадение обнаружено, вычисляем новое состояние
              clients.get(reverseOrder.clientName) match {
                case None =>
                  // Данной ситуации быть не может, так как мы не разрешаем вставлять заказы от несуществующих клиентов
                  log.warning("Order from unknown client(%s) are stored".format(reverseOrder.clientName))
                  stay replying ClientNotFound
                case Some(reverseClient) =>
                  // TODO если операция с первым отматченным клиентом не получилась, нужно скипать его и идти дальше
                  // Пока список клиентов не окажется пустым
                  // в этом случае нужно просто добавить заявку в очередь
                  val forwardTransfer = makeTransfer(client, order)
                  val reverseTransfer = makeTransfer(reverseClient, reverseOrder)

                  (forwardTransfer, reverseTransfer) match {
                    case (Left(c1), Left(c2)) =>
                      log.debug("Making transfer between %s and %s".format(client.name, reverseClient.name))
                      stay using ClientsAndBids(
                        clients = clients + (c1.name -> c1, c2.name -> c2),
                        orders = orders + (reverse -> tail)
                      ) replying Processed

                    case (Right(failure), _) =>
                      // Сначала проверяем на ошибку одного клиента
                      log.debug("Not enough resources to process order")
                      stay replying failure
                    case (_, Right(failure)) =>
                      // TODO вот как раз этого кейза надо избежать
                      log.warning("Not enough resources to process order 2")
                      // Потом - второго
                      stay replying failure
                  }
              }
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
    stay using ClientsAndBids(clients, orders + (key -> updated)) replying Enqueued
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

  def reverse(order: Order): OrderKey = {
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
