package org.scala_tools.subcut.inject

/*
 * Created by IntelliJ IDEA.
 * User: dick
 * Date: 2/17/11
 * Time: 11:39 AM
 */
import scala.collection._

/**
 * The binding key, used to uniquely identify the desired injection using the class and an optional name.
 */
private[inject] case class BindingKey[A](m: Manifest[A], name: Option[String])

/**
 * The main BindingModule trait.
 * Not intended to be used directly, instead extend NewBindingModule with a function to create the bindings
 * (recommended - the result will be immutable) or a MutableBindingModule (not recommended unless you know what
 * you are doing and take on the thread safety responsibility yourself).
 */
trait BindingModule { outer =>

  /** Abstract binding map definition */
  def bindings: immutable.Map[BindingKey[_], Any]
  
  /**
   * Merge this module with another. The resulting module will include all bindings from both modules, with this
   * module winning if there are common bindings (binding override).
   * @param other another BindingModule to cons with this one. Any duplicates will favor the bindings from this
   * rather than other.
   */
  def ~(other: BindingModule): BindingModule = {
    new BindingModule {
      override val bindings = other.bindings ++ outer.bindings
    }
  }

  /**
   * Provide a mutable copy of these bindings to a passed in function so that it can override the bindings
   * for just the scope of that function. Useful for testing.
   * @param fn a function that takes the new mutable binding copy and may use it within scope. Can
   * return any type, and the any return from the function will be returned from this function.
   * @return the value returned from the provided function.
   */
  def modifyBindings[A](fn: MutableBindingModule => A): A = {
    val mutableBindings = new MutableBindingModule {
      bindings = outer.bindings
    }
    fn(mutableBindings)
  }

  /**
   * Inject for a given class with optional name. Whatever is bound to the class will be provided.
   * If the name is given, only a matching class/name pair will be used, and it will not fall back
   * to just providing an implementation based only on the class.
   * @param name an optional name to use for the binding match
   * @return the instance the binding was configured to return
   */
  def inject[T <: Any : Manifest](name: Option[String]): T = {
    val key = BindingKey(manifest, name)
    injectOptional[T](key) match {
      case None => throw new BindingException("No binding for key " + key)
      case Some(instance) => instance
    }
  }

  /**
   * Retrieve an optional binding for class T with the optional name provided. If no
   * binding is available with the given class and optional name, a None will be returned,
   * otherwise the binding will be evaluated and an instance of a subclass of T will be
   * returned.
   * @param clazz the class to match a binding for
   * @param name Option[String] name, if present will be matched, if None only class will
   * be used for lookup (note, this also means any named bindings of the same type will
   * not be matched)
   * @return Option[T] containing either an instance subtype of T, or None if no matching
   * binding is found.
   */
  def injectOptional[T <: Any : Manifest](name: Option[String]): Option[T] =
    injectOptional(BindingKey(manifest, name))


  /**
   * Retrieve an optional binding for class T with the given BindingKey, if no
   * binding is available for the binding key, a None will be returned,
   * otherwise the binding will be evaluated and an instance of a subclass of T will be
   * returned.
   * @param key a BindingKey to use for the lookup
   * @return Option[T] containing either an instance subtype of T, or None if no matching
   * binding is found.
   */
  def injectOptional[T](key: BindingKey[T]): Option[T] = {
    // common sense check - many binding maps are empty, we can short circuit all lookup if it is
    // and just return None
    if (bindings.isEmpty) None else {
      val bindingOption = bindings.get(key)
      if (bindingOption == None) None
      else
        bindingOption.get match {
          case ip: ClassInstanceProvider[T] => Some(ip.newInstance())
          case lip: LazyInstanceProvider[T] => Some(lip.instance)
          case nip: NewInstanceProvider[T] => Some(nip.instance)
          case i: T => Some(i)
          case _ => throw new BindingException("Illegal binding for key " + key)
        }
    }
  }
}

