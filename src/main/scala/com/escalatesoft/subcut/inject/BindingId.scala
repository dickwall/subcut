package com.escalatesoft.subcut.inject

/**
 * Created with IntelliJ IDEA.
 * User: dick
 * Date: 1/1/13
 * Time: 2:05 PM
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
