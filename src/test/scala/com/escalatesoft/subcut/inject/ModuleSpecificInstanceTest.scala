package com.escalatesoft.subcut.inject

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite

/**
 * Created with IntelliJ IDEA.
 * User: rlwall2
 * Date: 6/12/12
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */

class ModuleSpecificInstanceTest extends FunSuite with ShouldMatchers {
  test ("module specific reflective instances") {
    implicit val bm = DogSnakeModule

    val habitat = new Habitat
    habitat.quadruped.toString should be ("I make noise Woof and I live in lush, green fields")
    habitat.reptile.toString should be ("I Slither to move and I live in lush, green fields")
    habitat.quadruped.domain should be theSameInstanceAs (habitat.reptile.domain)

    val habitat2 = new Habitat
    habitat2.quadruped should be theSameInstanceAs(habitat.quadruped)
    habitat2.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat2.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat2.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)

    val bm2 = SheepModule ~ DogSnakeModule
    val habitat3 = new Habitat()(bm2)
    habitat3.quadruped should not be theSameInstanceAs(habitat.quadruped)
    habitat3.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat3.quadruped should not be theSameInstanceAs(habitat2.reptile)

    habitat3.quadruped.domain should not be theSameInstanceAs (habitat.quadruped.domain)
    habitat3.reptile.domain should not be theSameInstanceAs (habitat.reptile.domain)
    habitat3.quadruped.domain should be theSameInstanceAs (habitat3.reptile.domain)

    habitat3.quadruped.toString should be ("I make noise Baaa and I live in lush, green fields")
    habitat3.reptile.toString should be ("I Slither to move and I live in lush, green fields")

    val bm3 = DesertModule ~ DogSnakeModule
    val habitat4 = new Habitat()(bm3)
    habitat4.quadruped should not be theSameInstanceAs(habitat.quadruped)
    habitat4.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat4.quadruped should not be theSameInstanceAs(habitat3.quadruped)
    habitat4.reptile should not be theSameInstanceAs(habitat3.reptile)

    habitat4.quadruped.domain should not be theSameInstanceAs (habitat3.quadruped.domain)
    habitat4.reptile.domain should not be theSameInstanceAs (habitat3.reptile.domain)
    habitat4.quadruped.domain should be theSameInstanceAs (habitat4.reptile.domain)

    habitat4.quadruped.toString should be ("I make noise Woof and I live in dry, arid desert")
    habitat4.reptile.toString should be ("I Slither to move and I live in dry, arid desert")

