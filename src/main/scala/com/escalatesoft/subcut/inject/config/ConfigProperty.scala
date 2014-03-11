package com.escalatesoft.subcut.inject.config

sealed trait ConfigProperty {
  def name: String
  def value: String
  
  def valueAs[T](implicit converter: ConfigProperty => T) : Option[T]
}

case class Defined(name: String, value: String) extends ConfigProperty {
  def valueAs[T](implicit converter: ConfigProperty => T) : Option[T] = Some( converter(this) )
}

case class Undefined(name: String) extends ConfigProperty {
  def value = throw new IllegalArgumentException(s"Undefined value for property $name")

  def valueAs[T](implicit converter: ConfigProperty => T) : Option[T] = None
}
