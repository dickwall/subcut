package org.scala_tools.subcut.inject

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{SeveredStackTraces, FunSuite}
import org.scala_tools.subcut.inject.Injected._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 4/29/11
 * Time: 6:38 AM
 */

@RunWith(classOf[JUnitRunner])
class ConstructorInjectionTest extends FunSuite with ShouldMatchers with SeveredStackTraces {
  test("Use defined binding, supply no constructor parameter") {
    implicit val bindings = AnimalModule
    val ad = new AnimalDomain
    ad.soundsFromDomain should be ("Woof Woof Woof ")
  }

  test("Use supplied animal in constructor parameter") {
    implicit val bindings = AnimalModule
    val ad = new AnimalDomain(new Frog)
    ad.soundsFromDomain should be ("Ribbit Ribbit Ribbit ")
  }

  test("Force an unbind, should fail with a binding exception if no parameter supplied but work otherwise") {
    AnimalModule.modifyBindings { animalModule =>
      animalModule.unbind[Animal]

      // should still work for explicit parameter in constructor
      val ad = new AnimalDomain(new Frog)(animalModule)
      ad.soundsFromDomain should be ("Ribbit Ribbit Ribbit ")

      // but not for binding case any more
      intercept[BindingException] {
        val ad2 = new AnimalDomain()(animalModule)
      }
    }
  }
}

trait Animal {
  def makeNoise(): String
}

class Frog extends Animal {
  def makeNoise() = "Ribbit"
}

class Dog extends Animal {
  def makeNoise() = "Woof"
}

object AnimalModule extends NewBindingModule({ module =>
  module.bind[Animal].toClass[Dog]
})

class AnimalDomain(an: Animal = injected)(implicit val bindingModule: BindingModule) extends Injectable {
  val animal = injectIfMissing[Animal](an)

  def soundsFromDomain(): String = {
    (animal.makeNoise + " ") * 3
  }
}
