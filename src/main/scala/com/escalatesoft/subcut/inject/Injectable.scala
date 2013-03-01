package com.escalatesoft.subcut.inject

/**
 * The trait that provides dependency injection features for a class or object. To use this trait,
 * Mix it in to the class or object definition, and then define the abstract bindingModule which holds
 * the bindings to be used. There are several ways to provide these binding modules: simple val override
 * in the class, mixing in a trait that defines the bindingModule (see BoundToModule), a constructor parameter
 * or, perhaps most flexibly, an implicit constructor parameter in a curried parameter list. This last option
 * can provide flexible and mostly invisible bindings all the way down an object instance creation chain.
 */
trait Injectable {
  implicit def bindingModule: BindingModule

  /**
   * Inject an instance for the given trait based on the class type required. If there is no binding, this
   * method will throw a BindingException. This form is for straight trait injection without an identifying name.
   * @return an instance configured by the binding module to use for the given trait.
   */
  def inject[T <: Any](implicit m: scala.reflect.Manifest[T]): T =
    bindingModule.inject[T](None)

  /**
   * Inject an instance for the given trait based on the class type required and an ID symbol. If there is no
   * matching binding, this method will throw a BindingException. The Symbol provided will be converted to a string
   * prior to the lookup, so the symbol is interchangeable with the string version of the same ID, in other words
   * 'maxPoolSize and "maxPoolSize" are considered equivalent by the lookup mechanism.
   * @param symbol the identifying name to look up for the binding, e.g. 'maxPoolSize
   * @return an instance configured by the binding module to use for the given trait and ID
   */
  def inject[T <: Any](symbol: Symbol)(implicit m: scala.reflect.Manifest[T]): T =
    bindingModule.inject[T](Some(symbol.name))

  /**
   * Inject an instance for the given trait based on the class type required and an ID string. If there is no
   * matching binding, this method will throw a BindingException. The string ID is interchangeable with the
   * symbol version of the same ID, in other words 'maxPoolSize and "maxPoolSize" are considered equivalent by the
   * lookup mechanism.
   * @param name the identifying name to look up for the binding, e.g. "maxPoolSize"
   * @return an instance configured by the binding module to use for the given trait and ID
   */
  def inject[T <: Any](name: String)(implicit m: scala.reflect.Manifest[T]): T =
    bindingModule.inject[T](Some(name))

  /**
   * Inject an instance for the given trait based on the class type only if there is no instance already provided.
   * If no instance is provided (i.e. the existing impl passed in is null) and no binding is available to match, a
   * BindingException will be thrown. If an existing impl is provided (not null), then the binding will not be
   * used and does not need to be present. This form of the inject does not need a provided ID symbol or string.
   * @param implToUse from the call site. If it is null, the binding provider will fill it in instead
   * @return an instance configured by the binding module to use for the given trait
   */
  def injectIfMissing[T <: Any](implToUse: Option[T])(implicit m: scala.reflect.Manifest[T]): T =
    if (implToUse != None) implToUse.get
    else inject[T]

  /**
   * Inject an instance for the given trait based on the class type only if there is no instance already provided.
   * If no instance is provided (i.e. the existing impl passed in is null) and no binding is available to match, a
   * BindingException will be thrown. If an existing impl is provided (not null), then the binding will not be
   * used and does not need to be present. This form of the inject takes a symbol ID to use to match the binding.
   * @param implToUse from the call site. If it is null, the binding provider will fill it in instead
   * @param name binding ID symbol to use - e.g. 'maxPoolSize
   * @return an instance configured by the binding module to use for the given trait
   */
  def injectIfMissing[T <: Any](implToUse: Option[T], name: String)(implicit m: scala.reflect.Manifest[T]): T =
    if (implToUse != None) implToUse.get
    else inject[T](name)

  /**
   * Inject an instance for the given trait based on the class type only if there is no instance already provided.
   * If no instance is provided (i.e. the existing impl passed in is null) and no binding is available to match, a
   * BindingException will be thrown. If an existing impl is provided (not null), then the binding will not be
   * used and does not need to be present. This form of the inject takes a string ID to use to match the binding.
   * @param implToUse from the call site. If it is null, the binding provider will fill it in instead
   * @param symbol binding ID symbol to use - e.g. 'maxPoolSize
   * @return an instance configured by the binding module to use for the given trait
   */
  def injectIfMissing[T <: Any](implToUse: Option[T], symbol: Symbol)(implicit m: scala.reflect.Manifest[T]): T =
    if (implToUse != None) implToUse.get
    else inject[T](symbol)

