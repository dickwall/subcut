package com.escalatesoft.subcut.inject

/**
 * Created by IntelliJ IDEA.
 * User: dick
 * Date: 4/29/11
 * Time: 6:29 AM
 * To change this template use File | Settings | File Templates.
 */

import scala.collection._

private[inject] class ClassInstanceProvider[I <: Any](val clazz: Class[Any]) {
  def newInstance[I](module: BindingModule)(implicit m: scala.reflect.Manifest[I]): I = {
    try {
      clazz.getConstructor(classOf[BindingModule]).newInstance(module).asInstanceOf[I]
    }
    catch {
      case _ : NoSuchMethodException | _ : InstantiationException =>
        try {
          clazz.newInstance.asInstanceOf[I]
        }
        catch {
          case _ : InstantiationException =>
            throw new InstantiationException(("Unable to create injected instance of %s, " +
              "did you provide a zero-arg constructor either with or without implicit binding module?").
              format(clazz.getName))
        }
    }
  }

  override def toString = "ClassInstanceProvider[%s]".format(clazz.getName)
}

private[inject] class ClassSingleModuleProvider[I <: Any](clazz: Class[Any]) extends ClassInstanceProvider[I](clazz) {
  private[this] val instanceMap = new mutable.HashMap[BindingModule, Any] with mutable.SynchronizedMap[BindingModule, Any]

  override def newInstance[I : Manifest](module: BindingModule): I =
    instanceMap.getOrElseUpdate(module, super.newInstance[I](module)).asInstanceOf[I]
}

private[inject] class LazyInstanceProvider[I <: Any](fn: () => I) {
  lazy val instance: I = fn()    // create an instance the first time we use it, and always use that

  override def toString = "LazyInstanceProvider[" + instance.toString + "]"
}

private[inject] class LazyModuleInstanceProvider[I <: Any](module: BindingModule, fn: BindingModule => I) {
  lazy val instance: I = fn(module)

  override def toString = "LazyModuleInstanceProvider[" + instance.toString + "]"

  private[inject] def copyAndReset(newModule: BindingModule) =
    new LazyModuleInstanceProvider(newModule, fn)
}

private[inject] class NewInstanceProvider[I <: Any](fn: () => I)(implicit m: Manifest[I]) {
  def instance: I = fn()     // create a new instance each time we ask for one
  val boundType = m.runtimeClass.getName

  override def toString = "NewInstanceProvider[%s]".format(boundType)
}

private[inject] class NewBoundInstanceProvider[I <: Any](fn: BindingModule => I)(implicit m: Manifest[I]) {
  def instance(module: BindingModule): I = fn(module)   // create new instance with binding module provided
  val boundType = m.runtimeClass.getName

  override def toString = "NewBoundInstanceProvider[%s]".format(boundType)
}
