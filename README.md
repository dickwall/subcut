SubCut Extended README
=============

SUBCUT 2.5 WILL ADD SUPPORT FOR PROPERTY INJECTION SO I'M GOING TO STOP ANY SUPPORT FOR THIS PROJECT WHEN IT WILL BE RELEASED

This project is an extension of SubCut (https://github.com/dickwall/subcut) I'm keeping active while waiting for the extension
code to be merged in the original project.

I added some feature to implement injection similarly to the @value annotation in spring. For example:

```(scala)
class ToInject(implicit val bindingModule: BindingModule) extends Injectable {
    val stringProperty = injectProperty[String]("property1")
    val intProperty = injectProperty[Int]("property2")
    val longProperty = injectProperty[Long]("property3")
    val floatProperty = injectProperty[Float]("property4")
}
```

Allowing to inject in this way:

```(scala)
{
  implicit val bindingModule = newBindingModuleWithConfig(PropertiesConfigPropertySource.fromPath("src/test/resources/test.properties"))

   val configReaderInstance = new ToInject
}
```

It supports injection of basic types and allows generic types injection if a custom conversion function is provided.

SubCut README
=============

SubCut, or Scala Uniquely Bound Classes Under Traits, is a mix of service locator and dependency
injection patterns designed to provide an idiomatic way of providing configured dependencies to scala
applications. It is not a full inversion of control solution like Spring, but instead provides flexible
and nearly invisible binding of traits to instances, classes or provider methods, along with a convenient
binding DSL (Domain Specific Language) and an emphasis on convenience for developers, compile time performance, 
compile time type safety and immutability.

It is also small (a few hundred lines of code) and has no dependencies other than the Scala runtime
libraries (plus scalatest and junit if you want to build from source and run the tests).

The SubCut library is available as open source under the Apache v2 license.

Documentation
=============

The scaladoc in the source code is reasonably complete, and will continue to be improved. The unit tests
provide further code examples of how subcut can be used, but does not attempt to demonstrate recommended
uses or effective recipes. There is a [Getting Started](https://github.com/dickwall/subcut/blob/master/GettingStarted.md) document that spells out the quickest way to get
going, and (in the author's opinion), the best way to use subcut effectively.

Scaladocs can be found on the [GitHub home page for SubCut](http://dickwall.github.com/subcut).

Or, take a look at an overview of [What's new in SubCut 2.0](https://github.com/dickwall/subcut/blob/master/NewIn2.0.md)
