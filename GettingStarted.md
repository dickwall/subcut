# Getting Started with SubCut

This document outlines a quick start with SubCut, including downloading the library, setting up your
first binding module, creating injectable classes and using the implicit method of passing configuration
down through your injectable classes. It concentrates on one recommended way to use subcut, in brief:

* Immutable binding modules.
* Binding by trait and trait/name keys.
* Creating an injectable class with an implicit binding module.
* Using the injectOptional semantics (which only use injection if a configuration is defined for a
  particular trait, otherwise using the provided definition to the right of the injectOptional method.
* Testing with mutable copies of binding modules.

There are many other ways in which you can use SubCut, including explicit injection, constructor
injection, mixin configuration, and even mutable binding modules (to be used with extreme caution).


## Quick Version

This is the high-level overview and cheat sheet for using subcut. For more details, see below.

This is just one recipe that works, and is my recommendation. There are other ways you can use SubCut.

1. Include SubCut in your dependencies or download the jar file. There are no further dependencies beyond
   the Scala runtime libraries.

2. Create a binding module using, e.g.

    ```scala
    object PoolSize extends BindingId   // probably in another binding IDs file
    object ServerURL extends BindingId

    object SomeConfigurationModule extends NewBindingModule (module => {
      import module._  // optional but convenient - allows use of bind instead of module.bind

      bind [X] toSingle Y
      bind [Z] toProvider { codeToGetInstanceOfZ() }
      bind [A] toProvider { implicit module => new AnotherInjectedClass(param1, param2) } // module singleton
      bind [B] to newInstanceOf [Fred]    // create a new instance of Fred every time - Fred require injection
      bind [C] to moduleInstanceOf [Jane] // create a module scoped singleton Jane that will be used
      bind [Int] idBy PoolSize to 3       // bind an Int identified by PoolSize to constant 3
      bind [String] idBy ServerURL to "http://escalatesoft.com"
    })
    ```

3. For all classes you want injection, or for any that you want to make new injectable instances in, add
   to the class declaration: an (implicit val bindingModule: BindingModule) and trait Injectable, e.g:
   
    ```scala
    
    class SomeServiceOrClass(param1: String, param2: Int)(implicit val bindingModule: BindingModule)
        extends SomeTrait with Injectable {...}
    ```

    You can also use the AutoInjectable trait with the subcut compiler plugin (the plugin must be on the
    class path for the compiler) to avoid having to include the implicit val binding module definition, e.g:

    ```scala
    class SomeServiceOrClass(param1: String, param2: Int) extends SomeTrait with AutoInjectable {...}
    ```

    This will be expanded by the compiler plugin to be equivalent to the Injectable form above. If you forget
    to include the compiler plugin on the classpath, you will see errors about a missing abstract bindingModule
    field.

4. Within the class where you want to inject bindings, use the following code 
   or similar, the function to the right side of the binding expression is used as default if there is no
   matching binding.

    ```scala
      val service1 = injectOptional [Service1] getOrElse { new Service1Impl }
    ```

5. For testing, create a obtain a modifiable binding from the normal immutable binding module and rebind
   that:

    ```scala
    test("Some test") {
       SomeConfigurationModule.modifyBindings { implicit testModule =>  // modifiable test module is now default
         testModule.bind [SomeTrait] toSingle new FakeSomeTrait
         val testInstance = new SomeServiceOrClass(param1, param2)   // uses test module available implicitly
         // test stuff...
       }
    }
    ```

## Including SubCut on your project

If using another project configuration mechanism than Maven or SBT (e.g. using an IDE), simply download
the latest .jar file for subcut available from the scala-tools maven repository and include that on your classpath.

For maven:

```xml
    <dependency>
      <groupId>com.escalatesoft.subcut</groupId>
      <artifactId>subcut_2.10</artifactId>
      <version>2.0</version>
    </dependency>
```

replace _2.10.1 with the version of Scala you are using.
Replace 2.0 with whatever the latest stable version of subcut is (or add -SNAPSHOT if you want a snapshot).
Snapshot and release builds are available from the maven central repo.

For sbt:

See the instructions for maven about versions and repo configuration. To use subcut in your project, add
the dependency:

```scala
    "com.escalatesoft.subcut" %% "subcut" % "2.0"
```

replacing 2.0 with the latest (or desired) version of subcut.


## Setting up a configuration module

This covers the recommended way to set up and use a single binding module, and demonstrates the common
bindings that can be used. For more possibilities, see the bottom of this section.

To create a new immutable binding module:

```scala

    object BindingKeys {   // in some other file?
      object WebAnalyzerId extends BindingId
      object CurrentUserId extends BindingId
      object MaxThreadPoolSizeId extends BindingId
    }

    object ProjectConfiguration extends NewBindingModule(module => {
      import module._   // can now use bind directly

      import BindingKeys._  // use the Binding IDs conveniently
      
      bind [Database] toSingle new MySQLDatabase
      bind [Analyzer] idBy WebAnalyzerId to moduleInstanceOf [WebAnalyzer]  // module singleton
      bind [Session] idBy CurrentUserId toProvider { WebServerSession.getCurrentUser().getSession() }
      bind [Int] idBy MaxThreadPoolSizeId toSingle 10
      bind [WebSearch] toModuleSingle { implicit module => new GoogleSearchService() }
    })
```

The above bindings are as follows:

Database will be bound to a single instance of MySQLDatabase created the first time this Database binding is
requested. The same instance will always be returned for this binding so care should be taken that this
single instance is thread-safe if used in a threaded environment.

Analyzer, with the provided identifying ID WebAnalyzerId, will be bound to a class WebAnalyzer. When the
Analyzer is first requested for a module, a new WebAnalyzer will be created (and injected if required)
and will then be re-used for every other request made within that module, HOWEVER, if the module is
modified, or merged in with another, or reconfigured in any way, the binding will be reset, and the next
time Analyzer is requested, the new WebAnalyzer will be created again and used from that point on. This
allows configuration to change in new binding modules and get picked up when that new configuration is used
but avoids the cost of creating a new item every time. This is called Module Singleton, and it is new in 2.0.

Session, with the provided ID CurrentUserID, is bound to a function that will provide a Session upon each
use of the binding. This is the most flexible option for providing instances, since the provider method
can do whatever it likes to return that instance, as long as the instance returned is a sub-type of the
trait Session. Another option here is to use the form `toProvider { implicit module => new SomeSession }`
where SomeSession may itself be injectable. The module is provided to the provider method to use for
configuration, keeping everything consistent (and should be used whenever a module is required, rather
than referring to the module this binding is being defined in, since others may wish to override configuration
later).

The Int identified by MaxThreadPoolSizeId will always return the Int value 10 when used. Note that while
you can bind common types like Int and String without an identifying name, doing so is not recommended
since the resulting binding will be very broad and could be picked up accidentally.

The final binding, trait WebSearch is bound lazily to a module singleton obtained by the provided method.
There are a couple of things to note about this binding. The toModuleSingle will defer the instance from
being created until the first time it is requested, but after that the same instance will always be returned
for that module. Like the module provider example above, the binding module configuration is passed in to
the function to produce the web service, marking it implicit on the way in means that it can be used
by default for any injectable classes in that function. This differs from the module provider form in that
toModuleSingle always returns the same cached instance for a module, while toProvider { module => ... }
always runs the function again. Also, toModuleSingle bindings will be reset when any copy is taken
from the current module (i.e. it is merged in to another module or the modifyBindings method is used
in tests) and will be re-bound on the next request.

Note also that the binding module defined here is a singleton object. There is the recommended usage as
it keeps things nice and simple. Define it in some package in your project that denotes configuration,
and makes it easy to find.

In Subcut 2.0 there is a new convenience method for making binding modules without needing to create
an object or class:

```scala
    import NewBindingModule._

    implicit val projectConfiguration = newBindingModule { module =>
      import module._   // can now use bind directly

      // ...
    }
```


## Creating an Injectable Class

To use these bindings in your class, the recommended way is to do as follows:

```scala
    import BindingKeys._    // convenient access to the Binding IDs

    class DoStuffOnTheWeb(val siteName: String, val date: Date)(implicit val bindingModule: BindingModule) extends Injectable {
      val webSearch = injectOptional [WebSearch] getOrElse { new BingSearchService }
      val maxPoolSize = injectOptional [Int](MaxThreadPoolSizeId) getOrElse { 15 }
      val flightLookup = injectOptional [FlightLookup] getOrElse { new OrbitzFlightLookup }
      val session = injectOptional [Session](CurrentUserID) getOrElse { Session.getCurrent() }

      def doSomethingCool(searchString: String): String = {
        val webSearch = webSearch.search(searchString)
        val flight = flightLookup(extractFlightDetails(webSearch))
        // ...
      }
    }
```

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

* The injectOptional [Trait] vals defined at the top of the class are where the configuration bindings are
  used. injectOptional will use the configured definition if one is provided, and if not, the getOrElse will fall
  back to the provided default on the right hand side of the expression, so for example, in the line:

```scala
    val session = injectOptional [Session](CurrentUserId) getOrElse { Session.getCurrent() }
```

  subcut will look to see if there is a definition bound to trait Session with id 'currentUser, and if so
  it will use that (in this case there is, so Session.getCurrent() will not be evaluated or used).
  However, for the line:

```scala
    val flightLookup = injectOptional [FlightLookup] getOrElse { new OrbitzFlightLookup }
```

  subcut looks for the trait FlightLookup (with no extra ID name to identify it) and doesn't find one
  bound, so instead falls back to the default expresion in the getOrElse, which is evaluated and results in a
  new instance of OrbitzFlightLookup every time a new instance of our class is created.
  OrbitzFlightLookup can itself be an injected class, and because the implicit parameter for bindings is
  in scope, the compiler will apply those bindings to it automatically for us, so OrbitzFlightLookup
  could be defined as:

```scala
    class OrbitzFlightLookup(implicit val bindingModule: BindingModule) extends FlightLookup with Injectable { ... }
```

  Note that OrbitzFlightLookup must mix in the FlightLookupTrait in order to satisfy the binding, and
  must still include the implicit parameter list even though it doesn't have any constructor parameters.
  If you want to avoid adding the implicit parameter everywhere, see AutoInjectable below.

* The rest of the class can use these injected values as normal through the methods defined on the traits
  they are defined under. You can also inject values at any point in the class, not just in the
  constructor code, and any new Injectable instances you create at any point in the class will get the
  binding configuration passed into them automatically unless you explicitly override them.

injectOptional is only one form of injection subcut provides, the others being inject [Trait] which will
always inject the trait from the bindingModule definition, and will throw an exception if no such binding is provided.
The other form is injectIfMissing [Trait] where the instance is only bound in if it has not already been
provided in a constructor parameter (see the scaladoc for more information on how to use this). Both of
these other types of injection will fail if no binding has been provided, while injectOptional will always
fall back on the default if no binding is available. This is one of the reasons it is the recommended way
to use subcut, since you reduce the possibility of runtime binding failure. Another reason is that
programmers reading your code can easily see what the "normal" implementation of a specific trait is,
right there in the class definition.

Using injectOptional also makes it possible for you to have a completely empty BindingModule for
production, in fact I often do. You must provide the binding module still, so that it may be overridden
for testing or other purposes, but leaving the BindingModule empty means that the defaults will always be
used, and also carries a slight performance advantage if you do so, since if the bindingModule is empty,
the lookup is optimized out when binding. You can still override bindings at any time to change the
default behavior.


## Using your BindingModule

So far we have created a binding module, and shown how to inject bindings into traits. The last piece is
to connect the dots.

With a BindingModule provided via the implicit definition (implicit val bindingModule: BindingModule) to
an Injectable class, in order to use a specific module you must do one of two things:

Either, create an implicit value definition before you create the new instance of the top class, like
this:

```scala
    implicit val bindingModule = ProjectConfiguration
    val topInstance = new DoStuffOnTheWeb("stuff", new Date())
```

in which case the binding module will be provided to the DoStuffOnTheWeb automatically, and to all
instances created inside of that as well (this is how you provide a project wide configuration with a
single assignment). Alternatively you could use the explicit (shorthand form) which is:

```scala
    val topInstance = new DoStuffOnTheWeb("stuff", new Date())(ProjectConfiguration)
```

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
class D doesn't need either. The other alternative is to mix in AutoInjectable to all the classes, as this
will add the implicit parameter automatically through the compiler plugin.


## Easy Module Merging

There are two easy ways of merging modules together in SubCut (these are useful for splitting up configuration
into separate modules for maintainability, and then combining them into one uber-module for configuration, or
indeed allowing lots of flexibility in combining them in different ways).

In-line merging looks like this:

```scala
// in one location for use:
implicit val = AppConfiguration ~ ProjectDefaults ~ CompanyDefaults
```

This will use bindings found in AppConfiguration first, then if a match can't be found it will look in ProjectDefaults
and finally in CompanyDefaults, and will then consider that there is no binding if it can't find any. Not everyone
likes to use operators in libraries, so there is a wordy equivalent:

```scala
implicit val = AppConfiguration andThen ProjectDefaults andThen CompanyDefaults
```

These work identically, so just decide if you prefer ~ or andThen.

You can also reconfigure your configuration easily with this ~ merger in another place, e.g. a test:

```scala
// another configuration, in a test, say
implicit val = TestOverrides ~ AppConfiguration ~ ProjectDefaults ~ CompanyDefaults
```

There is also Module Definition merging, which looks like this:

```scala
object UberModule extends NewBindingModule ( module => {
  import module._

  module <~ CompanyDefaults
  module <~ ProjectDefaults

  bind [AppService] idBy SomeIdentified toModuleSingle { implicit module => new WackyWonderfulAppService }
})
```

In this form, bindings are overwritten as the configuration progresses, so CompanyDefaults will be merged in
first, then when ProjectDefaults are merged in, any duplicates will be overwritten by ProjectDefaults. Finally
we can provide some of our own custom bindings after merging in all of the others. Again, a full text method
name equivalent for <~ is available:

```scala
object UberModule extends NewBindingModule ( module => {
  import module._

  module mergeWithReplace CompanyDefaults
  module mergeWithReplace ProjectDefaults

  bind [AppService] idBy SomeIdentified toModuleSingle { implicit module => new WackyWonderfulAppService }
})
```

This is functionally identical to the example above (<~ and mergeWithReplace are aliases).


## AutoInjectable and the Compiler Plugin

For convenience, subcut includes a compiler plugin which can be used to avoid needing to add the
(implicit val bindingModule: BindingModule) curried parameter list in new class definitions. To use it, you
need to do the following:

* Include the compiler plugin on the classpath for the compiler:

  In sbt, use

```scala
  addCompilerPlugin("com.escalatesoft.subcut" %% "subcut" % "2.0") // (or 2.0-SNAPSHOT right now)
```
  in the project build settings. In your IDE or other build environment, use the recommended method there
  to add the compiler plugin.

* Use the AutoInjectable trait instead of Injectable and skip the implicit val bindingModule parameter:

```scala
  class DoStuffOnTheWeb(val siteName: String, val date: Date) extends AutoInjectable {
    // ...
  }
```

When the compiler plugin sees the AutoInjectable trait on a class definition, it will automatically add
the implicit parameter before continuing with compilation. If you see errors about AutoInjectable classes
not having a bindingModule defined, it indicates that the compiler plugin is not on the compile time classpath
correctly so check your settings.

Use of the compiler plugin for subcut is completely optional. The code generated is identical to using Injectable
with the implicit parameter for bindingModule added manually, so if you prefer not to use compiler plugins, just
stick to Injectable and the implicit parameter. At present, most IDEs do not integrate compiler plugins into the
compile cycle, so if you use AutoInjectable, you may get warnings about errors in the code in your IDE even though
the compiler has no problem. If this bothers you, skip the compiler plugin and just put the implicit parameters
back in your code.


## Integrating with other libraries

SubCut can easily be integrated with other libraries that are not subcut aware by providing the
bindingModule configuration by a couple of different mechanisms. This includes libraries like, for
example, wicket, where you do not control the creation of new page instances.

The traditional problem is that if you don't control the new instance, how do you get the right
binding configuration to the top level instance created by the library. Normally the library needs some
kind of integration plugin to help provide the right configuration.

In subcut there is an easier way, simply create a new subclass of the Injectable (or AutoInjectable) class,
and provide a definition for the bindingModule that is implicit in the constructor of that subClass, e.g.

```scala
    class SomePage extends WicketPage with AutoInjectable { }

    class ProdSomePage extends SomePage(ProjectConfiguration)
```

You can now register ProdSomePage with the wicket library to be created when needed. It will always be
bound to the same configuration, but that's normally what you want in production anyway.


## Testing with SubCut

At this point it should be clear that you can easily provide your own custom bindings for any new
Injectable instance, and those bindings will be used from that point down in the new instances hierarchy.
There are some enhancements provided for convenience beyond this for testing purposes however, since this
is such a common place to want to change bindings.

A typical test with SubCut overriding will look like this:

```scala
    test("Test lookup with mocked out services") {
      ProjectConfiguration.modifyBindings { implicit module =>  // implicit makes the test module default
        import module._
        // module now holds a mutable copy of the general bindings, which we can re-bind however we want
        bind [Int] identifiedBy MaxThreadPoolSizeId to None    // unbind and use the default
        bind [WebSearch] toSingleInstance new FakeWebSearchService  // use a fake service defined elsewhere
        bind [FlightLookup] toSingleInstance new FakeFlightLookup   // ditto

        val doStuff = new DoStuffOnTheWeb("test", new Date())   // test module is used implicitly

        doStuff.canFindMatchingFlight should be (true)
        // etc.
      }
    }
```


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


## Other Notes

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

1. Use injectOptional and always supply default implementations, which is better for readability anyway,
   and means that there will never be a BindingException thrown at runtime, and

2. Test all of your instances with an empty bindingModule assuming you do use injectOptional, or
   alternatively bind your standard configuration module (or each module in turn) implicitly into scope,
   and then create new instances of your injectable classes. This will enable you to pick up any missing
   or misconfigured bindings at testing time, rather than in production.

To be fair, Cake is the only DI approach I have ever seen where the compiler can provide that level of
safety. Most (all) other DI approaches I have seen can get runtime failures if required bindings are
unavailable, so subcut is not all that unusual in this regard, although I have tried hard to provide
strategies for working around the risk in practice.

Finally I would like to thank the various contributors who made
the 2.0 release possible, including: ptillemans, dholbrook, aerskine, iron9light, nadavwr, cessationoftime,
artemkozlov and crispywalrus (if I missed you, sorry, I took these from the pull requests in GitHub). 
While I could not always merge in the pull requests, all of them were greatly
appreciated and often incorporated in some form (when I could I merged, when I couldn't I cherrypicked).
Also, thanks to everyone who submitted bug reports, messages of support, wrote articles, badgered me to
put out snapshots for new versions of Scala, and anything else that helped get to 2.0.

Thanks, and Happy SubCutting.
