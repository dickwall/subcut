package com.escalatesoft.subcut.inject

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{FunSuite, SeveredStackTraces}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import io.Source

/**
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 5/1/11
 * Time: 4:46 PM
 */

@RunWith(classOf[JUnitRunner])
class ModuleCompositionAndMergingTest extends FunSuite with ShouldMatchers with SeveredStackTraces {

  test("Modules should be composable the :: operator") {
    implicit def billsBindings = MediumGardenModule andThen LawnModule andThen LarchModule
    val billsGarden = new Garden    // will pick up the configuration module implicitly
    billsGarden.describeGarden should be ("The garden is 200 sq ft, has a Larch tree 120 high, Grass ground covering that cost 50 initially and 25 maintenance")
  }

  test("Modules using the :: operator should have left to right priority") {
    implicit def sallysBindings = SmallGardenModule andThen MediumGardenModule andThen OakModule andThen LawnModule andThen LarchModule
    val sallysGarden = new Garden    // will pick up the configuration module implicitly
    sallysGarden.describeGarden should be ("The garden is 50 sq ft, has a Oak tree 150 high, Grass ground covering that cost 50 initially and 25 maintenance")
  }

  test("Should not be able to cast a binding module to Mutable") {
    intercept[ClassCastException] {
      val mutable = SmallGardenModule.asInstanceOf[MutableBindingModule]
    }
  }

  test("Merging configuration strings over existing") {
    val fromProperties = new FromProperties
    implicit val configuration = fromProperties andThen StandardConfiguration
    configuration.modifyBindings { mod =>
      mod.showDeepBindings()
    }

    println("----")

    implicit val orig = StandardConfiguration andThen fromProperties
    orig.modifyBindings { mod =>
      mod.showDeepBindings()
    }

  }
}

// let's make some modules to bind in

object StandardConfiguration extends NewBindingModule({ module =>
  import module._
  bind [String] idBy 'wsUrl toSingle "http://main.service.com/main/web/service"
  bind [String] idBy 'database toSingle "mysql:localhost:3306/main"
  bind [String] idBy 'version toSingle "1.3"
})

class FromProperties extends BindingModule {
  val bindings = {
    val newMod = new NewBindingModule({ module =>
      import module._
      bind [String] idBy 'wsUrl toSingle "http://alt.service.com/alternative/web/service"
      bind [String] idBy 'database toSingle "mysql:remote-host:3306/main"
      bind [String] idBy 'grue toSingle "You have been eaten"
    })
    newMod.bindings
  }
}

object LarchModule extends NewBindingModule({ module =>
  module.bind [Tree] toSingle { new Larch }
})

object OakModule extends NewBindingModule({ module =>
  module.bind [Tree] toSingle { new Oak }
})

object LawnModule extends NewBindingModule({ module =>
  module.bind [GroundCover] toSingle { new Lawn }
})

object PavedModule extends NewBindingModule({ module =>
  module.bind [GroundCover] toSingle { new Paved }
})

object SmallGardenModule extends NewBindingModule({ module =>
  module.bind [GardenPlot] toSingle { new SmallGardenPlot }
})

object MediumGardenModule extends NewBindingModule({ module =>
  module.bind [GardenPlot] toSingle { new MediumGardenPlot }
})

class Garden(implicit val bindingModule: BindingModule) extends Injectable {
  val plot = inject[GardenPlot]
  val tree = inject[Tree]
  val cover = inject[GroundCover]

  def describeGarden: String =
    "The garden is %d sq ft, has a %s tree %d high, %s ground covering that cost %d initially and %d maintenance".format(
      plot.area, tree.species, tree.height, cover.coverType, cover.initialCost, cover.maintenanceCost
    )
}

trait Tree {
  def species: String
  def height: Int
}

class Oak extends Tree {
  def species = "Oak"
  def height = 150
}

class Larch extends Tree {
  def species = "Larch"
  def height = 120
}

trait GroundCover {
  def coverType: String
  def initialCost: Int
  def maintenanceCost: Int
}

class Lawn extends GroundCover {
  def coverType = "Grass"
  def initialCost = 50
  def maintenanceCost = 25
}

class Paved extends GroundCover {
  def coverType = "Stone"
  def initialCost = 100
  def maintenanceCost = 0
}

trait GardenPlot {
  def x: Int
  def y: Int
  def area = x * y
}

class SmallGardenPlot extends GardenPlot {
  def x = 5
  def y = 10
}

class MediumGardenPlot extends GardenPlot {
  def x = 10
  def y = 20
}
