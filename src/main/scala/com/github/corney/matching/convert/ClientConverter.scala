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
          name = fields(CLIENT_NAME_FIELD),
          balance = BigInt(fields(BALANCE_FIELD)),
          amounts = Map(
            A -> BigInt(fields(AMOUNT_A_FIELD)),
            B -> BigInt(fields(AMOUNT_B_FIELD)),
            C -> BigInt(fields(AMOUNT_C_FIELD)),
            D -> BigInt(fields(AMOUNT_D_FIELD))
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
      client.amounts.getOrElse(A, ZERO),
      client.amounts.getOrElse(B, ZERO),
      client.amounts.getOrElse(C, ZERO),
      client.amounts.getOrElse(D, ZERO)
    )
  }
  val ZERO = BigInt(0)

  val CLIENT_NAME_FIELD = 0
  val BALANCE_FIELD = 1
  val AMOUNT_A_FIELD = 2
  val AMOUNT_B_FIELD = 3
  val AMOUNT_C_FIELD = 4
  val AMOUNT_D_FIELD = 5
}
