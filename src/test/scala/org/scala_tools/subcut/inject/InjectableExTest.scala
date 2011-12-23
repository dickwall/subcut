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
      bind [Foo] toProvider(new SomeFoo)
    })

    val x = classOf[Bar].getConstructor(classOf[BindingModule]).newInstance(bindingModule)

    x.myFoo.foo should be === "foo"

    x.mySomeFoo.foo should be === "foo"

    x.myInnerFoo.foo should be === "inner"
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