# Configuration from Property Files

A feature I believe is long overdue in SubCut is the ability to load configuration from files that can be altered
without needing re-compilation. While it has always been possible (even easy) to write such property loaders yourself
and bind them into the subcut modules, I really wanted to add the functionality into the core subcut library and
also provide some level of early problem detection and type support, including custom types.

As of SubCut 2.5, this feature is now available for Java property files at present, with XML and JSON support to follow
soon (JSON will likely be a separate extension library so that subcut retains its current policy of no dependencies
other than standard scala libs).

The BindingModule for using Property file based bindings is called `PropertyFileModule` and can be used as follows:

1. Include a `PropertyFileModule` definition in your bindings definition, e.g.

   ```scala
   implicit val bindings = PropertyFileModule.fromResourceFile("somepropfileonclasspath.txt") ~ ProjectBindings ~ GlobalBindings
   ```

   or by mixing it in to a standard binding module definition:

   ```scala
   val withBothParsers = PropertyMappings.Standard + ("Seq[String]" -> seqStringParser) + ("Person" -> personParser)

   implicit val bindings: BindingModule = newBindingModule( module => {
     module <~ PropertyFileModule.fromResourceFile("custompropbindings.txt", withBothParsers)
     module.bind[Person] idBy "Fred" toSingle Person("Fred", "Smith", 33)
     module.bind[Int] idBy "aprogint" toSingle 33
   })
   ```

   More details on the definition of the `PropertyFileModule` can be found below, along with the `fromResourceFile` method
   and what the `PropertyMappings.Standard` and the custom parsers definitions are all about (but in a nutshell, you
   can define your own types to parse from property files using this mechanism).

2. Create a property file to load the configuration from. Some examples:

    ```
    system.database.url = http://mydbserver:3306/somedb
    system.database.timeout = 5 seconds
    ```

    This is the simplest form of property bindings, and assumes string values for the bindings used. The
    `PropertyFileModule` is also capable of parsing non-string types if you specify them in the property file
    like this:

    ```
    system.database.name = mydbserver
    system.database.port.[Int] = 3306
    system.database.timeout.[Duration] = 5 seconds
    ```

    the type to use goes at the end after a ., and using the `[]` brackets (which scala uses to denote types). This must
    be the last segment of the property key, and will not be used as part of the name (so the names above will be
    `system.database.name`, `system.database.port` and `system.database.timeout`, and will have types `String`, `Int`
    and `Duration` respectively. Standard parsers are provided for
    `String, Int, Long, Boolean, Float, Double, Char and Duration` and you can also add your own.

3. Use the injected bindings as normal. The type is enforced (and parsed) as the module is loaded, so there should
   be no runtime surprises when you inject the values. For example, a `[Duration]` defined in the property file
   must have a valid duration definition and will be parsed and checked when the property module is loaded. When you
   want to use it, you just use it like this:

   ```scala
   class DBConfig(implicit val bindingModule: BindingModule) extends Injectable {
     val dbName = inject [String] ("system.database.name")
     val dbPort = inject [Int] ("system.database.port")
     val dbTimeout = inject [Duration] ("system.database.timeout")

     val dbUrl = s"http://$dbName:$dbPort"
   }
   ```

   attempting to inject the system.database.timeout as anything other than a Duration (e.g. trying to inject it as a String)
   will give a binding not found exception. The type safety is ensured by the parsing of the property file.


## Custom Parsers

String, common primitive, and duration parsers are provided, but naturally you will find yourself wanting to create
property parsers for your own types and use those to load a property file. Fortunately this is very easy. Simply
create an extension of `PropertyParser[YourTypeHere]` and then add a map entry `YourTypeHere -> newPropertyParser` to
the PropertyMappings.Standard map when you create the PropertyFileModule to parse the file. For example:

    ```scala
    case class Person(first: String, last: String, age: Int)

    val seqStringParser = new PropertyParser[Seq[String]] {
      def parse(propString: String): Seq[String] = propString.split(',').map(_.trim).toList
    }

    val personParser = new PropertyParser[Person] {
      def parse(propString: String): Person = {
        val fields = propString.split(',').map(_.trim)
        Person(fields(1), fields(0), fields(2).toInt)
      }
    }

    val customParserMap = PropertyMappings.Standard + ("Seq[String]" -> seqStringParser) + ("Person" -> personParser)

    implicit val binding = PropertyFileModule.fromResourceFile("custompropbindings.txt", customParserMap)
    ```

The string you use in the parser map for the key should correspond to the type you will specify in the property file.
For example, in the above code, we define parsers for `Seq[String]` and a `Person` types, so these would be specified
in the property file like this:

    ```
    # our new custom bindings
    seq.of.strings.[Seq[String]] = hello, there, today
    some.person.[Person] = Wall, Dick, 25

    # some other standard bindings
    simple1 = hello
    someInt.[Int] = 6
    ```



Then once the property file has loaded without errors, you can inject your custom types along with the standard ones:

    ```scala
    class PropertyInjectedClass(implicit val bindingModule: BindingModule) extends Injectable {

      val seqOfString = inject[Seq[String]]("seq.of.strings")  // inject the Seq[String]
      val person = inject[Person]("some.person")               // inject the Person

      val simpleString = inject[String]("simple1")
      val someInt = inject[Int]("someInt")
    }
    ```

## Loading files from specific paths

The form `PropertyFileModule.fromResourceFile(filename, mappings)` (mappings is optional) is provided as a convenient way
of finding a property file on the classpath and loading that, but if you want to use a specific path to a file, just use
the standard `apply` method on `PropertyFileModule` that takes a `java.io.File` to load, e.g.:

    ```scala
    PropertyFileModule(new File("/home/user/.config/myapp/properties.txt"))
    ```

    or using a custom mapping

    ```scala
    PropertyFileModule(new File("/home/user/.config/myapp/properties.txt", customMappings))
    ```





