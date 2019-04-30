package com.escalatesoft.subcut.inject

import NewBindingModule.newBindingModule
import org.scalatest.{FunSuite, Matchers}

/**
 * Test the interplay between BindingId objects, symbols and Strings to make sure they all interoperate as expected
 */
class BindingIdObjectsTest extends FunSuite with Matchers {

  test ("Interop of String bindings should be compatible") {
    implicit val bm = newBindingModule { module =>
      import module._
      bind [String] idBy "Mammal" toSingle "Vole"
      bind [String] idBy "Role" toSingle "Ride"
      bind [String] idBy "Vehicle" toSingle "Motorbike"
    }

    val vrm = new Speedway
    vrm.toString should be ("I am a Vole that likes to Ride using a Motorbike")
  }

  test ("Interop of Symbol bindings should be compatible") {
    implicit val bm = newBindingModule { module =>
      import module._
      bind [String] idBy 'Mammal toSingle "Mole"
      bind [String] idBy 'Role toSingle "Tunnel"
      bind [String] idBy 'Vehicle toSingle "Drill"
    }

    val mtd = new Speedway
    mtd.toString should be ("I am a Mole that likes to Tunnel using a Drill")
  }

  test ("Interop of BindingId bindings should be compatible") {
    implicit val bm = newBindingModule { module =>
      import module._
      bind [String] idBy Mammal toSingle "Cheetah"
      bind [String] idBy Role toSingle "Run"
//      bind [String] idBy Vehicle toSingle "Gallop"
      bind [String] idBy PoolSize toSingle "Gallop"
    }

    val mtd = new Speedway
    mtd.toString should be ("I am a Cheetah that likes to Run using a Gallop")
  }

}

object Vehicle extends BindingId
object Role extends BindingId
object Mammal extends BindingId
object PoolSize extends TypedBindingId[Int]

class Speedway(implicit val bindingModule: BindingModule) extends Injectable {
  val mammal = inject[String](Mammal)
  val role = inject[String]('Role)
  val vehicle = inject[String]("Vehicle")

  override def toString = "I am a " + mammal + " that likes to " + role + " using a " + vehicle
}
