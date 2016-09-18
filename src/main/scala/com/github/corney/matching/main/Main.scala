package com.github.corney.matching.main

import java.io.PrintWriter

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.corney.matching.convert.{ClientConverter, OrderConverter}
import com.github.corney.matching.trade.RequestMessages._
import com.github.corney.matching.trade.ResponseMessages.Balance
import com.github.corney.matching.trade.TradeMachine

import scala.concurrent.duration._
import scala.io.Source

case object Start


class Main(config: Config) extends Actor with ActorLogging {

  val tradeMachine = context.actorOf(Props[TradeMachine], name = "trademachine")
  implicit val timeout = Timeout(200 milliseconds)

  override def receive = {
    case Start =>
      log.info("Processing clients...")
      Source
        .fromFile(config.clients)
        .getLines()
        .foreach(line => {
          ClientConverter(line) match {
            case Left(client) =>
              tradeMachine ? SendClient(client)
            case Right(error) =>
              log.warning("%s: %s", error, line)
          }
        })

      log.info("done")
      tradeMachine ? AllClientsTransferred
      log.info("Processing orders...")

      Source
        .fromFile(config.orders)
        .getLines()
        .foreach(line => {
          OrderConverter(line) match {
            case Left(order) =>
              val result = tradeMachine ? SendOrder(order)
            case Right(error) =>
              log.warning("%s: %s", error, line)
          }
        })

      log.info("done")

      tradeMachine ! CheckBalance

    case Balance(balance) =>
      log.info("Writing new balances...")
      val writer = new PrintWriter(config.result)
      try {
        balance.foreach(client => writer.println(ClientConverter(client)))
      } finally {
        log.info("done")
        writer.close()
      }

      log.info("Stopping system...")
      tradeMachine ? Stop
      context.stop(self)
      log.info("done")
  }
}

/**
  * Created by corney on 17.09.16.
  */
object Main {
  def main(args: Array[String]) {

    Config.get(args) match {
      case Some(config) =>

        val system = ActorSystem("TradeMachine")

        val main = system.actorOf(Props(new Main(config)), "main")

        main ! Start
      case None =>
      // Do nothing
    }
  }
}
