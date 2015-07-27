package com.escalatesoft.subcut

import scala.language.implicitConversions

package object inject {
  /**
   * Package definition to provide an injected value that indicates the default value for a constructor parameter
   * is to be injected. This is just a name for None and is typed to Option[Nothing] so it should be possible to
   * use it as a missing default for any constructor injected parameter. Should be used in conjunction with
   * Injectable.injectIfMissing
   */
  val injected: Option[Nothing] = None

  /**
   * Convenience method to create a new reflective instance provider for the given class. When bound, each
   * request for the bound item will create a new instance reflectively and return that new instance. Instances
   * must not take any constructor parameters other than (optionally) the bindingModule.
   * @param m implicit manifest of class to create instances of
   * @tparam T the class to create new instances of
   * @return a new class instance provider
   */
  def newInstanceOf[T <: Any](implicit m: scala.reflect.Manifest[T]) = {
    new ClassInstanceProvider[T](m.runtimeClass.asInstanceOf[Class[Any]])
  }

  /**
   * Convenience method to create a new reflective instance provider for the given class. When bound, each
   * request for the bound item will return the same instance for the current bindingModule. Calls with
   * new bindingModules will get a new instance which will also be a singleton within that bindingModule.
   * @param m implicit manifest of class to create instances of
   * @tparam T the class to create new instances of
   * @return a new class instance provider
   */
  def moduleInstanceOf[T <: Any](implicit m: scala.reflect.Manifest[T]) = {
    new ClassSingleModuleProvider[T](m.runtimeClass.asInstanceOf[Class[Any]])
  }

  /**
   * An implicit conversion from BindingId subclasses to String type, so that objects that extend BindingId
   * can be used as Binding identifiers both for binding and for injection.
   * @param bindingId a sub-object of BindingId
   * @return String short name of Binding object class
   */
  implicit def bindingIdToString(bindingId: BindingId): String = bindingId.bindingName

}

