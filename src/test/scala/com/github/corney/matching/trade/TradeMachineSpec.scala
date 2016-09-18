package com.github.corney.matching.trade


import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import akka.util.Timeout
import com.github.corney.matching.domain.{BidType, Client, Finance, Order}
import Finance._
import com.github.corney.matching.trade.RequestMessages._
import com.github.corney.matching.trade.ResponseMessages._
import org.specs2.matcher.Matchers
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

/**
  * Created by corney on 17.09.16.
  */
class TradeMachineSpec extends TestKit(ActorSystem("TradeMachineSpec")) with SpecificationLike with ImplicitSender with Matchers {


  implicit val timeout = Timeout(200 millis)

  sequential

  val tradeMachine = TestFSMRef(new TradeMachine)


  "TradeMachine" should {

    val clients = Seq(
      Client(name = "green", balance = 1000, amounts = Map(A -> 100, B -> 200)),
      Client(name = "orange", balance = 0, amounts = Map(C -> 1000, D -> 2000)),
      Client(name = "yellow", balance = 0, amounts = Map(A -> 100, B -> 100, C -> 100, D -> 100)),
      Client(name = "pink", balance = 9600, amounts = Map.empty)
    )


    "Successfully loads clients" in {


      clients.foreach(client => {
        val result = tradeMachine ! SendClient(client)
      })

      tradeMachine ! CheckBalance

      expectMsgPF() {
        case Balance(balance) =>
          balance.toSet must be_==(clients.toSet)
      }

      tradeMachine ! AllClientsTransferred
      expectMsg(OK)

      ok
    }

    "Loads orders" in {
      tradeMachine ! SendOrder(Order("green", BidType.Sell, A, 20, 50))

      tradeMachine ! SendOrder(Order("green", BidType.Sell, C, 20, 50))

      tradeMachine ! SendOrder(Order("orange", BidType.Sell, C, 200, 25))

      tradeMachine ! SendOrder(Order("yellow", BidType.Buy, A, 20, 50))

      tradeMachine ! SendOrder(Order("pink", BidType.Buy, C, 200, 25))

      tradeMachine ! SendOrder(Order("pink", BidType.Buy, D, 100, 25))

      tradeMachine ! SendOrder(Order("yellow", BidType.Sell, D, 100, 25))

      tradeMachine ! SendOrder(Order("pink", BidType.Buy, A, 20, 50))


      tradeMachine ! CheckBalance

      expectMsgPF() {
        case Balance(balance) =>
          balance.toSet must be_==(Set(
            Client(name = "green", balance = 2000, amounts = Map(A -> 80, B -> 200)),
            Client(name = "orange", balance = 5000, amounts = Map(C -> 800, D -> 2000)),
            Client(name = "yellow", balance = 2500, amounts = Map(A -> 100, B -> 100, C -> 100, D -> 0)),
            Client(name = "pink", balance = 1100, amounts = Map(A -> 20, C -> 200, D -> 100))
          ))
      }

      ok
    }

    "Succesfully stops" in {
      tradeMachine ! Stop
      expectMsg(OK)

      ok
    }


  }


}
