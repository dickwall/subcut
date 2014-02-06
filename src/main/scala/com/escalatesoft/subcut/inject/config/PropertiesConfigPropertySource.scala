package com.escalatesoft.subcut.inject.config

import java.util.Properties
import java.io.{FileInputStream, File}

class PropertiesConfigPropertySource(properties: Properties) extends ConfigPropertySource {
  def getOptional(propertyName: String) : ConfigProperty = {
    val prop = properties.getProperty(propertyName)

    if(prop != null)
      Defined(propertyName, prop)
    else
      Undefined(propertyName)
  }
}

class PropertiesConfigMapSource(properties: Map[String, String]) extends ConfigPropertySource {
  def getOptional(propertyName: String) : ConfigProperty = properties.get(propertyName) match {
      case Some(prop) =>  Defined(propertyName, prop)
      case None =>  Undefined(propertyName)
    }
}

object PropertiesConfigPropertySource {
  def apply(properties: Properties) = new PropertiesConfigPropertySource(properties)
  def apply(properties: Map[String, String]) = new PropertiesConfigMapSource(properties)

  def fromPath(path: String) = {
    val properties = new Properties()
    properties.load(new FileInputStream(new File(path)))
    new PropertiesConfigPropertySource(properties)
  }

  def fromClasspathResource(path: String) = {
    val properties = new Properties()
    properties.load(getClass.getClassLoader.getResourceAsStream(path))
    new PropertiesConfigPropertySource(properties)
  }
}
