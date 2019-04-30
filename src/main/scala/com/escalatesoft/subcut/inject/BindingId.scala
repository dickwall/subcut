package com.escalatesoft.subcut.inject

/**
 * An abstract superclass for BindingId definitions as objects - used for easy, typesafe and code completion
 * compatible binding names.
 *
 * E.g.
 *
 * {{{
 *   object MainDB extends BindingId
 *   ...
 *
 *   bind [Db] idBy BindingId toSingle new SqlDB
 * }}}
 */
abstract class BindingId {
  lazy val bindingName = {
    val shortName = this.getClass.getSimpleName
    // strip the trailing $ for objects
    if (shortName.last == '$') shortName.init else shortName
  }
}

abstract class TypedBindingId[T] {
  lazy val bindingName = {
    val shortName = this.getClass.getSimpleName
    // strip the trailing $ for objects
    if (shortName.last == '$') shortName.init else shortName
  }
}
