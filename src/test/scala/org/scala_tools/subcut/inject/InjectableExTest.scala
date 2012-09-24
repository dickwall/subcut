package org.scala_tools.subcut.inject

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class InjectableExTest extends FunSuite with ShouldMatchers {
  test("test InjectableEx") {
    val bindingModule = new NewBindingModule(module => {
      import module._
      bind[Foo] toProvider (new SomeFoo)
    })

    val bar = classOf[Bar].getConstructor(classOf[BindingModule]).newInstance(bindingModule)

    bar.myFoo.foo should be === "foo"
    bar.mySomeFoo.foo should be === "foo"
    bar.myInnerFoo.foo should be === "inner"

    val baz = new Baz()(bindingModule)

    baz.bar.myFoo.foo should be === "foo"
    baz.bar.mySomeFoo.foo should be === "foo"
    baz.bar.myInnerFoo.foo should be === "inner"

    baz.innerBar.myFoo.foo should be === "foo"
    baz.innerBar.mySomeFoo.foo should be === "foo"
    baz.innerBar.myInnerFoo.foo should be === "inner"
  }
}

trait Foo {
  def foo: String
}

class SomeFoo extends Foo {
  def foo = "foo"
}

class Bar(implicit val bindingModule: BindingModule) extends InjectableEx {
  val myFoo = inject[Foo]

  val mySomeFoo = inject[SomeFoo]

  val myInnerFoo = inject[InnerFoo]

  class InnerFoo extends Foo {
    def foo = "inner"
  }

}

class Baz(implicit val bindingModule: BindingModule) extends InjectableEx {
  val bar = inject[Bar]

  val innerBar = inject[InnerBar]

  class InnerBar(implicit val bindingModule: BindingModule) extends InjectableEx {
    val myFoo = inject[Foo]

    val mySomeFoo = inject[SomeFoo]

    val myInnerFoo = inject[InnerFoo]

    class InnerFoo extends Foo {
      def foo = "inner"
    }
  }
}