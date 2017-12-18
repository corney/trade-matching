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
        fields(BID_TYPE_FIELD) match {
          case "b" =>
            Left(Order(
              clientName = fields(CLIENT_NAME_FIELD),
              bidType = BidType.Buy,
              finance = Finance.withName(fields(FINANCE_FIELD)),
              amount = BigInt(fields(AMOUNT_FIELD)),
              price = BigInt(fields(PRICE_FIELD))
            ))
          case "s" =>
            Left(Order(
              clientName = fields(CLIENT_NAME_FIELD),
              bidType = BidType.Sell,
              finance = Finance.withName(fields(FINANCE_FIELD)),
              amount = BigInt(fields(AMOUNT_FIELD)),
              price = BigInt(fields(PRICE_FIELD))
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
  val CLIENT_NAME_FIELD = 0
  val BID_TYPE_FIELD = 1
  val FINANCE_FIELD = 2
  val AMOUNT_FIELD = 3
  val PRICE_FIELD = 4
}
