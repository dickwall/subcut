package com.escalatesoft.subcut.inject

import org.scalatest.{FunSuite, Matchers, SeveredStackTraces}

class InstanceProviderTest extends FunSuite with Matchers {

  test("NewInstanceProvider should provide a new instance each time instance property is accessed") {  
    object InstanceCounter extends GlobalCounter
    class InstanceVerifier {
      val count = InstanceCounter.getAndIncrement
    }
    
    val p = new NewInstanceProvider(() => {new InstanceVerifier})
    val ia = p.instance
    val ib = p.instance 
    ia.count should be < (ib.count)
  } 

  test("LazyInstanceProvider should provide the same instance each time instance property is accessed") {  
    object InstanceCounter extends GlobalCounter
    class InstanceVerifier {   
      val count = InstanceCounter.getAndIncrement
    }
    
    val p = new LazyInstanceProvider(() => {new InstanceVerifier})
    val ia = p.instance
    val ib = p.instance 
    ia.count should be (ib.count)
  }

  test("ClassInstanceProvider should provide a new instance each time instance method is called") {             
    val p = new ClassInstanceProvider[ClassInstanceProviderVerifier](classOf[ClassInstanceProviderVerifier].asInstanceOf[Class[Any]])
    import NewBindingModule.newBindingModule
    val bindingModule = newBindingModule { module => }
    val ia = p.newInstance[ClassInstanceProviderVerifier](bindingModule)
    val ib = p.newInstance[ClassInstanceProviderVerifier](bindingModule)
    ia.count should be < (ib.count)
  }

}

class GlobalCounter {
  val x = new java.util.concurrent.atomic.AtomicInteger(0)
  def getAndIncrement = x.getAndIncrement
  def getCount = x.get
}

object ClassInstanceProviderInstanceCounter extends GlobalCounter
class ClassInstanceProviderVerifier {
  val count = ClassInstanceProviderInstanceCounter.getAndIncrement
}  
