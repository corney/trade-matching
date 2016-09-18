package com.github.corney.matching.domain

/**
  * Так как количество ценных бумаг в данный момент фикисровано, ограничим его здесь.
  * В реальной системе, разумеется, должна быть возможность добавления ценных бумаг без модификации кода.
  */
object Finance extends Enumeration {
  type Finance = Value
  val A, B, C, D = Value
}








