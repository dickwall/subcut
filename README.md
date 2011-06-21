SubCut README
=============

SubCut, or Scala Uniquely Bound Classes Under Traits, is a mix of service locator and dependency
injection patterns designed to provide an idiomatic way of providing configured dependencies to scala
applications. It is not a full inversion of control solution like Spring, but instead provides flexible
and nearly invisible binding of traits to instances, classes or provider methods, along with a convenient
binding DSL (Domain Specific Language) and an emphasis on immutability, compile time type safety, and
convenience for developers.

It is also small (a few hundred lines of code) and has no dependencies other than the Scala runtime
libraries (plus scalatest and junit if you want to build from source and run the tests).

The SubCut library is available as open source under the Apache v2 license.

Documentation
=============

The scaladoc in the source code is reasonably complete, and will continue to be improved. The unit tests
provide further code examples of how subcut can be used, but does not attempt to demonstrate recommended
uses or effective recipes. There is a [Getting Started](https://github.com/dickwall/subcut/blob/master/GettingStarted.md) document that spells out the quickest way to get
going, and (in the author's opinion), the best way to use subcut effectively.

To create the scaladocs (until I find somewhere to host them) download the source code and use either
maven or sbt to create them.