/**
 * A class to create a new, immutable, binding module. In order to work, the constructor of this class
 * takes a function to evaluate, and passes this on to a bindings method which can be used to resolve
 * the bindingModule using the function on demand. The binding module loaned to the passed in function
 * is mutable, allowing the convenient DSL to be used, then the bindings are copied to an immutable
 * class upon exit of the bindings evaluation.
 * <p/>
 * To use this class:
 * <p/>
 * <pre>
 * class ProductionBindings extends NewBindingModule({ implicit module =>
 *    import module._   // for convenience
 *    bind [DBLookup] toSingle new MySQLLookup
 *    bind [WebService].toClass[RealWebService]
 *    bind [Int] identifiedBy 'maxPoolSize toSingle 10   // could also use idBy instead of identifiedBy
 *    bind [QueryService] toSingle { new SlowInitQueryService }
 * })
 * </pre>
 * @param fn a function that takes a mutable binding module and initializes it with whatever bindings
 * you want. The module will be frozen after creation of the bindings, but is mutable for the
 * time you are defining it with the DSL.
 */
class NewBindingModule(fn: MutableBindingModule => Unit) extends BindingModule {
  lazy val bindings = {
    val module = new Object with MutableBindingModule
    fn(module)
    module.freeze().fixed.bindings
  }
}

/**
 * A mutable binding module. This module is used during construction of bindings configuration
 * with the DSL, but may also be used as a mutable binding module in its own right. Anyone wishing
 * to use the mutable binding module to inject a real system should be aware that they take on all
 * responsibility for threading issues with such usage. For example, tests running in parallel could
 * reconfigure the same binding and cause a race condition which will cause the tests to fail.
 * As such, direct usage of the mutable binding module, particularly in a production environment, is
 * strongly discouraged. Use NewBindingModule instead to ensure immutability and thread safety.
 * <p/>
 * The MutableBindingModule is also provided on a per-function usage by the modifyBindings method on
 * BindingModule. This is the recommended way to have rebindings on a test-by-test basis and is
 * thread safe, as each test gets a new copy of the binding module and will not interfere with others.
 * <p/>
 * An example usage will look like this:
 * <pre>
 * class SomeBindings extends NewBindingModule ({ implicit module =>
 *   import module._
 *   bind [Trait1] toSingle new Class1Impl
 * })
 *
 * // in a test...
 * SomeBindings.modifyBindings { testBindings =>  // holds mutable copy of SomeBindings
 *   module.bind[Trait1] toSingle mockClass1Impl  // where the mock has been set up already
 *   // run tests using the mockClass1Impl
 * }  // coming out of scope destroys the temporary mutable binding module
 * </pre>
 */
trait MutableBindingModule extends BindingModule { outer =>

  @volatile private[this] var _bindings = immutable.Map.empty[BindingKey[_], Any]
  @volatile private[this] var _frozen = false

  private[inject] def bindings_=(newBindings: BindingModule) {
    ensureNotFrozen()
    this._bindings = newBindings.bindings
  }
  private[inject] def bindings_=(newBindings: immutable.Map[BindingKey[_], Any]) {
    ensureNotFrozen()
    this._bindings = newBindings
  }
  
  def bindings = this._bindings

  /**
   * return an immutable copy of these bindings by creating a new binding module
   * with the bindings taken as an immutable snapshot of the current bindings in
   * this module.
   * @return a new immutable BindingModule
   */
  def fixed(): BindingModule = {
    new BindingModule {
      override val bindings = outer._bindings
    }
  }

  /**
   * freeze the current state of this mutable binding module so that it may not be
   * changed further. This is done by checking the frozen property in the bindings
   * property modifier and is not as safe as using fixed() to obtain a completely
   * immutable copy of the bindings configuration, so fixed() is recommended. However
   * there may be times this approach is preferable. Calling freeze() on a mutable
   * binding module cannot be reversed.
   */
  def freeze() = { this._frozen = true; this }

