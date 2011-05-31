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
private[inject] case class BindingKey(val clazz: Class[Any], val name: Option[String])

/**
 * The main BindingModule trait.
 * Not intended to be used directly, instead extend NewBindingModule with a function to create the bindings
 * (recommended - the result will be immutable) or a MutableBindingModule (not recommended unless you know what
 * you are doing and take on the thread safety responsibility yourself).
 */
trait BindingModule { outer =>

	/** Abstract binding map definition */
  def bindings: immutable.Map[BindingKey, Any]
  
	/**
	 * Cons this module with another. The resulting module will include all bindings from both modules, with this
	 * module winning if there are common bindings (binding override).
	 */
  def ::(other: BindingModule): BindingModule = {
    new BindingModule {
      override val bindings = outer.bindings ++ other.bindings
    }
  }

  /**
	 * Provide a mutable copy of these bindings to a passed in function so that it can override the bindings
	 * for just the scope of that function. Useful for testing.
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
   * @param clazz the class to match for the binding match
   * @param name an optional name to use for the binding match
   * @returns the instance the binding was configured to return
   */
  def inject[T <: Any](clazz: Class[T], name: Option[String]): T = {
    val key = BindingKey(clazz.asInstanceOf[Class[Any]], name)
    injectOptional[T](key) match {
      case None => throw new BindingException("No binding for key " + key)
      case Some(instance) => instance
    }
  }

  def injectOptional[T <: Any](clazz: Class[T], name: Option[String]): Option[T] =
    injectOptional(BindingKey(clazz.asInstanceOf[Class[Any]], name))

  def injectOptional[T <: Any](key: BindingKey): Option[T] = {
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

class NewBindingModule(fn: MutableBindingModule => Unit) extends BindingModule {
  def bindings = {
    val module = new Object with MutableBindingModule
    fn(module)
    module.freeze().fixed.bindings
  }
}


trait MutableBindingModule extends BindingModule { outer =>

  @volatile private[this] var _bindings = immutable.Map.empty[BindingKey, Any]
  @volatile private[this] var _frozen = false

  private[inject] def bindings_=(newBindings: BindingModule) {
    ensureNotFrozen()
    this._bindings = newBindings.bindings
  }
  private[inject] def bindings_=(newBindings: immutable.Map[BindingKey, Any]) {
    ensureNotFrozen()
    this._bindings = newBindings
  }
  
  def bindings = this._bindings

  def fixed(): BindingModule = {
    new BindingModule {
      override val bindings = outer._bindings
    }
  }

  def freeze() = { this._frozen = true; this }
  def frozen: Boolean = _frozen

  def ensureNotFrozen() = {
    if (_frozen) throw new BindingException("Module is frozen, no further bindings allowed")
  }

  private def bindingKey[T](m: Manifest[T], name: Option[String]) =
    BindingKey(m.erasure.asInstanceOf[Class[Any]], name)

  def mergeWithReplace(other: BindingModule) = {
    this.bindings = this.bindings ++ other.bindings
  }

  def replaceBindings(other: BindingModule) = {
    this.bindings = other.bindings
  }

  def withBindingModules(modules: BindingModule*) = {
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

  def unbind[T <: Any]()(implicit m: scala.reflect.Manifest[T]): Option[T] = {
    val key = bindingKey(m, None)
    val existing = bindings.get(key)
    bindings -= key
    existing.asInstanceOf[Option[T]]
  }

  def unbind[T <: Any](name: String)(implicit m: scala.reflect.Manifest[T]): Option[T] = {
    val key = bindingKey(m, Some(name))
    val existing = bindings.get(key)
    bindings -= key
    existing.asInstanceOf[Option[T]]
  }

  def unbind[T <: Any](symbol: Symbol)(implicit m: scala.reflect.Manifest[T]): Option[T] = unbind[T](symbol.name)

  def showMap() = {
    println(mapString.mkString("\n"))
  }

  def mapString = {
    for ((k, v) <- bindings) yield { k.toString + " -> " + v.toString }
  }

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
  class Bind[T <: Any](implicit m: scala.reflect.Manifest[T]) {
    var name: Option[String] = None

    def toInstance[I <: T](instance: I) = {
      name match {
        case Some(n) => outer.bindInstance[T](n, instance)
        case None => outer.bindInstance[T](instance)
      }
      name = None
    }

    def toProvider[I <: T](function: => I) = {
      name match {
        case Some(n) => outer.bindProvider[T](n, function _)
        case None => outer.bindProvider[T](function _)
      }
      name = None
    }

    def toLazyInstance[I <: T](function: => I) = {
      name match {
        case Some(n) => outer.bindLazyInstance[T](n, function _)
        case None => outer.bindLazyInstance[T](function _)
      }
    }

    def to(none: None.type) = {
      name match {
        case Some(n) => outer.unbind[T](n)
        case None => outer.unbind[T]()
      }
      name = None
    }

    def to(instOfClass: ClassInstanceProvider[T]) = {
      name match {
        case Some(n) => outer.bindClass[T](n, instanceOfClass)
        case None => outer.bindClass[T](instanceOfClass)
      }
      name = None
    }

    def instanceOfClass[I <: T](implicit m: scala.reflect.Manifest[I], t: scala.reflect.Manifest[T]) = {
      new ClassInstanceProvider[T](m.erasure.asInstanceOf[Class[Any]])
    }

    def toClass[I <: T](implicit m: scala.reflect.Manifest[I], t: scala.reflect.Manifest[T]) = {
      name match {
        case Some(n) => outer.bindClass[T](n, new ClassInstanceProvider[T](m.erasure.asInstanceOf[Class[Any]]))
        case None => outer.bindClass[T](new ClassInstanceProvider[T](m.erasure.asInstanceOf[Class[Any]]))
      }
      name = None
    }

    def identifiedBy(n: String) = {
      this.name = Some(n)
      this
    }

    def identifiedBy(symbol: Symbol) = {
      this.name = Some(symbol.name)
      this
    }
  }

  // and a parameterized bind method to kick it all off
  def bind[T <: Any](implicit m: scala.reflect.Manifest[T]) = new Bind[T]()
}
