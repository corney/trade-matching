package com.github.corney.matching.convert

import java.util.NoSuchElementException

import com.github.corney.matching.domain.{BidType, Client, Finance, Order}
import com.github.corney.matching.domain.Finance._

/**
  * Created by corney on 18.09.16.
  */
object OrderConverter {

  def apply(line: String): Either[Order, ParseError] = {
    val fields = line.split("\t", -1)
    if (fields.length == 5) {
      try {
        fields(1) match {
          case "b" =>
            Left(Order(
              clientName = fields(0),
              bidType = BidType.Buy,
              finance = Finance.withName(fields(2)),
              amount = BigInt(fields(3)),
              price = BigInt(fields(4))
            ))
          case "s" =>
            Left(Order(
              clientName = fields(0),
              bidType = BidType.Sell,
              finance = Finance.withName(fields(2)),
              amount = BigInt(fields(3)),
              price = BigInt(fields(4))
            ))
          case _ =>
            Right(WrongFieldValue)
        }

      } catch {
        case e: NumberFormatException =>
          Right(WrongNumber)
        case e: NoSuchElementException =>
          Right(WrongFieldValue)
      }
    } else {
      Right(WrongNumberOfFields)
    }
  }
}
