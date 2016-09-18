package com.github.corney.matching.domain

import com.github.corney.matching.domain.Finance.Finance

/**
  * Created by corney on 17.09.16.
  */
case class Client(name: String, balance: BigInt, amounts: Map[Finance, BigInt])
