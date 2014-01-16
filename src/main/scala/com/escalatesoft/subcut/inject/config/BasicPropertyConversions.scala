package com.escalatesoft.subcut.inject.config

object BasicPropertyConversions  {

  implicit def _toString(input: ConfigProperty) : String = input.value
  implicit def _toInt(input: ConfigProperty) : Int = augmentString(input.value).toInt
  implicit def _toLong(input: ConfigProperty) : Long = augmentString(input.value).toLong
  implicit def _toDouble(input: ConfigProperty) : Double = augmentString(input.value).toDouble
  implicit def _toFloat(input: ConfigProperty) : Float = augmentString(input.value).toFloat
  implicit def _toByte(input: ConfigProperty) : Byte = augmentString(input.value).toByte
  implicit def _toShort(input: ConfigProperty) : Short = augmentString(input.value).toShort
}

import BasicPropertyConversions._
trait BasicPropertyConversions {

  implicit def toString(input: ConfigProperty) : String = _toString(input)
  implicit def toInt(input: ConfigProperty) : Int = _toInt(input)
  implicit def toLong(input: ConfigProperty) : Long = _toLong(input)
  implicit def toDouble(input: ConfigProperty) : Double = _toDouble(input)
  implicit def toFloat(input: ConfigProperty) : Float = _toFloat(input)
  implicit def toByte(input: ConfigProperty) : Byte = _toByte(input)
  implicit def toShort(input: ConfigProperty) : Short = _toShort(input)

}


