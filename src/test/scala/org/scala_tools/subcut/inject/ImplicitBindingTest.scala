package org.scala_tools.subcut.inject

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{SeveredStackTraces, FunSuite}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 4/24/11
 * Time: 6:28 PM
 */

@RunWith(classOf[JUnitRunner])
class ImplicitBindingTest extends FunSuite with ShouldMatchers with SeveredStackTraces {
  test("Implicit binding 1") {
    implicit val bm: BindingModule = ImplicitModule1 // binding module implicit in scope
    // now create the new instance and use it
    val tryIt = new TryIt(10)
    val (str, num) = tryIt.doItUsingInjected
    str should be ("Impl1a")
    num should be (10)
  }

  test("Implicit binding 2") {
    implicit val bm: BindingModule = ImplicitModule2 // binding module implicit in scope
    // now create the new instance and use it
    val tryIt = new TryIt(12)
    val (str, num) = tryIt.doItUsingInjected
    str should be ("Impl1b")
    num should be (144)
  }

  test("Explicit binding 1") {
    implicit val bm: BindingModule = ImplicitModule2 // binding module implicit should not be used
    val tryIt = new TryIt(12)(ImplicitModule1)  // instead, this explicit should be used
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

object ImplicitModule1 extends MutableBindingModule {
  bind [DoIt1] toInstance (new Impl1a)
}

object ImplicitModule2 extends MutableBindingModule {
  bind [DoIt1] toInstance (new Impl1b)
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
