package com.github.corney.matching.domain

import com.github.corney.matching.domain.BidType.BidType
import com.github.corney.matching.domain.Finance.Finance

object BidType extends Enumeration {
  type BidType = Value
  val Buy, Sell = Value
}

/**
  * Created by corney on 18.09.16.
  */
case class Order(clientName: String, bidType: BidType, finance: Finance, amount: BigInt, price: BigInt)