  /**
   * return whether the current state of these bindings is frozen.
   */
  def frozen: Boolean = _frozen

  def ensureNotFrozen() {
    if (_frozen) throw new BindingException("Module is frozen, no further bindings allowed")
  }

  private def bindingKey[T](m: Manifest[T], name: Option[String]) =
    BindingKey(m, name)

  /**
   * Merge in bindings from another binding module, replacing any conflicts with the new bindings from the
   * other module supplied. May be used to bulk-apply some test configuration onto a mutable copy of the
   * regular bindings.
   * @param other A BindingModules with bindings to merge and/or replace the bindings in this module.
   */
  def mergeWithReplace(other: BindingModule) = {
    this.bindings = this.bindings ++ other.bindings
  }

  /**
   * Convenience form of merge with replace, can be used like this:
   *
   * <pre>
   * class SomeBindings extends NewBindingModule ({ module =>
   *   module <~ OtherModule1   // include all bindings from Module1
   *   module <~ OtherModule2   // include all bindings from Module2, overwriting as necessary
   *   module.bind[Trait1] toSingle new Class1Impl
   * })
   * </pre>
   */
  def <~(other: BindingModule) = mergeWithReplace(other)

  /**
   * Replace the current bindings configuration module completely with the bindings from the other module
   * supplied. This will effectively unbind anything currently bound that is not bound in the new module.
   * @param other the other binding module with which to replace the current bindings.
   */
  def replaceBindings(other: BindingModule) {
    this.bindings = other.bindings
  }

  /**
   * A convenient way to combine multiple binding modules into one module. Just use withBindingModules and
   * supply a repeated parameter list of BindingModules to merge. The order for conflict resolution is
   * last in wins, so if you have <code>withBindingModule(ModuleA, ModuleB)</code> and both ModuleA and ModuleB
   * bind the same class (and optional name), ModuleB will win.
   */
  def withBindingModules(modules: BindingModule*) {
    if (!this.bindings.isEmpty) throw new BindingException("withBindingModules may only be used on an empty module for initialization")
    for (module <- modules) mergeWithReplace(module)
  }
  
