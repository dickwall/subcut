package com.escalatesoft.subcut.inject

import org.scalatest.Matchers
import org.scalatest.{FunSuite, SeveredStackTraces}

/*
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 3/27/11
 * Time: 4:01 PM
 */

class SomeTestClass extends Injectable with PushBindingsTestInjections {
  def tryThemAll(s: String, i: Int, d: Double): (String, Int, Double, Int) = {
    val b1 = inject[BoundTrait1]
    val b2 = inject[BoundTrait2]

    (b1.t1Method1(s), b1.t1Method2(i), b2.t2Method1(d), b2.t2Method2(s))
  }
}


class PushBindingsTest extends FunSuite with Matchers with SeveredStackTraces {

  test("Just using the standard bindings") {
    val tc = new SomeTestClass

    tc.tryThemAll("1234", 12, 5.0) should be ("4321", 24, 10.0, 4)
  }

  test("Bind in the second implementation of trait 1") {
    // check the standard bindings first
    val tc = new SomeTestClass
    tc.tryThemAll("1234", 12, 5.0) should be ("4321", 24, 10.0, 4)

    // now push some new bindings in
    PushBindingsTestModule.pushBindings {
      PushBindingsTestModule.bind [BoundTrait1] to newInstanceOf [T1Impl2]

      val tc2 = new SomeTestClass
      // new instance should have the new bindings for T1 but not for T2
      tc2.tryThemAll("1234", 12, 5.0) should be ("Hello 1234", 144, 10.0, 4)
    }

    // and outside of the push bindings, we should be back to normal
    val tc3 = new SomeTestClass
    tc3.tryThemAll("1234", 12, 5.0) should be ("4321", 24, 10.0, 4)
  }

  test("Bind in the second implementation of trait 2") {
    // check the standard bindings first
    val tc = new SomeTestClass
    tc.tryThemAll("1234", 12, 5.0) should be ("4321", 24, 10.0, 4)

    // now push some new bindings in
    PushBindingsTestModule.pushBindings {
      PushBindingsTestModule.bind[BoundTrait2] to newInstanceOf [T2Impl2]

      val tc2 = new SomeTestClass
      // new instance should have the new bindings for T2 but not for T1
      tc2.tryThemAll("1234", 12, 5.0) should be ("4321", 24, 25.0, 1234)

      // now push in another implementation of T2, and T1 as well
      PushBindingsTestModule.pushBindings {
        PushBindingsTestModule.bind [BoundTrait1] to newInstanceOf [T1Impl3]
        PushBindingsTestModule.bind [BoundTrait2] to newInstanceOf [T2Impl3]

        val tc3 = new SomeTestClass
        // new instance should have impl3 bindings for both traits
        tc3.tryThemAll("1234", 12, 5.0) should be ("G'day 1234", 36, 20.0, 8)
      }

      // should have popped the bindings back to the first push - impl 2 of t2, impl 1 of t1
      val tc4 = new SomeTestClass
      // new instance should have the new bindings for T2 but not for T1
      tc4.tryThemAll("1234", 12, 5.0) should be ("4321", 24, 25.0, 1234)

    }

    // and outside of the push bindings, we should be back to normal
    val tc5 = new SomeTestClass
    tc5.tryThemAll("1234", 12, 5.0) should be ("4321", 24, 10.0, 4)
  }

}

object PushBindingsTestModule1 extends MutableBindingModule {
  bind [BoundTrait1] toSingle new T1Impl1
}

object PushBindingsTestModule2 extends MutableBindingModule {
  bind [BoundTrait2] toSingle new T2Impl1
}

object PushBindingsTestModule extends MutableBindingModule {
  withBindingModules(PushBindingsTestModule1, PushBindingsTestModule2)
}

trait PushBindingsTestInjections extends BoundToModule {
  val bindingModule = PushBindingsTestModule
}

trait BoundTrait1 {
  def t1Method1(x: String): String
  def t1Method2(i: Int): Int
}

trait BoundTrait2 {
  def t2Method1(d: Double): Double
  def t2Method2(s: String): Int
}

class T1Impl1 extends BoundTrait1 {
  def t1Method1(x: String) = x.reverse
  def t1Method2(i: Int) = i + i
}

class T1Impl2 extends BoundTrait1 {
  def t1Method1(x: String) = "Hello " + x
  def t1Method2(i: Int) = i * i
}

class T1Impl3 extends BoundTrait1 {
  def t1Method1(x: String) = "G'day " + x
  def t1Method2(i: Int) = i * 3
}

class T2Impl1 extends BoundTrait2 {
  def t2Method1(d: Double) = d * 2.0
  def t2Method2(s: String) = s.length
}

class T2Impl2 extends BoundTrait2 {
  def t2Method1(d: Double) = d * d
  def t2Method2(s: String) = s.toInt
}

class T2Impl3 extends BoundTrait2 {
  def t2Method1(d: Double) = d * 4.0
  def t2Method2(s: String) = s.length * 2
}
