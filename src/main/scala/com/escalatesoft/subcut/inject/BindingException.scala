package org.scala_tools.subcut.inject

/**
 * Created by IntelliJ IDEA.
 * User: dick
 * Date: 4/29/11
 * Time: 6:27 AM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Thrown if there is an issue with a binding, for example, no matching binding for a given Binding key.
 */
class BindingException(message: String) extends Exception(message)
