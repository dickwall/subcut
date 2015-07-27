package com.escalatesoft.subcut.inject

import NewBindingModule._
import org.scalatest.{FunSuite, Matchers, SeveredStackTraces}

class ImplicitBindingTest extends FunSuite with Matchers with SeveredStackTraces {
  test("Implicit binding 1") {
    implicit val bm = newBindingModule { implicit module =>
      module.bind [DoIt1] toSingle (new Impl1a)
    }
    // binding module implicit in scope
    // now create the new instance and use it
    val tryIt = new TryIt(10)
    val (str, num) = tryIt.doItUsingInjected
    str should be ("Impl1a")
    num should be (10)
  }

  test("Implicit binding 2") {
    implicit val bm = newBindingModule { module =>
      module.bind [DoIt1] toSingle (new Impl1b)
    }
    // now create the new instance and use it
    val tryIt = new TryIt(12)
    val (str, num) = tryIt.doItUsingInjected
    str should be ("Impl1b")
    num should be (144)
  }

  test("Explicit binding 1") {
    implicit val bm = newBindingModule { module =>
      module.bind [DoIt1] toSingle (new Impl1b)
    }
    val explicitModule = newBindingModule { implicit module =>
      module.bind [DoIt1] toSingle (new Impl1a)
    }
    val tryIt = new TryIt(12)(explicitModule)  // instead, this explicit should be used
    val (str, num) = tryIt.doItUsingInjected
    num should be (12) // and the explicit should propagate through, not the implicit module 2
    str should be ("Impl1a")
  }

}

trait DoIt1 {
  def doIt1(): String
  def doIt2(x: Int): Int
}

class Impl1a extends DoIt1 {
  def doIt1() = "Impl1a"
  def doIt2(x: Int) = x
}

class Impl1b extends DoIt1 {
  def doIt1() = "Impl1b"
  def doIt2(x: Int) = x * x
}

trait ImplicitBinding {
  implicit val bindingModule: MutableBindingModule
}

class TryIt(val theNum: Int)(implicit val bindingModule: BindingModule) extends Injectable {
  val doIt = inject[DoIt1]
  val usedInTryIt = new UsedInTryIt // should get the implicit binding propagated

  def doItUsingInjected(): (String, Int) = {
    (doIt.doIt1, usedInTryIt.doTheNumCall(theNum))
  }
}

class UsedInTryIt(implicit val bindingModule: BindingModule) extends Injectable {
  val doItInner = inject[DoIt1]

  def doTheNumCall(n: Int): Int = {
    doItInner.doIt2(n)
  }
}
