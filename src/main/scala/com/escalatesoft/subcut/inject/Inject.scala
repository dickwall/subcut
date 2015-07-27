package com.escalatesoft.subcut.inject

import scala.annotation.StaticAnnotation

/* An annotation, used by the compiler plugin, to automatically add the implicit bindingModule parameter and
 * the Injectable trait to any class decorated with the annotation.
 */
class Inject extends StaticAnnotation {
}