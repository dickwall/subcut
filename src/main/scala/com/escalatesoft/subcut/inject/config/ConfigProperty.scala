package com.escalatesoft.subcut.inject.config

sealed trait ConfigProperty {
  def name: String
  def value: String
}

case class Defined(name: String, value: String) extends ConfigProperty

case class Undefined(name: String) extends ConfigProperty {
  def value = throw new IllegalArgumentException(s"Undefined value for property $name")
}
