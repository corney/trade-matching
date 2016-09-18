package com.github.corney.matching.trade

import com.github.corney.matching.domain.{BidType, Client, Order}
import com.github.corney.matching.domain.Finance._
import com.github.corney.matching.convert._
import org.specs2.mutable.Specification


/**
  * Created by corney on 18.09.16.
  */
class ConverterSpec extends Specification {
  "ClientConverter" should {
    "Parse strings" in {
      ClientConverter("green\t2000\t100\t200\t300\t400") must be_==(
        Left(Client(
          name = "green",
          balance = 2000,
          amounts = Map(A -> 100, B -> 200, C -> 300, D -> 400)
        ))
      )
    }

    "Decline strings" in {
      ClientConverter("green\t2000\t100\t200\t300\t400\t") must be_==(Right(WrongNumberOfFields))
      ClientConverter("green\t2000\t100\t200\t300") must be_==(Right(WrongNumberOfFields))
      ClientConverter("green\t2000a\t100\t200\t300\t400") must be_==(Right(WrongNumber))
      ClientConverter("green\t2000\t100\td200\t300\t400") must be_==(Right(WrongNumber))
      ClientConverter("green\t2000\t100\t200\t300\t400-10") must be_==(Right(WrongNumber))
    }

    "Produce strings" in {
      ClientConverter(Client(
        name = "green",
        balance = 2000,
        amounts = Map(A -> 100, B -> 200, C -> 300, D -> 400)
      )) must be_==("green\t2000\t100\t200\t300\t400")
    }
  }

  "OrderConverter" should {
    "Parse strings" in {
      OrderConverter("brown\tb\tD\t200\t300") must be_==(
        Left(Order(
          clientName = "brown",
          bidType = BidType.Buy,
          finance = D,
          amount = 200,
          price = 300
        ))
      )

      OrderConverter("pink\ts\tB\t2000\t30") must be_==(
        Left(Order(
          clientName = "pink",
          bidType = BidType.Sell,
          finance = B,
          amount = 2000,
          price = 30
        ))
      )
    }

    "Decline strings" in {
      OrderConverter("pink\ts\tB\t2000\t30\t") must be_==(Right(WrongNumberOfFields))
      OrderConverter("pink\ts\tB\t2000") must be_==(Right(WrongNumberOfFields))
      OrderConverter("pink\tc\tB\t2000\t30") must be_==(Right(WrongFieldValue))
      OrderConverter("pink\ts\tE\t2000\t30") must be_==(Right(WrongFieldValue))
      OrderConverter("pink\ts\tB\t2000a\t30") must be_==(Right(WrongNumber))
    }
  }

}
