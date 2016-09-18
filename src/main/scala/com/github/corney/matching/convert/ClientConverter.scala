package com.github.corney.matching.convert

import com.github.corney.matching.domain.Client
import com.github.corney.matching.domain.Finance._

/**
  * Created by corney on 18.09.16.
  */
object ClientConverter {

  def apply(line: String): Either[Client, ParseError] = {
    val fields = line.split("\t", -1)
    if (fields.length == 6) {
      try {
        Left(Client(
          name = fields(0),
          balance = BigInt(fields(1)),
          amounts = Map(
            A -> BigInt(fields(2)),
            B -> BigInt(fields(3)),
            C -> BigInt(fields(4)),
            D -> BigInt(fields(5))
          )
        ))
      } catch {
        case e: NumberFormatException =>
          Right(WrongNumber)
      }
    } else {
      Right(WrongNumberOfFields)
    }
  }

  def apply(client: Client): String = {
    "%s\t%d\t%d\t%d\t%d\t%d".format(
      client.name,
      client.balance,
      client.amounts.getOrElse(A, BigInt(0)),
      client.amounts.getOrElse(B, BigInt(0)),
      client.amounts.getOrElse(C, BigInt(0)),
      client.amounts.getOrElse(D, BigInt(0))
    )
  }
}