  /**
   * Inject an instance if a binding for that type is defined. If it is not defined, the function provided will
   * be used instead to create an instance to be used. This is arguably the most useful and efficient form of
   * injection usage, as the typical configuration can be provided at the call site and developers can easily
   * see what the "usual" instance is. An alternative binding will only be used if it is defined, e.g. for testing.
   * This form of the injector takes only a trait to match and no ID name.
   * @param fn a function to be used to return an instance, if there is no binding defined for the desired trait.
   * @return an instance that subclasses the trait, either from the binding definitions, or using the provided
   * function if no matching binding is defined.
   */
  def injectIfBound[T <: Any](fn: => T)(implicit m: scala.reflect.Manifest[T]): T = {
    bindingModule.injectOptional[T](None) match {
      case None => // must then have a valid impltouse
        val implToUse = fn
        if (implToUse == null)
          throw new IllegalStateException("No binding for %s, so provided impl function may not result in null".format(m.erasure.toString))
        implToUse
      case Some(instance) => instance
    }
  }

  /**
   * Inject an instance if a binding for that type is defined. If it is not defined, the function provided will
   * be used instead to create an instance to be used. This is arguably the most useful and efficient form of
   * injection usage, as the typical configuration can be provided at the call site and developers can easily
   * see what the "usual" instance is. An alternative binding will only be used if it is defined, e.g. for testing.
   * This form of the injector takes a symbol ID to use in the binding definition lookup, e.g. 'maxPoolSize.
   * @param name symbol ID to be used to identify the matching binding definition.
   * @param fn a function to be used to return an instance, if there is no binding defined for the desired trait.
   * @return an instance that subclasses the trait, either from the binding definitions, or using the provided
   * function if no matching binding is defined.
   */
  def injectIfBound[T <: Any](name: String)(fn: => T)(implicit m: scala.reflect.Manifest[T]): T = {
    bindingModule.injectOptional[T](Some(name)) match {
      case None => // must then have a valid impltouse
        val implToUse = fn
        if (implToUse == null)
          throw new IllegalStateException("No binding for %s named %s, so provided impl function may not result in null".format(m.erasure.toString, name))
        implToUse
      case Some(instance) => instance
    }
  }

  /**
   * Inject an instance if a binding for that type is defined. If it is not defined, the function provided will
   * be used instead to create an instance to be used. This is arguably the most useful and efficient form of
   * injection usage, as the typical configuration can be provided at the call site and developers can easily
   * see what the "usual" instance is. An alternative binding will only be used if it is defined, e.g. for testing.
   * This form of the injector takes a string ID to use in the binding definition lookup, e.g. "maxPoolSize".
   * @param symbol ID to be used to identify the matching binding definition.
   * @param fn a function to be used to return an instance, if there is no binding defined for the desired trait.
   * @return an instance that subclasses the trait, either from the binding definitions, or using the provided
   * function if no matching binding is defined.
   */
  def injectIfBound[T <: Any](symbol: Symbol)(fn: => T)(implicit m: scala.reflect.Manifest[T]): T =
    injectIfBound(symbol.name)(fn)


  /**
   * Inject an optional instance for the given trait based on the class type required. If there is no binding, this
   * method will return None. This form is for straight trait injection without an identifying name.
   * @return an optional instance configured by the binding module to use for the given trait.
   */
  def injectOptional[T <: Any](implicit m: scala.reflect.Manifest[T]): Option[T] =
    bindingModule.injectOptional[T](None)

  /**
   * Inject an optional instance for the given trait based on the class type required and an ID symbol. If there is no
   * matching binding, this method will return None. The Symbol provided will be converted to a string
   * prior to the lookup, so the symbol is interchangeable with the string version of the same ID, in other words
   * 'maxPoolSize and "maxPoolSize" are considered equivalent by the lookup mechanism.
   * @param symbol the identifying name to look up for the binding, e.g. 'maxPoolSize
   * @return an optional instance configured by the binding module to use for the given trait and ID
   */
  def injectOptional[T <: Any](symbol: Symbol)(implicit m: scala.reflect.Manifest[T]): Option[T] =
    bindingModule.injectOptional[T](Some(symbol.name))

  /**
   * Inject an optional instance for the given trait based on the class type required and an ID string. If there is no
   * matching binding, this method will return None. The string ID is interchangeable with the
   * symbol version of the same ID, in other words 'maxPoolSize and "maxPoolSize" are considered equivalent by the
   * lookup mechanism.
   * @param name the identifying name to look up for the binding, e.g. "maxPoolSize"
   * @return an optional instance configured by the binding module to use for the given trait and ID
   */
  def injectOptional[T <: Any](name: String)(implicit m: scala.reflect.Manifest[T]): Option[T] =
    bindingModule.injectOptional[T](Some(name))

}

/**
 * AutoInjectable is identical to Injectable except that if you are using the compiler plugin, the implicit
 * bindingModule parameter will be automatically filled in for you. It is provided as a convenient boilerplate buster
 * but is distinct from Injectable because sometimes you want to mark something as Injectable but keep the injection
 * abstract until later.
 */
trait AutoInjectable extends Injectable

/**
 * A trait that can be used to provide the cake-like ability to mix in a trait upon instance creation
 * rather than using the implicit parameter. This is less flexible but may still be desired by some
 * developers. To use it, simply create a new trait that extends this one, and provide the definition of
 * bindingModule to point to a suitable BindingModule. You can then mix this trait in to any other class or
 * new composition to provide the injector bindings.
 */
trait BoundToModule {
  val bindingModule: BindingModule
}
