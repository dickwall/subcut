package org.scala_tools.subcut.inject

import tools.nsc.plugins.{Plugin, PluginComponent}
import tools.nsc.Global
import tools.nsc.transform.{TypingTransformers, InfoTransform, Transform}
import com.sun.xml.internal.ws.developer.MemberSubmissionAddressing.Validation
import tools.nsc.symtab.Flags

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

  private object AnnotationsInjectComponent extends PluginComponent with Transform with TypingTransformers {
    val global: AnnotationsInjectPlugin.this.global.type = AnnotationsInjectPlugin.this.global
    import global._

    // val runsAfter = "parser"
    //Using the Scala Compiler 2.8.x the runsAfter should be written as below
    val runsAfter = List[String]("parser")
    override val runsBefore = List[String]("namer")
    val phaseName = AnnotationsInjectPlugin.this.name

    def newTransformer(unit: CompilationUnit) = new AnnotationsInjectTransformer (unit)

    class AnnotationsInjectTransformer(unit: CompilationUnit) extends TypingTransformer(unit) {
      def preTransform(tree: Tree): Tree = {

        tree match {
          case cd @ ClassDef(modifiers, name, tparams, classBody) => {
            // TODO: This should not be a string check in the production version
            val injectPresent = cd.mods.annotations.toString.contains("new Inject()")
            if (injectPresent) {
              inform("@Injecting the class %s".format(name))
              //val newInj = Ident(newTypeName("Injectable"))
              //val newParents = newInj :: classBody.parents
              val newParents = classBody.parents

              val body = classBody.body.map {
                case item @ DefDef(modifiers, termname, tparams, vparamss, tpt, rhs) =>
                  if (termname.toString == "<init>") {
                    val newMods = Modifiers(536879616L)
                    val newImplicit = new ValDef(newMods, "bindingModule", Ident(newTypeName("BindingModule")), EmptyTree)
                    val newParams = vparamss ::: List(List(newImplicit))
                    val newTree = treeCopy.DefDef(item, modifiers, termname, tparams, newParams, tpt, rhs)
                    newTree
                  }
                  else { item }
                case t => t
              }

              val newImpVal = ValDef(Modifiers(Flags.IMPLICIT | Flags.PARAMACCESSOR), "bindingModule", Ident(newTypeName("BindingModule")), EmptyTree)

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