package com.github.corney.matching.trade

import com.github.corney.matching.domain.{Client, Order}

object RequestMessages {

  case class SendClient(client: Client)

  case class SendOrder(order: Order)

  case object AllClientsTransferred

  case object CheckBalance

  case object Stop

}

object ResponseMessages {

  sealed trait Response

  sealed trait ResponseFailure extends Response

  sealed trait OK extends Response

  case object Enqueued extends Response

  case object Processed extends Response

  case class Balance(clients: Seq[Client]) extends Response

  case object UnexpectedCommand extends ResponseFailure

  case object ClientNotFound extends ResponseFailure

  case object ClientNotMatched extends ResponseFailure

  case object LackOfResources extends ResponseFailure
}

