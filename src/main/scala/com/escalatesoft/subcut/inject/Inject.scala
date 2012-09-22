package org.scala_tools.subcut.inject

import scala.annotation.StaticAnnotation

/**
 * Created by IntelliJ IDEA.
 * User: dick
 * Date: 7/25/11
 * Time: 2:40 PM
 * To change this template use File | Settings | File Templates.
 */

/* An annotation, used by the compiler plugin, to automatically add the implicit bindingModule parameter and
 * the Injectable trait to any class decorated with the annotation.
 */
class Inject extends StaticAnnotation {
}