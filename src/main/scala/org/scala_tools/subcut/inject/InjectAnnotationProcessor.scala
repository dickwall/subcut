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
        tree match {
          case cd @ global.ClassDef(modifiers, name, tparams, classBody) => {
            // TODO: This should not be a print in the production version
            val injectPresent = cd.mods.annotations.toString.contains("new Inject()")
            if (injectPresent) {
              println("@Injecting the class %s".format(name))
              println(classBody.body)
              //for (defdef @ global.DefDef(_, _, _, _, _, _) <- classBody.body) println("hello")
                  /*body.map {
                    case init @ global.DefDef(_, termname, tparams, vparamss, tpt, rhs) =>
                      println(termname)
                      println(tparams)
                      println(vparamss)
                      println(tpt)
                      println(rhs)
                      super.transform(init)
                    case t => super.transform(t)
                  }*/
            }
            super.transform(cd)
            /* // First filtering the list of parameters to get only those parameters which are annotated
            // with a non-null annotation
            val nonnullParameters = findNonnullParameters(vparamss)
            if (!nonnullParameters.isEmpty) {
              // Looking for the "Block" element defining the body of the method
              var block: global.Block = null
              tree.find(tree => tree.isInstanceOf[global.Block]) match {
                case None => throw new IllegalArgumentException("Cannot find body for method %s".format(name))
                case Some(blockTree) => block = blockTree.asInstanceOf[global.Block]
              }

              // For each non-null variable, generating and adding a check to the body of the method
              nonnullParameters foreach { param =>
                val nonnullCheck = createNonnullCheck(param)
                val nonnullCheckWithPos = global.posAssigner.atPos(tree.pos)(nonnullCheck)

                block = block + nonnullCheckWithPos
              }

              // Returning the tree with the block element substituted with the modified one
              val result = new global.Transformer {
                override def transform(tree: global.Tree) = {
                  tree match {
                    case global.Block(_, _) => block
                    case t => super.transform(t)
                  }
                }
              }.transform(dd)

              result
            } else {
              // Just returning the tree, in case no changes need to be done
              dd
            }*/
          }
          case t => super.transform(t)
        }
      }

      /**
       * Here we extend a <code>Block</code> with a plus (+) method, which adds a new statement to the list of
       * statements in the block. The result is a new block.
       * @param block The block to extend.
       */
      /*private implicit def extendBlockWithPlus(block: global.Block) = new {
        def +(toAdd: global.Tree) = {
          global.copy.Block(block, toAdd :: block.stats, block.expr)
        }
      }*/

      /**
       * Creates a nonnull check for the given parameter.
       * @return A {@code Tree} checking that the given parameter is not null.
       */
      /*private def createNonnullCheck(param: global.ValDef) = {
        // Getting the name of the parameter

        /*
          We need to generate:
          if (param == null) throw new IllegalArgumentException("Parameter '#{param.name}' should not be null.")
         */
        global.If(
          // if: param == ...
          global.Apply(
            global.Select(
              global.Ident(param.name),
              global.newTermName("$eq$eq")),
            List(global.Literal(global.Constant(null)))
            ),
          // then: throw ...
          global.Throw(
            global.Apply(
              global.Select(
                global.New(global.Ident(global.newTypeName("IllegalArgumentException"))),
                global.newTermName("<init>")),
              List(global.Literal(global.Constant("Parameter '%s' should not be null.".format(param.name)))))),
          // else
          global.EmptyTree)
      }*/

      /**
       * In the given list of parameters, looks for parameters annotated with a nonnull annotation, that
       * is any annotation that matches the "{@code No[nt][Nn]ull} regexp.
       * @return The list of parameters which are annotated with a nonnull annotation.
       */
      /*private def findInitDefs(body: List[List[global.ValDef]]) = {
        vparamss.flatten[global.ValDef].filter(param => {
          // Checking if the parameter contains a non-null annotation
          param.mods.annotations.exists(annotation => {
            // Checking if the annotation is a non-null annotation
            annotation.constr.find(tree => {
              tree match {
                // case global.Select(qualifier, selector) => "No[nt][Nn]ull".r.findFirstIn(selector.toString).isDefined
                case global.Ident(name) => "No[nt][Nn]ull".r.findFirstIn(name.toString).isDefined
                case _ => false
              }
            }).isDefined
          })
        })
      }*/
    }
  }
}