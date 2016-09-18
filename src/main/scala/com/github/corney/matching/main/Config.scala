package com.github.corney.matching.main

import java.io.File

/**
  * Created by corney on 17.09.16.
  */
case class Config(
                   clients: File = new File("sample/clients.txt"),
                   orders: File = new File("sample/orders.txt"),
                   result: File = new File("./result.txt")
                 )

object Config {
  def get(args: Array[String]): Option[Config] = {
    new scopt.OptionParser[Config]("trade-matching") {
      head("trade-matching", "0.1")

      opt[File]('c', "clients").
        action( (x, c) => c.copy(clients = x) ).
        text("path to clients.txt")

      opt[File]('o', "orders").
        action( (x, c) => c.copy(orders = x) ).
        text("path to orders.txt")

      opt[File]('r', "result").
        action( (x, c) => c.copy(result = x) ).
        text("path to result.txt")
    }.parse(args, Config())

  }
}