    val habitat5 = new Habitat()(bm)
    habitat5.quadruped should be theSameInstanceAs(habitat.quadruped)
    habitat5.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat5.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat5.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)
    habitat5.reptile.domain should be theSameInstanceAs (habitat5.quadruped.domain)
    habitat5.quadruped should not be theSameInstanceAs(habitat4.quadruped)
    habitat5.reptile should not be theSameInstanceAs(habitat4.reptile)
  }

  test ("module specific provider instances") {
    implicit val bm = DogSnakeProviderModule
    import NewBindingModule._

    val habitat = new Habitat
    habitat.quadruped.toString should be ("I make noise Woof and I live in lush, green fields")
    habitat.reptile.toString should be ("I Slither to move and I live in lush, green fields")
    habitat.quadruped.domain should be theSameInstanceAs (habitat.reptile.domain)

    val habitat2 = new Habitat
    habitat2.quadruped should be theSameInstanceAs (habitat.quadruped)
    habitat2.reptile should be theSameInstanceAs (habitat.reptile)
    habitat2.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat2.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)

    val bm2 = newBindingModule { module =>
      module <~ DogSnakeProviderModule
      module.bind [Quadruped] toModuleSingle { implicit module => new Sheep }
    }

    val habitat3 = new Habitat()(bm2)
    habitat3.quadruped should not be theSameInstanceAs(habitat.quadruped)
    habitat3.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat3.quadruped should not be theSameInstanceAs(habitat2.reptile)

    habitat3.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat3.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)
    habitat3.quadruped.domain should be theSameInstanceAs (habitat3.reptile.domain)

    habitat3.quadruped.toString should be ("I make noise Baaa and I live in lush, green fields")
    habitat3.reptile.toString should be ("I Slither to move and I live in lush, green fields")

    val bm3 = newBindingModule { module =>
      module <~ DogSnakeProviderModule
      module.bind [Domain] toSingle new Desert
    }
    val habitat4 = new Habitat()(bm3)
    habitat4.quadruped should not be theSameInstanceAs(habitat.quadruped)
    habitat4.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat4.quadruped should not be theSameInstanceAs(habitat3.quadruped)
    habitat4.reptile should not be theSameInstanceAs(habitat3.reptile)

    habitat4.quadruped.domain should not be theSameInstanceAs (habitat3.quadruped.domain)
    habitat4.reptile.domain should not be theSameInstanceAs (habitat3.reptile.domain)
    habitat4.quadruped.domain should be theSameInstanceAs (habitat4.reptile.domain)

    habitat4.quadruped.toString should be ("I make noise Woof and I live in dry, arid desert")
    habitat4.reptile.toString should be ("I Slither to move and I live in dry, arid desert")

    val habitat5 = new Habitat()(bm)
    habitat5.quadruped should be theSameInstanceAs(habitat.quadruped)
    habitat5.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat5.reptile should be theSameInstanceAs(habitat.reptile)
    habitat5.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)
    habitat5.reptile.domain should be theSameInstanceAs (habitat5.quadruped.domain)
    habitat5.quadruped should not be theSameInstanceAs(habitat4.quadruped)
    habitat5.reptile should not be theSameInstanceAs(habitat4.reptile)
  }

  test ("module specific provider instances using tilde merge") {
    implicit val bm = DogSnakeProviderModule
    import NewBindingModule._

    val habitat = new Habitat
    habitat.quadruped.toString should be ("I make noise Woof and I live in lush, green fields")
    habitat.reptile.toString should be ("I Slither to move and I live in lush, green fields")
    habitat.quadruped.domain should be theSameInstanceAs (habitat.reptile.domain)

    val habitat2 = new Habitat
    habitat2.quadruped should be theSameInstanceAs (habitat.quadruped)
    habitat2.reptile should be theSameInstanceAs (habitat.reptile)
    habitat2.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat2.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)

    val bm2 = SheepProviderModule ~ DogSnakeProviderModule
    bm2.listBindings foreach println

    val habitat3 = new Habitat()(bm2)
    habitat3.quadruped should not be theSameInstanceAs(habitat.quadruped)
    habitat3.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat3.quadruped should not be theSameInstanceAs(habitat2.reptile)

    habitat3.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat3.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)
    habitat3.quadruped.domain should be theSameInstanceAs (habitat3.reptile.domain)

    habitat3.quadruped.toString should be ("I make noise Baaa and I live in lush, green fields")
    habitat3.reptile.toString should be ("I Slither to move and I live in lush, green fields")

    val bm3 = DesertProviderModule ~ DogSnakeProviderModule

    val habitat4 = new Habitat()(bm3)
    habitat4.quadruped should not be theSameInstanceAs(habitat.quadruped)
    habitat4.reptile should not be theSameInstanceAs(habitat.reptile)
    habitat4.quadruped should not be theSameInstanceAs(habitat3.quadruped)
    habitat4.reptile should not be theSameInstanceAs(habitat3.reptile)

    habitat4.quadruped.domain should not be theSameInstanceAs (habitat3.quadruped.domain)
    habitat4.reptile.domain should not be theSameInstanceAs (habitat3.reptile.domain)
    habitat4.quadruped.domain should be theSameInstanceAs (habitat4.reptile.domain)

    habitat4.quadruped.toString should be ("I make noise Woof and I live in dry, arid desert")
    habitat4.reptile.toString should be ("I Slither to move and I live in dry, arid desert")

    val habitat5 = new Habitat()(bm)
    habitat5.quadruped should be theSameInstanceAs(habitat.quadruped)
    habitat5.quadruped.domain should be theSameInstanceAs (habitat.quadruped.domain)
    habitat5.reptile should be theSameInstanceAs(habitat.reptile)
    habitat5.reptile.domain should be theSameInstanceAs (habitat.reptile.domain)
    habitat5.reptile.domain should be theSameInstanceAs (habitat5.quadruped.domain)
    habitat5.quadruped should not be theSameInstanceAs(habitat4.quadruped)
    habitat5.reptile should not be theSameInstanceAs(habitat4.reptile)
  }

  test ("for new NewBindingModule method, toModuleSingle should still work as expected") {
    val custom = new NewBindingModule(module => {
      module.bind [Quadruped] toModuleSingle { implicit module => new Sheep }
      module.bind [Domain] toModuleSingle { implicit module => new Desert }
    })
    // simple lookup
    val newConfig: BindingModule = custom
    val quad1 = newConfig.inject[Quadruped](None)
    val quad2 = newConfig.inject[Quadruped](None)
    quad1 should be theSameInstanceAs quad2

    val dom1 = newConfig.inject[Domain](None)
    val dom2 = newConfig.inject[Domain](None)
    dom1 should be theSameInstanceAs dom2
  }

  test ("toModuleSingle should work just fine with ~ merges") {
    val a = new NewBindingModule(module => {
      module.bind[Domain] toModuleSingle { implicit module => new Desert }
    })
    val b = new NewBindingModule(module => {
      module.bind[Domain] toModuleSingle { implicit module => new Desert }
    })
    val dom1 = a.inject[Domain](None)
    val dom2 = b.inject[Domain](None)
    dom1 should not be theSameInstanceAs (dom2)
    val config: BindingModule = a ~ b
    val dom3 = config.inject[Domain](None)
    val dom4 = config.inject[Domain](None)
    dom3 should be theSameInstanceAs (dom4)
    dom3 should not be theSameInstanceAs (dom1)
    dom3 should not be theSameInstanceAs (dom2)
  }
}

