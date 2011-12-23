package org.scala_tools.subcut.inject

import java.lang.reflect.Modifier

trait InjectableEx extends Injectable {self =>
  override def inject[T <: Any](implicit m: scala.reflect.Manifest[T]): T = {
    try {
      super.inject(m)
    } catch {
      case _ex: BindingException =>
        val clazz = m.erasure.asInstanceOf[Class[T]]
        if (clazz.isMemberClass && !Modifier.isStatic(clazz.getModifiers)) {
          if (classOf[Injectable].isAssignableFrom(clazz)) {
            try {
              clazz.getConstructor(self.getClass, classOf[BindingModule]).newInstance(self, bindingModule)
            } catch {
              case _ => clazz.getConstructor(self.getClass).newInstance(self)
            }
          } else {
            clazz.getConstructor(self.getClass).newInstance(self)
          }
        } else {
          if (classOf[Injectable].isAssignableFrom(clazz)) {
            try {
              clazz.getConstructor(classOf[BindingModule]).newInstance(bindingModule)
            } catch {
              case _ => clazz.newInstance()
            }
          } else {
            clazz.newInstance()
          }
        }
    }
  }
}