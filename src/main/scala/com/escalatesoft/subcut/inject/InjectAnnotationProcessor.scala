package com.escalatesoft.subcut.inject

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.symtab.Flags
import scala.tools.nsc.transform.{InfoTransform, Transform, TypingTransformers}

/**
 * A compiler plugin to automatically add the implicit binding module to any class
 * that extends the trait AutoInjectable
 */

class AnnotationsInjectPlugin(val global: Global) extends Plugin {
  val name = "annotations-inject-implicit"
  val description = "generates code which adds an implicit parameter of type BindingModule when an AutoInjectable trait is mixed in"
  val components = List[PluginComponent](AnnotationsInjectComponent)

  private object AnnotationsInjectComponent extends PluginComponent with Transform with TypingTransformers {
    val global: AnnotationsInjectPlugin.this.global.type = AnnotationsInjectPlugin.this.global
    import global._

    //Using the Scala Compiler 2.8.x the runsAfter should be written as below
    // val runsAfter = "parser"
    val runsAfter = List[String]("parser")
    override val runsBefore = List[String]("namer")
    val phaseName = AnnotationsInjectPlugin.this.name

    def newTransformer(unit: CompilationUnit) = new AnnotationsInjectTransformer (unit)

    val autoInjectable = "AutoInjectable"
    val bindingModule = "bindingModule"
    val constructorMethod = "<init>"

    val bindingModuleType =
      Select(
        Select(
          Select(
            Select(
              Select(
                Ident(
                  newTermName("_root_")),
                newTermName("com")),
              newTermName("escalatesoft")),
            newTermName("subcut")),
          newTermName("inject")),
        newTypeName("BindingModule"))

    class AnnotationsInjectTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      def preTransform(tree: Tree): Tree = {

        import Flags._

        tree match {
          case cd @ ClassDef(modifiers, name, tparams, classBody) => {
            val injectPresent = classBody.parents.map(_.toString).contains(autoInjectable)
            if (injectPresent) {
              log("AutoInjecting class %s".format(name))
              val newParents = classBody.parents

              val body = classBody.body.map {
                case item @ DefDef(modifiers, termname, tparams, vparamss, tpt, rhs) =>
                  if (termname.toString == constructorMethod) {
                    val newMods = Modifiers(IMPLICIT | PARAM | PARAMACCESSOR)
                    val newImplicit = new ValDef(newMods, bindingModule, bindingModuleType, EmptyTree)
                    val newParams = vparamss ::: List(List(newImplicit))
                    val newTree = treeCopy.DefDef(item, modifiers, termname, tparams, newParams, tpt, rhs)
                    newTree
                  }
                  else { item }
                case t => t
              }

              val newImpVal = ValDef(Modifiers(IMPLICIT | PARAMACCESSOR), bindingModule, bindingModuleType, EmptyTree)

              treeCopy.ClassDef(cd, modifiers, name, tparams, Template(newParents, classBody.self, newImpVal :: body))
            }
            else cd
          }
          case t => t
        }
      }

      override def transform (tree:Tree):Tree = {
          val t = preTransform(tree)
          super.transform(t)
      }
    }
  }
}
