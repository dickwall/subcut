package org.scala_tools.subcut.inject

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{FunSuite, SeveredStackTraces}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/*
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 3/27/11
 * Time: 2:50 PM
 */

@RunWith(classOf[JUnitRunner])
class FrozenModuleTest extends FunSuite with ShouldMatchers with SeveredStackTraces {
  test("No binding in the module") {
    intercept[BindingException] {
      val mathFuncInjected = new MathFuncInjected
    }
  }

  test("Bind addition into the module") {
    MathModule.bind[MathFunc].toClass[AddFunc]

    // we should now have a working addition injected
    val mathFuncInjected = new MathFuncInjected
    mathFuncInjected.doIt(6, 3) should be (9)
  }

  test("Re-bind - before being fixed") {
    MathModule.bind[MathFunc].toClass[AddFunc]

    // we should now have a working addition injected
    val mathFuncInjected = new MathFuncInjected
    mathFuncInjected.doIt(6, 3) should be (9)

    // rebind to multiplication
    MathModule.bind[MathFunc].toClass[MultFunc]

    // original instance should be unaffected
    mathFuncInjected.doIt(6, 3) should be (9)

    // but a new one should multiply rather than add
    val mathFuncInjected2 = new MathFuncInjected
    mathFuncInjected2.doIt(6, 3) should be (18)
  }

  test("Re-bind after module is frozen") {
    MathModule.bind[MathFunc].toClass[AddFunc]

    // we should now have a working addition injected
    val mathFuncInjected = new MathFuncInjected
    mathFuncInjected.doIt(6, 3) should be (9)

    MathModule.freeze() // should not be able to change it again

    // rebind to multiplication
    intercept[BindingException] {
      MathModule.bind[MathFunc].toClass[MultFunc]
    }

    // original instance should be unaffected
    mathFuncInjected.doIt(6, 3) should be (9)

    // but a new one should still add
    val mathFuncInjected2 = new MathFuncInjected
    mathFuncInjected2.doIt(6, 3) should be (9)
  }

}

trait MathFunc {
  def doIt(x: Int, y: Int): Int
}

class AddFunc extends MathFunc {
  override def doIt(x: Int, y: Int): Int = x + y
}

class MultFunc extends MathFunc {
  override def doIt(x: Int, y: Int): Int = x * y
}

object MathModule extends MutableBindingModule

trait MathInjector extends BoundToModule {
  override val bindingModule = MathModule
}

class MathFuncInjected extends Injectable with MathInjector {
  val mathFunc = inject[MathFunc]

  def doIt(x: Int, y: Int) = mathFunc.doIt(x, y)
}
