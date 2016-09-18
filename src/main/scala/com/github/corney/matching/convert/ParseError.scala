package com.github.corney.matching.convert

/**
  * Created by corney on 18.09.16.
  */
sealed trait ParseError

case object WrongNumberOfFields extends ParseError

case object WrongNumber extends ParseError

case object WrongFieldValue extends ParseError
