package org.scala_tools.subcut.inject

/**
 * Created by IntelliJ IDEA.
 * User: dick
 * Date: 4/29/11
 * Time: 6:29 AM
 * To change this template use File | Settings | File Templates.
 */

private[inject] class ClassInstanceProvider[I <: Any](val clazz: Class[Any]) {
  def newInstance[I]()(implicit m: scala.reflect.Manifest[I]): I = {
    try {
      clazz.newInstance.asInstanceOf[I]
    }
    catch {
      case ex: InstantiationException =>
        throw new InstantiationException(("Unable to create injected instance of %s, " +
          "did you provide a zero-arg constructor without implicit binding module?").
          format(clazz.getName))
    }
  }

  override def toString = "ClassInstanceProvider[%s]".format(clazz.getName)
}

private[inject] class LazyInstanceProvider[I <: Any](fn: () => I) {
  lazy val instance: I = fn()    // create an instance the first time we use it, and always use that

  override def toString = "LazyInstanceProvider[%s]".format(fn.toString)
}

private[inject] class NewInstanceProvider[I <: Any](fn: () => I) {
  def instance: I = fn()     // create a new instance each time we ask for one

  override def toString = "NewInstanceProvider[%s]".format(fn.toString)
}
