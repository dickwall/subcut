package com.escalatesoft.subcut.inject

import org.scalatest.{FunSuite, Matchers}

class PlainScalaInjectInBindingTest extends FunSuite with Matchers {

  test("inject method is used as service locator for plain Scala constructor style dependency injection during binding") {
	  val client = EchoModule.inject [EchoClient] (Some("constructorStyle"))
	  val result = client.callEcho("Hallooo")
	  result should equal ("Hallooo Constructor Style")	  
  } 
  
  test("inject method is used as service locator for plain Scala def override style dependency injection during binding") {
	  val client = EchoModule.inject [EchoClient] (Some("defOverrideStyle"))
	  val result = client.callEcho("Hallooo")
	  result should equal ("Hallooo Def Override Style")	  
  }  
  
  test("inject method is used as service locator for plain Scala property style dependency injection during binding") {
	  val client = EchoModule.inject [EchoClient] (Some("propertyStyle"))
	  val result = client.callEcho("Hallooo")
	  result should equal ("Hallooo Property Style")	  
  }  
  
  
  class EchoService {
    def echo(echo: String): String = {
      echo
    }
  }
  
  trait EchoClient {
    def callEcho(echoMe: String): String
  }
  
  //constructor style implementation
  class EchoClientConstructorDeps(echoService: EchoService) extends EchoClient {
    def callEcho(echoMe: String): String = {
      echoService.echo(echoMe  + " Constructor Style")
    }
  }  

  //property style implementation
  class EchoClientPropertyDeps() extends EchoClient {
    var echoService: EchoService = null
    def callEcho(echoMe: String): String = {
      echoService.echo(echoMe  + " Property Style")
    }
  }
  
  //def override style implementation
  abstract class EchoClientDefDeps() extends EchoClient {
    def echoService: EchoService
    def callEcho(echoMe: String): String = {
      echoService.echo(echoMe + " Def Override Style")
    }
  }
  
  //all implementations unaware of framework, inject called during module definition
  object EchoModule extends MutableBindingModule with Injectable {
    
    val bindingModule = this
    
    bind [EchoService] toSingle new EchoService
    
    bind [EchoClient] identifiedBy "constructorStyle" toSingle {
      new EchoClientConstructorDeps(inject[EchoService])
    }
    
    bind [EchoClient] identifiedBy "defOverrideStyle" toSingle {
      new EchoClientDefDeps {
        override def echoService = inject[EchoService]
      }
    }

    bind [EchoClient] identifiedBy "propertyStyle" toSingle {
      new EchoClientPropertyDeps() {
        echoService = inject[EchoService]
      }
    }
    
  }  

}