package com.escalatesoft.subcut.inject

/**
 * Thrown if there is an issue with a binding, for example, no matching binding for a given Binding key.
 */
class BindingException(message: String) extends Exception(message)
