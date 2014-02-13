package com.escalatesoft.subcut.inject.util

import org.scalatest.{SeveredStackTraces, Matchers, FunSuite}
import java.io.File
import com.escalatesoft.subcut.inject.BindingException

/**
 * Test the property file module parsing
 */
class PropertyFileModuleParserTest extends FunSuite with Matchers with SeveredStackTraces {

  test("should throw exception if passed non existent file") {
    val exc = intercept[BindingException] {
      PropertyFileModule(new File("nosuchfile.txt"))
    }

    exc.getMessage should be ("No file nosuchfile.txt found")
  }

  test("should parse a simple text file without error") {
    val bindings = PropertyFileModule(new File(getClass.getClassLoader.getResource("simplestringbindings.txt").getFile))
  }

  test("the bindings for simple text properties should be valid") {
    val bindings = PropertyFileModule(new File(getClass.getClassLoader.getResource("simplestringbindings.txt").getFile))
    bindings.inject[String](Some("fish")) should be ("chips")
    bindings.inject[String](Some("bacon")) should be ("eggs")
    bindings.inject[String](Some("salt")) should be ("pepper")
    bindings.inject[String](Some("robot")) should be ("laser eyes")
    bindings.inject[String](Some("run")) should be ("hide")
    bindings.listBindings.size should be (5)
  }

  test("should parse a typed properties binding file without error") {
    val bindings = PropertyFileModule(new File(getClass.getClassLoader.getResource("propbindings.txt").getFile))
  }

  test("the bindings for typed properties should be valid") {
    val bindings = PropertyFileModule(new File(getClass.getClassLoader.getResource("propbindings.txt").getFile))
    bindings.listBindings.size should be (10)
    bindings.inject[String](Some("simple1")) should be ("hello")
    bindings.inject[String](Some("simple2")) should be ("well, hello there")
    bindings.inject[Int](Some("someInt")) should be (6)
    bindings.inject[Int](Some("anotherInt")) should be (7)
    bindings.inject[Long](Some("someLong")) should be (231L)
    bindings.inject[Float](Some("someFloat")) should be (23.21F +- 0.0001F)
    bindings.inject[Double](Some("someDouble")) should be (25.222 +- 0.0001)
    bindings.inject[Boolean](Some("someBoolean")) should be (true)
    bindings.inject[Boolean](Some("someFalseBoolean")) should be (false)
    bindings.inject[Char](Some("someChar")) should be ('a')
  }

  test("the bindings should be typed to the correct type and not respond to requests for other types") {
    val bindings = PropertyFileModule(new File(getClass.getClassLoader.getResource("propbindings.txt").getFile))
    intercept[BindingException] {
      bindings.inject[Int](Some("simple1"))
    }
    intercept[BindingException] {
      bindings.inject[Float](Some("someDouble"))
    }
    intercept[BindingException] {
      bindings.inject[Boolean](Some("noSuchBoolean"))
    }
    intercept[BindingException] {
      bindings.inject[Char](Some("anotherInt"))
    }
    intercept[BindingException] {
      bindings.inject[Long](Some("someInt"))
    }
  }

  test("incorrect formats should throw BindingException on property module load") {
    assert(intercept[BindingException] {
      PropertyFileModule(new File(getClass.getClassLoader.getResource("badintprop.txt").getFile))
    }.getMessage.contains ("""Could not parse "seven" for some.bad.[Int]"""))

    assert(intercept[BindingException] {
      PropertyFileModule(new File(getClass.getClassLoader.getResource("baddoubleprop.txt").getFile))
    }.getMessage.contains ("""Could not parse "two point five" for some.bad.[Double]"""))

    assert(intercept[BindingException] {
      PropertyFileModule(new File(getClass.getClassLoader.getResource("badboolprop.txt").getFile))
    }.getMessage.contains ("""Could not parse "Truee" for some.bad.[Boolean]"""))

    assert(intercept[BindingException] {
      PropertyFileModule(new File(getClass.getClassLoader.getResource("badcharprop.txt").getFile))
    }.getMessage.contains ("""Could not parse "" for some.bad.[Char]"""))
  }

  val seqStringParser = new PropertyParser[Seq[String]] {
    def parse(propString: String): Seq[String] = propString.split(',').map(_.trim).toList
  }

  case class Person(first: String, last: String, age: Int)

  val personParser = new PropertyParser[Person] {
    def parse(propString: String): Person = {
      val fields = propString.split(',').map(_.trim)
      Person(fields(1), fields(0), fields(2).toInt)
    }
  }

  test("custom formats should be parsed when a custom parser is provided, but not otherwise") {
    intercept[BindingException] {
      PropertyFileModule(new File(getClass.getClassLoader.getResource("custompropbindings.txt").getFile))
    }.getMessage should be ("No provided parser for type [Seq[String]] in properties custompropbindings.txt")

    val withSeqStringParser = PropertyMappings.Standard + ("Seq[String]" -> seqStringParser)

    intercept[BindingException] {
      PropertyFileModule(new File(getClass.getClassLoader.getResource("custompropbindings.txt").getFile), withSeqStringParser)
    }.getMessage should be ("No provided parser for type [Person] in properties custompropbindings.txt")

    val withBothParsers = PropertyMappings.Standard + ("Seq[String]" -> seqStringParser) + ("Person" -> personParser)

    val workingBindings = PropertyFileModule(new File(getClass.getClassLoader.getResource("custompropbindings.txt").getFile), withBothParsers)
    workingBindings.inject[Seq[String]](Some("seq.of.strings")) should be (List("hello", "there", "today"))
    workingBindings.inject[Person](Some("some.person")) should be (Person("Dick", "Wall", 25))
    workingBindings.listBindings.size should be (12)
  }
}