  private def bindInstance[T <: Any](instance: T)(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, None)
    bindings += key -> instance
  }

  private def bindLazyInstance[T <: Any](func: () => T)(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, None)
    bindings += key -> new LazyInstanceProvider(func)
  }

  private def bindProvider[T <: Any](func: () => T)(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, None)
    bindings += key -> new NewInstanceProvider(func)
  }

  private def bindInstance[T <: Any](name: String, instance: T)(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, Some(name))
    bindings += key -> instance
  }

  private def bindLazyInstance[T <: Any](name: String, func: () => T)(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, Some(name))
    bindings += key -> new LazyInstanceProvider(func)
  }

  private def bindProvider[T <: Any](name: String, func: () => T)(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, Some(name))
    bindings += key -> new NewInstanceProvider(func)
  }

  private def bindClass[T <: Any](instanceProvider: ClassInstanceProvider[T])(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, None)
    bindings += key -> instanceProvider
  }

  private def bindClass[T <: Any](name: String, instanceProvider: ClassInstanceProvider[T])(implicit m: scala.reflect.Manifest[T]) {
    val key = bindingKey(m, Some(name))
    bindings += key -> instanceProvider
  }

  /**
   * Unbind a given trait (without name) from the list of bindings.
   */
  def unbind[T <: Any]()(implicit m: scala.reflect.Manifest[T]): Option[T] = {
    val key = bindingKey(m, None)
    val existing = bindings.get(key)
    bindings -= key
    existing.asInstanceOf[Option[T]]
  }

  /**
   * Unbind a given trait with the provided name from the list of bindings.
   * @param name a String name that together with the trait type, identifies the binding to remove.
   */
  def unbind[T <: Any](name: String)(implicit m: scala.reflect.Manifest[T]): Option[T] = {
    val key = bindingKey(m, Some(name))
    val existing = bindings.get(key)
    bindings -= key
    existing.asInstanceOf[Option[T]]
  }

  /**
   * Unbind a given trait with the provided symbol from the list of bindings.
   * @param symbol A symbol that together with the trait type, identifies the binding to remove.
   */
  def unbind[T <: Any](symbol: Symbol)(implicit m: scala.reflect.Manifest[T]): Option[T] = unbind[T](symbol.name)

  /**
   * A convenient way to list the current bindings in the binding module. Useful for debugging purposes.
   * Prints to standard out.
   */
  def showMap() {
    println(mapString.mkString("\n"))
  }

  /**
   * A convenient way to obtain a string representation of the current bindings in this module.
   */
  def mapString = {
    for ((k, v) <- bindings) yield { k.toString + " -> " + v.toString }
  }

  /**
   * Temporarily push the bindings (as though on a stack) and let the current bindings be overridden for
   * the scope of a provided by-name function. The binding changes will be popped after execution of the
   * function, restoring the state of the bindings prior to the push.
   * @param fn by-name function that can use and modify the bindings for this module without altering the original.
   */
  def pushBindings[A](fn: => A): A = {
    val currentBindings = bindings
    try {
      fn
    }
    finally {
      bindings = currentBindings
    }
  }

  // an inner builder class to give us a nice little DSL for doing the bindings
  /**
   * Inner builder class providing a convenient DSL for configuring the bindings in this mutable binding module.
   */
  class Bind[T <: Any](implicit m: scala.reflect.Manifest[T]) {
    var name: Option[String] = None

    /**
     * Bind to a provider of type I where I is any subtype of T. The provider is a by-name function that returns
     * an instance of type I, and may perform any necessary operation in order to provide I, for example if
     * the current web session is to be injected, the provider may use whatever mechanism is required to obtain
     * the correct current web session for the current user, etc. and provide that back to the injection site.
     * The function will be evaluated for each injection of the matching binding and may return a unique instance
     * each time, or the same one, or anything in between.
     * @param function a by-name function returning type I where I is a subtype of the bound type T.
     */
    def toProvider[I <: T](function: => I) {
      name match {
        case Some(n) => outer.bindProvider[T](n, function _)
        case None => outer.bindProvider[T](function _)
      }
      name = None
    }

    /**
     * Bind to a single instance of I where I is a subtype of the bound type T. The single instance will not be
     * decided until the first time the matching binding is injected, but from then on will always be the same
     * instance. This is to provide a way to cope with injection of items that may not have been configured at
     * the time of application startup, but will be configured before the first usage. It can also be used for
     * object with a slow initialization or ones that may never be used in a run. Since the same instance is
     * always provided after the first evaluation, care should be taken that the threading capabilities of the
     * object bound should match the execution environment, in other words, thread safety of the returned instance
     * is your responsibility.
     * @param function a by-name function that is evaluated on the first injection of this binding, and after that
     * will always return the same instance I, where I is any subtype of T.
     */
    def toSingle[I <: T](function: => I) = {
      name match {
        case Some(n) => outer.bindLazyInstance[T](n, function _)
        case None => outer.bindLazyInstance[T](function _)
      }
    }

    /**
     * Synonym for #toSingle, provided for consistency
     */
    def toSingleInstance[I <: T](function: => I) = toSingle[I](function)

    /**
     * A convenient operator to bind to an instance of None (in this definition). Can be used instead of
     * unbind. For example <code>module.bind [Int] idBy 'timeLimit to None</code>
     * @param must be None (for this form of the method).
     */
    def to(none: None.type) = {
      name match {
        case Some(n) => outer.unbind[T](n)
        case None => outer.unbind[T]()
      }
      name = None
    }

    /**
     * Bind to a class instance provider of class. Intended to be used with instanceOfClass like this:
     * <code>module.bind [DBLookup] to instanceOfClass [MySQLDBLookup]</code>. Will provide a new
     * instance of the class configured for each injection site. Any instance provided in this way
     * must provide a zero parameter default constructor since reflection is used to create the instance
     * and it will fail if there is no default constructor. Note that this is true even for implicit
     * parameters, so you cannot use this form if you wish to provide the implicit binding chain to the
     * target instance. Use a toProvider instead.
     * @param instOfClass the class instance provider to use for the binding. Use instanceOfClass method
     * to conveniently obtain the right thing.
     */
    def to(instOfClass: ClassInstanceProvider[T]) = {
      name match {
        case Some(n) => outer.bindClass[T](n, instOfClass)
        case None => outer.bindClass[T](instOfClass)
      }
      name = None
    }

    /**
     * Create a class instance provider for the given class parameter. Intended for use with to(instOfClass).
     * Note that this will only work to provide instances of classes that have a zero arg default constructor.
     * reflection is used to create the class instance.
     */
    def instanceOfClass[I <: T](implicit m: scala.reflect.Manifest[I], t: scala.reflect.Manifest[T]) = {
      new ClassInstanceProvider[T](m.erasure.asInstanceOf[Class[Any]])
    }

    /**
     * Bind to a new instance of the provided class for each injection. The class provided, I, must be
     * a subtype of the binding class T. Because this form takes no parameters other than the type parameter
     * it can screw up the semicolon inference in Scala if not used with the explicit . form, e.g.
     * <code>module.bind[DBLookup].toClass[MySQLDBLookup]</code> is safe, but
     * <code>module.bind[DBLookup] toClass[MySQLDBLookup]</code> can cause issues with semicolon inference.
     * Note that the provided type I must provide a zero args default constructor for this binding to work.
     * It uses reflection to instantiate the class and will fail at injection time if no such default constructor is
     * available.
     */
    def toClass[I <: T](implicit m: scala.reflect.Manifest[I], t: scala.reflect.Manifest[T]) = {
      name match {
        case Some(n) => outer.bindClass[T](n, new ClassInstanceProvider[T](m.erasure.asInstanceOf[Class[Any]]))
        case None => outer.bindClass[T](new ClassInstanceProvider[T](m.erasure.asInstanceOf[Class[Any]]))
      }
      name = None
    }

    /**
     * Part of the fluent interface in the DSL, identified by provides a name to attach to the binding key so that,
     * in combination with the trait type being bound, a unique key is formed. This form takes a string name, but
     * there is an overloaded version that allows a symbol to be used instead. The symbol and string names are
     * interchangeable, i.e. 'maxPoolSize and "maxPoolSize" are equivalent both in definition and in usage.
     * <p/>
     * Typical usage:
     * <code>module.bind [Int] identifiedBy "maxPoolSize" toSingle 30</code>
     * @param n the string name to identify this binding when used in combination with the type parameter.
     */
    def identifiedBy(n: String) = {
      this.name = Some(n)
      this
    }

    /**
     * Part of the fluent interface in the DSL, identified by provides a name to attach to the binding key so that,
     * in combination with the trait type being bound, a unique key is formed. This form takes a symbol name, but
     * there is an overloaded version that allows a string to be used instead. The symbol and string names are
     * interchangeable, i.e. 'maxPoolSize and "maxPoolSize" are equivalent both in definition and in usage.
     * <p/>
     * Typical usage:
     * <code>module.bind [Int] identifiedBy 'maxPoolSize toSingle 30</code>
     * @param symbol the symbol name to identify this binding when used in combination with the type parameter.
     */
    def identifiedBy(symbol: Symbol) = {
      this.name = Some(symbol.name)
      this
    }

    def idBy(symbol: Symbol) = identifiedBy(symbol)
    def idBy(name: String) = identifiedBy(name)
  }

  // and a parameterized bind method to kick it all off
  def bind[T <: Any](implicit m: scala.reflect.Manifest[T]) = new Bind[T]()
}
