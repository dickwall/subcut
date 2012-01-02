package org.scala_tools.subcut.inject

import org.scalatest.{SeveredStackTraces, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TypeErasureTest extends FunSuite with ShouldMatchers with SeveredStackTraces {
  test("Can inject types regardless of erasure in binding keys") {
    val actual = new Injectable {
      val bindingModule = TypeErasureModule
      val intList = inject [List[Int]]
      val stringList = inject [List[String]]
      val intListOfListsFoo = inject [List[List[Int]]] ('foo)
      val stringListOfListsFoo = inject [List[List[String]]] ('foo)
    }
    actual.intList should be { List(1, 2, 3) }
    actual.stringList should be { List("a", "b", "c") }
    actual.intListOfListsFoo should be { List(List(1, 2, 3)) }
    actual.stringListOfListsFoo should be { List(List("a", "b", "c")) }
  }
}

object TypeErasureModule extends NewBindingModule({ module =>
  import module._
  bind [List[Int]] toSingle List(1, 2, 3)
  bind [List[String]] toSingle List("a", "b", "c")
  bind [List[List[Int]]] identifiedBy 'foo toSingle List(List(1, 2, 3))
  bind [List[List[String]]] identifiedBy 'foo toSingle List(List("a", "b", "c"))
})