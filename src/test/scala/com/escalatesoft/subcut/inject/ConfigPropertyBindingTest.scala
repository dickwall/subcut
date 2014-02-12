package com.escalatesoft.subcut.inject

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import NewBindingModule._
import com.escalatesoft.subcut.inject.config._
import com.escalatesoft.subcut.inject.config.Defined
import scala.Some
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.Date
import java.text.SimpleDateFormat
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ConfigPropertyBindingTest extends FunSuite with ShouldMatchers {


  test("Should inject from config") {
    class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
      val property1 = injectProperty[String]("property1")
      val property2 = injectProperty[String]("property2")

      def properties: List[Any] = List(property1, property2)
    }

    implicit val propertyProvider = PropertiesConfigPropertySource {
      Map( "property1" -> "value1", "property2" -> "value2")
    }
    implicit val bindingModule = newBindingModuleWithConfig

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List("value1", "value2"))
  }

  test("Should convert basic types") {
    class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
      val property1 = injectProperty[String]("property1")
      val property2 = injectProperty[Int]("property2")
      val property3 = injectProperty[Long]("property3")
      val property4 = injectProperty[Float]("property4")

      def properties: List[Any] = List(property1, property2, property3, property4)
    }

    implicit val propertyProvider = PropertiesConfigPropertySource {
      Map( "property1" -> "value1", "property2" -> "2", "property3" -> "3", "property4" -> "4.0")
    }
    implicit val bindingModule = newBindingModuleWithConfig { bindingModule => }

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List("value1", 2, 3l, 4.0))
  }

  test("Should bind properties from config source") {
    class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
      val property1 = inject[String]("property1")
      val property2 = inject[Int]("property2")
      val property3 = inject[Long]("property3")
      val property4 = inject[Float]("property4")

      def properties: List[Any] = List(property1, property2, property3, property4)
    }

    implicit val propertyProvider = PropertiesConfigPropertySource {
      Map( "property1" -> "value1", "property2" -> "2", "property3" -> "3", "property4" -> "4.0")
    }

    implicit val bindingModule = newBindingModuleWithConfig { bindingModule =>
      import bindingModule._
      import BasicPropertyConversions._

      bind [String] idBy 'property1 toProperty "property1"
      bind [Int] idBy 'property2 toProperty "property2"
      bind [Long] idBy 'property3 toProperty "property3"
      bind [Float] idBy 'property4 toProperty "property4"
    }

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List("value1", 2, 3l, 4.0))
  }

  test("Should optinally bind properties from config source") {
    class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
      val property1 = injectOptionalProperty[Int]("property1") getOrElse(-1)
      val property2 = injectOptionalProperty[Int]("property2") getOrElse(-1)

      def properties: List[Any] = List(property1, property2)
    }

    implicit val propertyProvider = PropertiesConfigPropertySource {
      Map( "property1" -> "100")
    }

    implicit val bindingModule = newBindingModuleWithConfig

    val configReaderInstance = new ToInject

    configReaderInstance.properties should equal (List(100, -1))
  }

  test("Should bind custom type if a converter is provided") {
    implicit def toDate(prop: ConfigProperty): Date = new SimpleDateFormat("yyyy-mm-DD").parse(prop.value)

    class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
      val property1 = inject[Date]("property1")
      val property2 = injectProperty[Date]("property2")
    }

    implicit val propertyProvider = PropertiesConfigPropertySource {
      Map( "property1" -> "2014-01-15", "property2" -> "2014-01-15")
    }

    implicit val bindingModule = newBindingModuleWithConfig { bindingModule =>
      import bindingModule._

      bind [Date] idBy 'property1 toProperty "property1"
    }

    val configReaderInstance = new ToInject

    configReaderInstance.property1 should equal (new SimpleDateFormat("yyyy-mm-DD").parse("2014-01-15"))
    configReaderInstance.property2 should equal (new SimpleDateFormat("yyyy-mm-DD").parse("2014-01-15"))
  }

  test("Operators on binding modules are bringing around the config properties provider - modifyBindings") {
    class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
      val property1 = inject[String]("property1")
      val property2 = inject[Int]("property2")
      val property3 = inject[Long]("property3")
      val property4 = inject[Float]("property4")

      def properties: List[Any] = List(property1, property2, property3, property4)
    }

    implicit val propertyProvider = PropertiesConfigPropertySource {
      Map( "property1" -> "value1", "property2" -> "2", "property3" -> "3", "property4" -> "4.0")
    }

    implicit val bindingModule = newBindingModuleWithConfig { bindingModule =>
      import bindingModule._
      import BasicPropertyConversions._

      bind [String] idBy 'property1 toProperty "property1"
      bind [Int] idBy 'property2 toProperty "property2"
      bind [Long] idBy 'property3 toProperty "property3"
      bind [Float] idBy 'property4 toProperty "property4"
    }

    bindingModule modifyBindings { implicit bindingModule =>
      import bindingModule._

      bind [String] idBy 'property1 toSingle "value1-MODIFIED"

      val configReaderInstance = new ToInject

      configReaderInstance.properties should equal (List("value1-MODIFIED", 2, 3l, 4.0))
    }
  }

  test("You can write more esoteric conversion using property name") {
    class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
      val timeout1 = injectProperty[Duration]("timeout.millis")
      val timeout2 = inject[Duration]("timeout2")
    }

    implicit def toDuration(prop: ConfigProperty): Duration = {
      val amount: Long = prop.value.toLong
      val timeQualifier: String = prop.name.split('.').last

      timeQualifier match {
        case "seconds" | "secs" => amount.seconds
        case "hours"  => amount.hours
        case _ => amount.milliseconds
      }
    }

    implicit val propertyProvider = PropertiesConfigPropertySource {
      Map( "timeout.millis" -> "100", "timeout.seconds" -> "20")
    }

    implicit val bindingModule = newBindingModuleWithConfig { bindingModule =>
      import bindingModule._

      bind [Duration] idBy "timeout2" toProperty "timeout.seconds"
    }

    val configReaderInstance = new ToInject

    configReaderInstance.timeout1 should equal (100.milliseconds)
    configReaderInstance.timeout2 should equal (20.seconds)
  }
}
