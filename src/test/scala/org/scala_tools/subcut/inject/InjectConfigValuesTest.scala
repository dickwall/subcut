package org.scala_tools.subcut.inject

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{FunSuite, SeveredStackTraces}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 5/1/11
 * Time: 10:32 AM
 */

@RunWith(classOf[JUnitRunner])
class InjectConfigValuesTest extends FunSuite with ShouldMatchers with SeveredStackTraces {
  test("inject some default configuration values using all bound") {
    implicit val bindings = ConfigValueModule
    val config1 = new ConfigValueInstance
    config1.poolSize should be (20)
    config1.minPoolSize should be (10)
    config1.threshold should be (0.2 plusOrMinus (0.0001))
    config1.theInt should be (1)
    config1.theOtherInt should be (2)
  }

  test("inject some default configuration values using some unbound") {
    ConfigValueModule.modifyBindings { configValueModule =>
      configValueModule.unbind[Int]('poolSize)
      configValueModule.bind [Int] identifiedBy 'minPoolSize toInstance (5)

      implicit val bindings = configValueModule

      val config1 = new ConfigValueInstance
      config1.poolSize should be (30)
      config1.minPoolSize should be (5)
      config1.threshold should be (0.2 plusOrMinus (0.0001))
      config1.theInt should be (1)
      config1.theOtherInt should be (2)
    }
  }
}

object ConfigValueModule extends NewBindingModule ({ module =>
  module.bind [Int] identifiedBy 'poolSize toInstance 20
  module.bind [Int] identifiedBy 'maxUsers toInstance 15
  module.bind [Double] identifiedBy 'threshold toInstance 0.2
  module.bind [Int] toInstance 1 // probably wouldn't ever do this, but we need to test it
  module.bind [Int] identifiedBy "theOther" toInstance 2
})

class ConfigValueInstance(implicit val bindingModule: BindingModule) extends Injectable {
  val poolSize = injectIfBound[Int]("poolSize") { 30 }
  val minPoolSize = injectIfBound[Int]("minPoolSize") { 10 }
  val threshold = inject[Double]('threshold)
  val theInt = inject[Int]
  val theOtherInt = inject[Int]('theOther)
}
