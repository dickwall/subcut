package com.escalatesoft.subcut.inject.config

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import com.escalatesoft.subcut.inject.{Injectable, BindingModule}
import com.escalatesoft.subcut.inject.NewBindingModule._
import java.util.Properties
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PropertiesConfigPropertySourceTest extends FunSuite with ShouldMatchers {

  class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
    val property1 = injectProperty[String]("property1")
    val property2 = injectProperty[Int]("property2")
    val property3 = injectProperty[Long]("property3")
    val property4 = injectProperty[Float]("property4")

    def properties: List[Any] = List(property1, property2, property3, property4)
  }

  test("should load from properties") {
    val properties = new Properties()
    properties.put("property1", "value1")
    properties.put("property2", "2")
    properties.put("property3", "3")
    properties.put("property4", "4")
    implicit val propertySource = PropertiesConfigPropertySource(properties)

    implicit val bindingModule = newBindingModuleWithConfig

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List("value1", 2, 3l, 4.0))
  }

  test("should load from classpath") {
    implicit val propertySource = PropertiesConfigPropertySource.fromClasspathResource("test.properties")

    implicit val bindingModule = newBindingModuleWithConfig

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List("value1", 2, 3l, 4.0))
  }

  test("should load from path") {
    implicit val propertySource = PropertiesConfigPropertySource.fromPath("src/test/resources/test.properties")

    implicit val bindingModule = newBindingModuleWithConfig

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List("value1", 2, 3l, 4.0))
  }

  test("should work without implicit too") {

    implicit val bindingModule = newBindingModuleWithConfig(PropertiesConfigPropertySource.fromPath("src/test/resources/test.properties"))

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List("value1", 2, 3l, 4.0))
  }
}
