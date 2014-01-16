package com.escalatesoft.subcut.inject.config

trait ConfigPropertySource {
  def get(propertyName: String) : ConfigProperty = getOptional(propertyName) match {
    case property@Defined(_, value) => property
    case Undefined(_) => throw new IllegalArgumentException(s"Missing property $propertyName in config")
  }

  def getOptional(propertyName: String) : ConfigProperty
}
