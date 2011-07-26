package org.scala_tools.subcut.inject

import tools.nsc.transform.Transform
import tools.nsc.plugins.{Plugin, PluginComponent}
import tools.nsc.Global

/**
 * Created by IntelliJ IDEA.
 * User: dick
 * Date: 7/25/11
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */

class AnnotationsInjectPlugin(val global: Global) extends Plugin {
  val name = "annotations-inject-implicit"
  val description = "generates code which adds an implicit parameter of type BindingModule, and Injectable trait, on @Inject annotation"
  val components = List[PluginComponent](AnnotationsInjectComponent)

  private object AnnotationsInjectComponent extends PluginComponent with Transform {
    val global: AnnotationsInjectPlugin.this.global.type = AnnotationsInjectPlugin.this.global
    // val runsAfter = "parser"
    //Using the Scala Compiler 2.8.x the runsAfter should be written as below
    val runsAfter = List[String]("parser")
    val phaseName = AnnotationsInjectPlugin.this.name

    def newTransformer(unit: global.CompilationUnit) = AnnotationsInjectTransformer

    object AnnotationsInjectTransformer extends global.Transformer {
      override def transform(tree: global.Tree): global.Tree = {
        import global._
        import global.definitions._
        tree match {
          case cd @ ClassDef(modifiers, name, tparams, classBody) => {
            // TODO: This should not be a string check in the production version
            val injectPresent = cd.mods.annotations.toString.contains("new Inject()")
            if (injectPresent) {
              warning("@Injecting the class %s".format(name))
              warning("Old class def")
              warning(cd.toString)
              val newInj = Ident("org.scala_tools.subcut.inject.Injectable")
              val newParents = newInj :: classBody.parents

              val body = classBody.body.map {
                case item @ DefDef(modifiers, termname, tparams, vparamss, tpt, rhs) =>
                  if (termname.toString == "<init>") {
                    val newMods = Modifiers(536879616L)
                    val newImplicit = new ValDef(newMods, "bindingModule", Ident("org.scala_tools.subcut.inject.BindingModule"), EmptyTree)
                    val newParams = vparamss ::: List(List(newImplicit))
                    super.transform(DefDef(modifiers, termname, tparams, newParams, tpt, rhs))
                  }
                  else { super.transform(item) }
                case t => super.transform(t)
              }
              val newClassDef = ClassDef(modifiers, name, tparams, Template(newParents, classBody.self, body))
              warning("New class def")
              warning(newClassDef.toString)
              super.transform(newClassDef)
            }
            super.transform(cd)
          }
          case t => super.transform(t)
        }
      }
    }
  }
}