class Habitat(implicit val bindingModule: BindingModule) extends Injectable {
  val quadruped = inject[Quadruped]
  val reptile = inject[Reptile]
}

object DogSnakeModule extends NewBindingModule( module => {
  import module._
  bind [Quadruped] to moduleInstanceOf [Doggy]
  bind [Reptile] to newInstanceOf [Snake]
  bind [Domain] to moduleInstanceOf [Field]
})

object DogSnakeProviderModule extends NewBindingModule( module => {
  import module._
  bind [Quadruped] toModuleSingle { implicit module => new Doggy }
  bind [Reptile] toModuleSingle { implicit module => new Snake }
  bind [Domain] toSingle new Field
})

object SheepModule extends NewBindingModule( module => {
  import module._
  bind [Quadruped] to moduleInstanceOf [Sheep]
})

object DesertModule extends NewBindingModule( module => {
  import module._
  bind [Domain] to moduleInstanceOf [Desert]
})

object SheepProviderModule extends NewBindingModule( module => {
  import module._
  bind [Quadruped] toModuleSingle { implicit module => new Sheep }
})

object DesertProviderModule extends NewBindingModule( module => {
  import module._
  bind [Domain] toSingle new Desert
})

trait Quadruped extends Injectable {
  val domain = inject[Domain]
  def makeNoise: String
  override def toString = "I make noise %s and I live in %s".format(makeNoise, domain.description)
}

class Doggy(implicit val bindingModule: BindingModule) extends Quadruped with Injectable {
  def makeNoise = "Woof"
}

class Sheep(implicit val bindingModule: BindingModule) extends Quadruped with Injectable {
  def makeNoise = "Baaa"
}

trait Domain {
  def description: String
}

class Desert extends Domain {
  def description = "dry, arid desert"
}

class Field extends Domain {
  def description = "lush, green fields"
}

trait Reptile extends Injectable {
  val domain = inject[Domain]
  def move: String
  override def toString = "I %s to move and I live in %s".format(move, domain.description)
}

class Snake(implicit val bindingModule: BindingModule) extends Reptile {
  def move = "Slither"
}

class Lizard(implicit val bindingModule: BindingModule) extends Reptile {
  def move = "Skitter"
}