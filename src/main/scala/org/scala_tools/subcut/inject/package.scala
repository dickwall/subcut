package org.scala_tools.subcut.inject

/**
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 5/5/11
 * Time: 10:06 AM
 */

package object inject {
  /**
   * Package definition to provide an injected value that indicates the default value for a constructor parameter
   * is to be injected. This is just a name for null and can introduce null pointer exceptions if you use it, so
   * use with care. Must be coupled with a injectIfMissing binding in the class initialization code, or an NPE is
   * highly likely to occur. injectIfBound is a safer and more idiomatic way to do optional injection binding and
   * will avoid any nastiness with nulls.
   */
  val injected: Null = null
}

