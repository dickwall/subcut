package com.escalatesoft.subcut.inject

import org.scalatest.Matchers
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
class InjectConfigValuesTest extends FunSuite with Matchers with SeveredStackTraces {
  test("inject some default configuration values using all bound") {
    implicit val bindings = ConfigValueModule
    val config1 = new ConfigValueInstance
    config1.poolSize should be (20)
    config1.minPoolSize should be (10)
    config1.threshold should be (0.2 +- (0.0001))
    config1.theInt should be (1)
    config1.theOtherInt should be (2)
  }

  test("inject some default configuration values using some unbound") {
    ConfigValueModule.modifyBindings { implicit configValueModule =>
      configValueModule.unbind[Int]('poolSize)
      configValueModule.bind [Int] identifiedBy 'minPoolSize toSingle (5)

      val config1 = new ConfigValueInstance
      config1.poolSize should be (30)
      config1.minPoolSize should be (5)
      config1.threshold should be (0.2 +- (0.0001))
      config1.theInt should be (1)
      config1.theOtherInt should be (2)
    }
  }
}

object ConfigValueModule extends NewBindingModule ({implicit module =>
  import module._
  bind [Int] idBy 'poolSize toSingle 20
  bind [Int] idBy 'maxUsers toSingle 15
  bind [Double] idBy 'threshold toSingle 0.2
  bind [Int] toSingle 1 // probably wouldn't ever do this, but we need to test it
  bind [Int] idBy "theOther" toSingle 2
})

class ConfigValueInstance(implicit val bindingModule: BindingModule) extends Injectable {
  val poolSize = injectOptional [Int] ('poolSize) getOrElse 30
  val minPoolSize = injectOptional [Int] ("minPoolSize") getOrElse 10
  val threshold = inject[Double]('threshold)
  val theInt = inject[Int]
  val theOtherInt = inject[Int]('theOther)
}
