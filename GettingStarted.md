Getting Started with SubCut
===========================

This document outlines a quick start with SubCut, including downloading the library, setting up your
first binding module, creating injectable classes and using the implicit method of passing configuration
down through your injectable classes. It concentrates on one recommended way to use subcut, in brief:

* Immutable binding modules.
* Binding by trait and trait/name keys.
* Creating an injectable class with an implicit binding module.
* Using the injectIfBound semantics (which only use injection if a configuration is defined for a
  particular trait, otherwise using the provided definition to the right of the injectIfBound method.
* Testing with mutable copies of binding modules.

There are many other ways in which you can use SubCut, including explicit injection, constructor
injection, mixin configuration, and even mutable binding modules (to be used with extreme caution).


Quick Version
=============

This is the high-level overview and cheat sheet for using subcut. For more details, see below.

This is just one recipe that works, and is my recommendation. There are other ways you can use SubCut.

1. Include SubCut in your dependencies or download the jar file. There are no further dependencies beyond
   the Scala runtime libraries.
2. Create a binding module using 

    object SomeConfigurationModule extends NewBindingModule({ module =>
        module.bind[X] toInstance Y
        module.bind[Z] toProvider { codeToGetInstanceOfZ() }
    })

3. For all classes you want injection, or for any that you want to make new injectable instances in, add
   to the class declaration: an (implicit val bindingModule: BindingModule) and trait Injectable, e.g:

    class SomeServiceOrClass(param1: String, param2: Int)(implicit val bindingModule: BindingModule)
        extends SomeTrait with Injectable {...}

4. Within the class where you want to inject bindings, use:

    val service1 = injectIfBound[Service1] { new Service1Impl }

   or similar, the function to the right side of the binding expression is used as default if there is no
   matching binding.
5. For testing, create a obtain a modifiable binding from the normal immutable binding module and rebind
   that:

    test("Some test") {
       SomeConfigurationModule.modifyBindings { testModule =>
         testModule.bind[SomeTrait] toInstance new FakeSomeTrait
         val testInstance = new SomeServiceOrClass(param1, param2)(testModule)
         // test stuff...
       }
    }


Including SubCut on your project
================================

If using another project configuration mechanism than Maven or SBT (e.g. using an IDE), simply download
the latest .jar file for subcut available from the scala-tools maven repository and include that on your classpath.

For maven:

    <dependency>
      <groupId>org.scala-tools</groupId>
      <artifactId>subcut_2.9.0</artifactId>
      <version>0.8</version>
    </dependency>

replace _2.9.0 with the version of Scala you are using (note, for 2.9.0-1, use _2.9.0 as there is no
separate build for this point version). Replace 0.8 with whatever the latest stable version of subcut is.
Snapshot builds are also available from the scala-tools snapshot repo. Note that if you don't have the
scala-tools repo in your list of maven repositories, see http://scala-tools.org for details on how to add
it.

For sbt:

See the instructions for maven about versions and repo configuration. To use subcut in your project, add
the dependency:

    val subcut = “org.scala-tools” %% “subcut” % “0.8”

replacing 0.8 with the latest (or desired) version of subcut.


Setting up a configuration module
=================================

This covers the recommended way to set up and use a single binding module, and demonstrates the common
bindings that can be used. For more possibilities, see the bottom of this section.

To create a new immutable binding module:

    object ProjectConfiguration extends NewBindingModule({ module =>
      module.bind[Database] toInstance new MySQLDatabase
      module.bind[Analyzer] 'webAnalyzer to instanceOfClass[WebAnalyzer]
      module.bind[Session] identifiedBy 'currentUser toProvider { WebServerSession.getCurrentUser().getSession() }
      module.bind[Int] identifiedBy 'maxThreadPoolSize toInstance 10
      module.bind[WebSearch] toLazyInstance { new GoogleSearchService()(ProjectConfiguration) }
    })

The above bindings are as follows:

Database will be bound to a single instance of MySQLDatabase created at the time the NewBindingModule is
created. The same instance will always be returned for this binding so care should be taken that this
single instance is thread-safe if used in a threaded environment.

Analyzer, with the provided identifying name webAnalyzer, will be bound to a class WebAnalyzer and each
time the binding is requested, a new instance of WebAnalyzer will be created via reflection and provided
to the call site. Note that WebAnalyzer must have a zero argument (default) constructor in order for this
binding to work at run time.

Session, with the provided name currentUser, is bound to a function that will provide a Session upon each
use of the binding. This is the most flexible option for providing instances, since the provider method
can do whatever it likes to return that instance, as long as the instance returned is a sub-type of the
trait Session.

The Int identified by maxThreadPoolSize will always return the Int value 10 when used. Note that while
you can bind common types like Int and String without an identifying name, doing do is not recommended
since the resulting binding will be very broad and could be picked up accidentally.

The final binding, trait WebSearch is bound lazily to a single instance obtained by the provided method.
There are a couple of things to note about this binding. The toLazyInstance will defer the instance from
being created until the first time it is bound, but after that the same instance will always be returned
for that binding. The lazy binding is necessary in this case for the second reason - a specific binding
configuration is provided to the GoogleSearchService constructor in the form of a second curried
parameter. It is necessary for this to be included as there is no implicit binding that can be picked up
in scope within the binding module configuration, and the laziness is required to avoid using the
configuration module before it has been defined. If this is confusing to you, don't worry about it until
you have read and understood the implicit binding approach (described below) to providing configuration,
and then it should make more sense.

Note also that the binding module defined here is a singleton object. There is the recommended usage as
it keeps things nice and simple. Define it in some package in your project that denotes configuration,
and makes it easy to find.


Creating an Injectable Class
============================

To use these bindings in your class, the recommended way is to do as follows:

    class DoStuffOnTheWeb(val siteName: String, val date: Date)(implicit val bindingModule: BindingModule) extends Injectable {
      val webSearch = injectIfBound[WebSearch] { new BingSearchService }
      val maxPoolSize = injectIfBound[Int]('maxThreadPoolSize) { 15 }
      val flightLookup = injectIfBound[FlightLookup] { new OrbitzFlightLookup }
      val session = injectIfBound[Session]('currentUser) { Session.getCurrent() }

      def doSomethingCool(searchString: String): String = {
        val webSearch = webSearch.search(searchString)
        val flight = flightLookup(extractFlightDetails(webSearch))
        // ...
      }
    }


Some things to note about the definition:

* The DoStuffOnTheWeb class has a regular constructor parameter list which must be filled in on instance
  creation, so if you want to use it you call it as new DoStuffOnTheWeb(site, date) just as you would
  with a non-injectable class.

* The implicit parameter for the bindingModule is how the binding configuration gets injected into the
  class. By making it an implicit, the compiler can fill it in for us at compile time, and this is the
  apparent "magic" behind subcut - the compiler does our injection for us. By declaring an implicit at
  the top level of our application, then calling new on an injectable class, we start the configuration
  injection all the way down. Any instance can call new on another instance, and because the implicit is
  in scope, the compiler knows what to fill in there. At any point, including the top, a different
  explicit binding can be provided to the class (just provide the second parameter list with the binding
  in it) and it will take over for the rest of the way down when creating new instances. If the chain
  gets broken, the compiler will tell us there is no binding module defined, and this can be fixed by
  adding the injectable trait and the implicit to the current class definition.

  The BindingModule implicit must be called bindingModule, as that is the value that the Injectable trait
  will look for. If bindingModule is not defined, you will get a compile time error. In this way subcut
  helps you to remember to include your injection bindings in an unbroken chain down to the lowest level
  class that needs it.
  
* The Injectable trait must appear in the traits for the injectable class somewhere. This defines all
  of the injection methods like injectIfBound.

* The injectIfBound[Trait] vals defined at the top of the class are where the configuration bindings are
  used. injectIfBound will use the configured definition if one is provided, and if not, it will fall
  back to the provided default on the right hand side of the expression, so for example, in the line:

    val session = injectIfBound[Session]('currentUser) { Session.getCurrent() }

  subcut will look to see if there is a definition bound to trait Session with id 'currentUser, and if so
  it will use that (in this case there is, so Session.getCurrent() will not be evaluated or used).
  However, for the line:

    val flightLookup = injectIfBound[FlightLookup] { new OrbitzFlightLookup }

  subcut looks for the trait FlightLookup (with no extra ID name to identify it) and doesn't find one
  bound, so instead falls back to the default expresion to the right, which is evaluated and results in a
  new instance of OrbitzFlightLookup every time a new instance of our class is created.
  OrbitzFlightLookup can itself be an injected class, and because the implicit parameter for bindings is
  in scope, the compiler will apply those bindings to it automatically for us, so OrbitzFlightLookup
  could be defined as:

    class OrbitzFlightLookup(implicit val bindingModule: BindingModule) extends FlightLookup with Injectable { ... }

  Note that OrbitzFlightLookup must mix in the FlightLookupTrait in order to satisfy the binding, and
  must still include the implicit parameter list even though it doesn't have any constructor parameters.

* The rest of the class can use these injected values as normal through the methods defined on the traits
  they are defined under. You can also inject values at any point in the class, not just in the
  constructor code, and any new Injectable instances you create at any point in the class will get the
  binding configuration passed into them automatically unless you explicitly override them.

injectIfBound is only one form of injection subcut provides, the others being inject[Trait] which will
always inject the trait from the bindingModule definition, and will fail if no such binding is provided.
The other form is injectIfMissing[Trait] where the instance is only bound in if it has not already been
provided in a constructor parameter (see the scaladoc for more information on how to use this). Both of
these other types of injection will fail if no binding has been provided, while injectIfBound will always
fall back on the default if no binding is available. This is one of the reasons it is the recommended way
to use subcut, since you reduce the possibility of runtime binding failure. Another reason is that
programmers reading your code can easily see what the "normal" implementation of a specific trait is,
right there in the class definition.

Using injectIfBound also makes it possible for you to have a completely empty BindingModule for
production, in fact I often do. You must provide the binding module still, so that it may be overridden
for testing or other purposes, but leaving the BindingModule empty means that the defaults will always be
used, and also carries a slight performance advantage if you do so, since if the bindingModule is empty,
the lookup is optimized out when binding. You can still override bindings at any time to change the
default behavior.


Using your BindingModule
========================

So far we have created a binding module, and shown how to inject bindings into traits. The last piece is
to connect the dots.

With a BindingModule provided via the implicit definition (implicit val bindingModule: BindingModule) to
an Injectable class, in order to use a specific module you must do one of two things:

Either, create an implicit value definition before you create the new instance of the top class, like
this:

    implicit val bindingModule = ProjectConfiguration
    val topInstance = new DoStuffOnTheWeb("stuff", new Date())

in which case the binding module will be provided to the DoStuffOnTheWeb automatically, and to all
instances created inside of that as well (this is how you provide a project wide configuration with a
single assignment). Alternatively you could use the explicit (shorthand form) which is:

    val topInstance = new DoStuffOnTheWeb("stuff", new Date())(ProjectConfiguration)

The explicit is only needed for the first instance, as it is implicitly available to all instances under
that (it is defined as an implicit in the parameter list - that makes it implicit in the class scope).
The shorthand form does exactly the same as the implicit val form above, but it's just less typing. This
is also how you can override the implicit binding at any depth in the tree (just provide it explicitly)
and also how you can specify an explicit binding for a new object while creating a new binding module
(use the binding module you are defining, and make the binding lazy or a provider to avoid using the
module before it is ready).

You can break the chain accidentally, but if you do the compiler will give an error. To illustrate this:

- Class A is Injectable with implicit binding
- Class B is not Injectable
- Class C is Injectable with implicit binding
- Class D is not Injectable

if top level instance of Class A creates a new Class B, it can do so just fine. It can also create new
instances of class C and D without problem.

However, class B cannot create an instance of Class C without explicitly providing some kind of
configuration. Class B did not get an implicit binding module (because it does not have the implicit
binding in its parameter list), therefore there is no implicit binding available when it tries to create
a new instance of class C. B can create an instance of class D just fine, since it doesn't need a binding
module either. In other words, the chain must remain unbroken as far down as you use the implicit, but
need go no further (at some point you will likely be dealing with small leaf classes that don't
themselves need anything injected nor use classes that do - at that point you can skip adding Injectable
and the implicit to each class).

The compiler will not compile in the above situation. Instead you will get a compiler error when trying
to create a new instance of class C in class B, since no value for the required bindingModule is
provided. Thus the compiler will help you make sure the chain is intact as far as it needs to go.

To correct the problem, simply add the implicit val bindingModule: BindingModule to a curried constructor
parameter in Class B. You don't even need to make B injectable if you don't need it to be, just so long
as the implicit is carried through. This will keep the implicit chain intact through to class C, and
class D doesn't need either.


Integrating with other libraries
================================

SubCut can easily be integrated with other libraries that are not subcut aware by providing the
bindingModule configuration by a couple of different mechanisms. This includes libraries like, for
example, wicket, where you do not control the creation of new page instances.

The traditional problem is that if you don't control the new instance, how do you get the right
binding configuration to the top level instance created by the library. Normally the library needs some
kind of integration plugin to help provide the right configuration.

In subcut there is an easier way, simply create a new subclass of the Injectable class, and provide a
definition for the bindingModule that is implicit in the constructor of that subClass, e.g.

    class SomePage(implicit val bindingModule: BindingModule) extends WicketPage with Injectable { }

    class ProdSomePage extends SomePage(ProjectConfiguration)

You can now register ProdSomePage with the wicket library to be created when needed. It will always be
bound to the same configuration, but that's normally what you want in production anyway.


Testing with SubCut
===================

At this point it should be clear that you can easily provide your own custom bindings for any new
Injectable instance, and those bindings will be used from that point down in the new instances hierarchy.
There are some enhancements provided for convenience beyond this for testing purposes however, since this
is such a common place to want to change bindings.

A typical test with SubCut overriding will look like this:

    test("Test lookup with mocked out services") {
      ProjectConfiguration.modifyBindings { module =>
        // module now holds a mutable copy of the general bindings, which we can re-bind however we want
        module.bind[Int] identifiedBy 'maxThreadPoolSize to None    // unbind and use the default
        module.bind[WebSearch] toInstance new FakeWebSearchService  // use a fake service defined elsewhere
        module.bind[FlightLookup] toInstance new FakeFlightLookup   // ditto
 
        val doStuff = new DoStuffOnTheWeb("test", new Date())(module)

        doStuff.canFindMatchingFlight should be (true)
        // etc.
      }
    }


In this example, the modifyBindings hands us back a copy of the immutable binding module, but in a
mutable form in which we can change any bindings we like for the tests. In this case we unbind (bind to
None) the Int identified by 'maxThreadPoolSize, and rebind both the WebSearch and FlightLookup traits to
fake ones (which we assume have been defined elsewhere). Mocks would be a better choice here (I use
borachio, and it works great with subcut) but you get the point. Now, when we call new DoStuffOnTheWeb,
we provide the mutable module available in the test, and that gets passed down the chain for new objects
starting with the DoStuffOnTheWeb instance. Any use of WebSearch injection at any depth under this
instance of DoStuffOnTheWeb will get the fake service instead of the real one, but the rest of the system
will be unaffected. A new copy module is created every time we do modifyBindings, so you can test in
parallel without any cross configuration polution from rebindings in other tests. At the end of the test,
the temporary module is released and can be garbage collected when necessary.


Other Notes
===========

SubCut provides many features similar to the cake pattern popular for dependency injection, but has the
advantage of providing full control over binding configuration at any depth, while still being able to
create new instances without factories, and using constructor parameters if you desire. It is great for
injection and testing, particularly functional testing (which can in my experience get tricky in large
projects using the cake pattern). 

It has one major feature missing when compared with cake though. With Cake, the compiler can always tell
if a suitable binding has been provided at compile time, while because of the nature of the dynamic
binding and lookup used in subcut, sometimes although the program compiles, a binding may be missing at
runtime and this can be hard to detect.

There are two main ways of reducing the risk of this occuring:

1. Use injectIfBound and always supply default implementations, which is better for readability anyway,
   and means that there will never be a BindingException thrown at runtime, and

2. Test all of your instances with an empty bindingModule assuming you do use injectIfBound, or
   alternatively bind your standard configuration module (or each module in turn) implicitly into scope,
   and then create new instances of your injectable classes. This will enable you to pick up any missing
   or misconfigured bindings at testing time, rather than in production.

To be fair, Cake is the only DI approach I have ever seen where the compiler can provide that level of
safety. Most (all) other DI approaches I have seen can get runtime failures if required bindings are
unavailable, so subcut is not all that unusual in this regard, although I have tried hard to provide
strategies for working around the risk in practice.

Finally we have been using SubCut for some time now interally at Locus Development, and it works well for
us. I would like to thank Locus Development for their patience and support while I developed SubCut, and
also for providing a testing ground for it. I will also remind you that SubCut is currently in pre-alpha
(0.8) and while it works great for us, it might eat your children, blow up your servers and other
horrible things that I am not responsible for. I would however like bug reports if it does do any of the
above (or just plain doesn't work for some reason). The pre 1.0 tag should also serve as a reminder that
while I will try and keep the API stable, there could well be changes that will break your code in the
months ahead as we approach a stable 1.0 release.

Thanks, and Happy SubCutting.
