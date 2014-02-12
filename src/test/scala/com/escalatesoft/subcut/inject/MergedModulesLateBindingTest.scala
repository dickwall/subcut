package com.escalatesoft.subcut.inject

import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Created with IntelliJ IDEA.
 * User: rlwall2
 * Date: 6/11/12
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */

@RunWith(classOf[JUnitRunner])
class MergedModulesLateBindingTest extends FunSuite with Matchers {

  test ("provider manufacturing configuration") {
    implicit val bm = RobotProviderModule
    val robotHome = new RobotHome
    robotHome.robot.doStuff should be ("I am a ManufacturingRobot and I perform action Make things")
  }

  test ("reflection manufacturing configuration") {
    implicit val bm = RobotReflectionModule
    val robotHome = new RobotHome
    robotHome.robot.doStuff should be ("I am a ManufacturingRobot and I perform action Make things")
  }

  test ("provider manufacturing configuration with adding roving feature") {
    import NewBindingModule._
    implicit val bm = newBindingModule { module =>
      module <~ RobotProviderModule
      module <~ RovingActionModule
    }
    val robotHome = new RobotHome
    robotHome.robot.doStuff should be ("I am a ManufacturingRobot and I perform action Move around")
  }

  test ("reflection manufacturing configuration with added roving feature") {
    implicit val bm = RovingActionModule ~ RobotReflectionModule
    val robotHome = new RobotHome
    robotHome.robot.doStuff should be ("I am a ManufacturingRobot and I perform action Move around")
  }

  test ("provider roving configuration") {
    implicit val bm = RovingNameModule ~ RovingActionModule ~ RobotProviderModule
    val robotHome = new RobotHome
    robotHome.robot.doStuff should be ("I am a RovingRobot and I perform action Move around")
  }

  test ("reflection roving configuration") {
    implicit val bm = RovingNameModule ~ RovingActionModule ~ RobotReflectionModule
    val robotHome = new RobotHome
    robotHome.robot.doStuff should be ("I am a RovingRobot and I perform action Move around")
  }
}

object RobotProviderModule extends NewBindingModule(module => {
  import module._
  bind [Robot] toProvider { implicit module => new AdaptableRobot }
  bind [RobotAction] toSingle new ManufacturingAction
})

object RobotReflectionModule extends NewBindingModule(module => {
  import module._
  bind [Robot] to moduleInstanceOf [AdaptableRobot]
  bind [RobotAction] toSingle new ManufacturingAction
})

object RovingActionModule extends NewBindingModule(module => {
  import module._
  bind [RobotAction] toSingle new RovingAction
})

object RovingNameModule extends NewBindingModule(module => {
  import module._
  bind [String] idBy 'robotName toSingle "RovingRobot"
})

trait Robot extends Injectable {
  val action = inject[RobotAction]
  def doStuff: String
}

trait RobotAction {
  def action: String
}

class RobotHome(implicit val bindingModule: BindingModule) extends Injectable {
  val robot = inject[Robot]
}

class AdaptableRobot(implicit val bindingModule: BindingModule) extends Robot with Injectable {
  val name = injectOptional[String]('robotName) getOrElse "ManufacturingRobot"
  def doStuff: String = "I am a %s and I perform action %s".format(name, action.action)
}

class ManufacturingAction extends RobotAction {
  def action: String = "Make things"
}

class RovingAction extends RobotAction {
  def action: String = "Move around"
}