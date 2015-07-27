package com.escalatesoft.subcut.inject

import org.scalatest.{FunSuite, Matchers, SeveredStackTraces}

class InjectionTest extends FunSuite with Matchers with SeveredStackTraces {

  test("Create an object with injected resources") {
    class SomeInjectedStuff extends Injectable with BoundToTrial {
      val impl1 = inject[TestTrait]
      val impl2 = inject[TestTrait]("something else")
      val impl3 = inject[TestTrait]('testConfig)

      def stringThemAll(): List[String] =
        List(impl1.someMethod(), impl2.someMethod(), impl3.someMethod())
    }

    val injected = new SomeInjectedStuff
    injected.stringThemAll should be (List("Hello World", "This is the testConfig", "This is the testConfig"))
  }

  test("Create an abstract class and then instantiate it with injected resources") {
    abstract class SomeAbstractInjectedStuff extends Injectable {
      lazy val impl1 = inject[TestTrait]
      lazy val impl2 = inject[TestTrait]("something else")
      lazy val impl3 = inject[TestTrait]('testConfig)

      def stringThemAll(): List[String] =
        List(impl1.someMethod(), impl2.someMethod(), impl3.someMethod())
    }

    val injected = new SomeAbstractInjectedStuff with BoundToTrial
    injected.stringThemAll should be (List("Hello World", "This is the testConfig", "This is the testConfig"))

    val injected2 = new SomeAbstractInjectedStuff { val bindingModule = TrialBindingModule }
    injected2.stringThemAll should be (List("Hello World", "This is the testConfig", "This is the testConfig"))
  }

  test("Use a lazy instance and make sure it works, and the same instance is always injected") {
    class SomeInjectedWithFixed(implicit val bindingModule: BindingModule) extends Injectable {
      lazy val impl1 = inject[TestTrait]('testConfig)
      lazy val impl2 = inject[TestTrait]('fixed)
    }

    implicit val bindings = TrialBindingModule
    val injected1 = new SomeInjectedWithFixed
    val injected2 = new SomeInjectedWithFixed

    println("Before first usage of the lazy instance")
    // the impl2 in each case should return "This is the testConfig fixed"
    injected1.impl2.someMethod should be ("This is the testConfig fixed")
    injected2.impl2.someMethod should be ("This is the testConfig fixed")

    // now, check the identities of the impl1s and impl2s. The impl1s should be non-identical, while the impl2s
    // should be identical
    (injected1.impl1 ne injected2.impl1) should be (true)
    (injected1.impl2 eq injected2.impl2) should be (true)
  }
}

object TrialBindingModule extends MutableBindingModule {
  bind [TestTrait] toProvider { new TestImpl }
  bind [TestTrait] identifiedBy 'testConfig toProvider (new AlternativeImpl)
  bind [TestTrait] identifiedBy "something else" toProvider { new AlternativeImpl }
  bind [TestTrait] identifiedBy 'fixed toSingle new AlternativeImpl2

  bind [AnotherTrait] toSingle new AnotherTraitImpl
  this.showMap()
}

trait BoundToTrial extends BoundToModule {
  val bindingModule = TrialBindingModule
}

trait TestTrait {
    def someMethod() : String
}

trait AnotherTrait {
    def someOtherMethod() : Int
}

class TestImpl extends TestTrait {
    override def someMethod() : String = "Hello World"
}

class AlternativeImpl extends TestTrait {
    override def someMethod() : String = "This is the testConfig"
}

class AlternativeImpl2 extends TestTrait {
    println("Creating AlternativeImpl2 instance")
    override def someMethod() : String = "This is the testConfig fixed"
}

class AnotherTraitImpl extends AnotherTrait {
    override def someOtherMethod() = 5
}
