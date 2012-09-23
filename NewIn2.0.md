# What's New in 2.0?

* Reflective binding to injectable classes is now possible. In subcut 1.0, only no-constructor-parameter classes
  could be injected reflectively, and that meant no implicit binding modules either. Subcut 2.0 now
  will inject no-constructor-parameter classes and also classes that require only an implicit binding module.
  It can't reflectively inject other constructor parameters (because, how would you? :-) ) but it will work
  with any class that has a default constructor now, or with any class that has a single implicit binding module
  constructor parameter.

* Also, bind [X] to newInstanceOf [Y] now works correctly, and this is how you can bind trait X to a new
  reflectively created instance of Y each time. Note also see bind [X] to moduleInstanceOf [Y] below for
  a module-specific singleton.

* the form

  ```scala
  bind [X] toClass [Y]
  ```

  has been deprecated in favor of either "to newInstanceOf[Y]" or "to moduleInstanceOf[Y]" instead. They are
  more expressive, and toClass [Y] often messed up scala's semicolon inference anyway.

* Re-settable singleton bindings when modules are altered: one problem with subcut 1.0 is that once a
  lazy binding had been evaluated, it always had that value even if you altered the module - traits
  could be explicitly re-bound, but in some cases other instances that were injected with those re-bound
  values were not updated, so they had the old instance with the old inner binding still in it. In subcut
  2.0 you have a new moduleInstanceOf[] binding which resets any time a module is updated, and forces
  the instance to be recreated and injected if a new binding module is created. you can use it like this:

  ```scala
  bind [SomeTrait] to moduleInstanceOf [SomeClassThatIsItselfInjected]
  ```

* Another problem with subcut 1.0 was that binding modules were completed by the compiler at the definition site
  (as it must be) and usually the module used was "this" module. The upshot was that if you re-bound some
  trait or value in a module that you merge in later, the value bound in this module was still used (since the
  future binding is unavailable to the compiler). In subcut 2.0, alternatives exist. Use either to newInstanceOf
  or moduleInstanceOf to delay binding until the final module is constructed and used, or use the new form of the
  provider method like this:

  ```scala
  bind [SomeTrait] toProvider { module => new InjectedClassWithParams(param1, param2)(module) } // explicit binding
  ```

  or

  ```scala
  bind [SomeTrait] toProvider { implicit module => new InjectedClassWithParams(param1, param2) } // implicit binding
  ```

  What happens in this form is that subcut makes the current binding module, whatever the configuration is,
  available to the provider method, so that the current configuration may be used to inject instances in the
  method, rather than what is available when the compiler sees the definition. The old form of toProvider (that
  does not provide a binding module to the function is still available for efficiency and backwards compatibility).

* Continuing in the theme of lazy module binding and resettable bindings, a new toModuleSingle binding is available
  also:

  ```scala
  bind [SomeTrait] toModuleSingle { implicit module => new MostlySingletonInstance(param1, param2) }
  ```

  This binds a late-bound singleton into the module, but if the module is merged with another or otherwise altered,
  the lazy binding is reset so you will get a new singleton (with potentially a difference configuration) within
  that module when you first use it.

* A new convenient syntax for creating a binding module that will be assigned to a val (rather than making an
  object to hold the configuration). Use it like this:

  ```scala
  import NewBindingModule._       // import the newBindingModule convenience function
  implicit val bindingModule = newBindingModule { implicit module =>   // look ma - no object!
     bind [DBLookup] toProvider { module => new MySQLLookup(module) }
     bind [WebService] to newInstanceOf[RealWebService]
     bind [Int] identifiedBy 'maxPoolSize toSingle 10   // could also use idBy instead of identifiedBy
     bind [QueryService] to moduleInstanceOf[SlowInitQueryService]
  }

* In much of the subcut 1.0 example code, the first thing to happen in a NewBindingModule was that module was
  made implicit, like this:

  ```scala
  object MyBindings extends NewBindingModule({ implicit module =>
    module.bind [Z] toSingle new SomeClass
  }
  ```

  This is no longer a recommended practice, because the module used by the compiler to satisfy the implicit is
  usually bound too early (causing unexpected results in practice). Instead, the following is the new recommended
  approach:

  ```scala
  object MyBindings extends NewBindingModule( module => {       // note { no longer next to ( either - more readable
    module.bind [Z] toSingle new SomeClass
  }
  ```

  and if SomeClass needs a binding module, you should consider not using toSingle at all, try toModuleSingle instead
  which can have difference configurations in different modules (probably what you want):

  ```scala
  object MyBindings extends NewBindingModule( module => {
    module.bind [Z] toModuleSingle { implicit module => new SomeClass } // implicit here late binds the module
  }
  ```
  