package com.escalatesoft.subcut.inject.util

import com.escalatesoft.subcut.inject.{BindingException, NewBindingModule, BindingModule, BindingKey}
import NewBindingModule.newBindingModule
import java.io.{FileInputStream, File}
import java.util.Properties
import scala.collection.JavaConverters._
import scala.language.existentials
import scala.concurrent.duration._

/**
 * Read simple value bindings from a property file for simple file-based configurations
 */
case class PropertyFileModule(propFile: File, propertyParsers: Map[String, PropertyParser[_]] = PropertyMappings.Standard) extends BindingModule {
  if(!propFile.exists) throw new BindingException(s"No file ${propFile.getName} found")

  val bindings = newBindingModule(module => {
    val in = new FileInputStream(propFile)
    try {
      val properties = new Properties
      properties.load(in)

      val propsMap = properties.asScala

      for ((k, v) <- propsMap) {
        val (name, parser) = parserFor(k)
        try {
          module.bindings += parser.keyFor(name) -> parser.parse(v)
        }
        catch {
          case ex: Exception => throw new BindingException(s"""Could not parse "$v" for $k - check value is consistent with [type]: ${ex.getMessage}""")
        }
      }
    } finally in.close()
  }).bindings

  private def parserFor(k: String): (String, PropertyParser[_]) = {
    val splits = k.split('.')
    val typeMarker = if (splits.length > 1 && splits.last.startsWith("["))  splits.last.tail.init else ""  // strip the []'s off

    if (typeMarker.isEmpty) (k, PropertyMappings.StringParser) else { // if no type marker, treat as String
      val typeParser = propertyParsers.get(typeMarker).getOrElse(
        throw new BindingException(s"No provided parser for type ${splits.last} in properties ${propFile.getName}"))
      (splits.init.mkString("."), typeParser)
    }
  }
}

object PropertyFileModule {
  def fromResourceFile(fileName: String, propertyParsers: Map[String, PropertyParser[_]] = PropertyMappings.Standard): PropertyFileModule = {
    val resourceFile = new File(getClass.getClassLoader.getResource(fileName).getFile)
    PropertyFileModule(resourceFile, propertyParsers)
  }
}


object PropertyMappings {
  val StringParser = new PropertyParser[String] {
    def parse(prop: String): String = prop
  }

  val IntParser = new PropertyParser[Int] {
    def parse(prop: String): Int = prop.toInt
  }

  val LongParser = new PropertyParser[Long] {
    def parse(prop: String): Long = prop.toLong
  }

  val CharParser = new PropertyParser[Char] {
    def parse(prop: String): Char = prop.head
  }

  val DoubleParser = new PropertyParser[Double] {
    def parse(prop: String): Double = prop.toDouble
  }

  val FloatParser = new PropertyParser[Float] {
    def parse(prop: String): Float = prop.toFloat
  }

  val BooleanParser = new PropertyParser[Boolean] {
    def parse(prop: String): Boolean = prop.toBoolean
  }

  val DurationParser = new PropertyParser[Duration] {
    def parse(prop: String): Duration = Duration(prop)
  }

  val Standard: Map[String, PropertyParser[_]] = Map (
    "String" -> StringParser,
    "Int" -> IntParser,
    "Long" -> LongParser,
    "Double" -> DoubleParser,
    "Float" -> FloatParser,
    "Boolean" -> BooleanParser,
    "Char" -> CharParser,
    "Duration" -> DurationParser
  )
}

abstract class PropertyParser[T : Manifest] {
  def parse(propString: String): T
  def keyFor(name: String): BindingKey[T] = BindingKey(manifest[T], Some(name))
}