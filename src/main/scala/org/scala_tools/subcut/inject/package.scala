package org.scala_tools.subcut

/**
 * Created by IntelliJ IDEA.
 * User: Dick Wall
 * Date: 5/5/11
 * Time: 10:06 AM
 */

package object inject {
  /**
   * Package definition to provide an injected value that indicates the default value for a constructor parameter
   * is to be injected. This is just a name for None and is typed to Option[Nothing] so it should be possible to
   * use it as a missing default for any constructor injected parameter. Should be used in conjunction with
   * Injectable.injectIfMissing
   */
  val injected: Option[Nothing] = None
}

