package com.kjetland.jackson.jsonSchema

import java.util

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.databind.jsonFormatVisitors._
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import org.slf4j.LoggerFactory

class JsonSchemaGenerator(rootObjectMapper: ObjectMapper) {

  import scala.collection.JavaConversions._

  val log = LoggerFactory.getLogger(getClass)

  trait MySerializerProvider {
    var provider: SerializerProvider = null

    def getProvider: SerializerProvider = provider

    def setProvider(provider: SerializerProvider): Unit = this.provider = provider

  }

  trait EnumSupport {

    val _node: ObjectNode

    def enumTypes(enums: util.Set[String]): Unit = {
      // l(s"JsonStringFormatVisitor-enum.enumTypes: ${enums}")

      val enumValuesNode = JsonNodeFactory.instance.arrayNode()
      _node.set("enum", enumValuesNode)

      enums.toSet[String].foreach {
        enumValue =>
          enumValuesNode.add(enumValue)
      }
    }
  }


  case class SubTypeAndTypeName[T](clazz: Class[T], subTypeName: String)

  class MyJsonFormatVisitorWrapper(objectMapper: ObjectMapper, indent: String = "", val node: ObjectNode = JsonNodeFactory.instance.objectNode()) extends JsonFormatVisitorWrapper with MySerializerProvider {

    def l(s: String): Unit = {
      println(indent + s)
    }

    def createChild(childNode: ObjectNode): MyJsonFormatVisitorWrapper = new MyJsonFormatVisitorWrapper(objectMapper, indent + "   ", childNode)

    override def expectStringFormat(_type: JavaType) = {
      l(s"expectStringFormat - _type: ${_type}")

      node.put("type", "string")

      new JsonStringFormatVisitor with EnumSupport {
        val _node = node
        override def format(format: JsonValueFormat): Unit = l(s"JsonStringFormatVisitor.format: ${format}")
      }


    }

    override def expectArrayFormat(_type: JavaType) = {
      l("expectArrayFormat")

      node.put("type", "array")

      new JsonArrayFormatVisitor with MySerializerProvider {
        override def itemsFormat(handler: JsonFormatVisitable, elementType: JavaType): Unit = l(s"expectArrayFormat - handler: $handler - elementType: $elementType")

        override def itemsFormat(format: JsonFormatTypes): Unit = l(s"itemsFormat - format: $format")
      }
    }

    override def expectNumberFormat(_type: JavaType) = {
      l("expectNumberFormat")

      node.put("type", "number")

      new JsonNumberFormatVisitor  with EnumSupport {
        val _node = node
        override def numberType(_type: NumberType): Unit = l(s"JsonNumberFormatVisitor.numberType: ${_type}")
        override def format(format: JsonValueFormat): Unit = l(s"JsonNumberFormatVisitor.format: ${format}")
      }
    }

    override def expectAnyFormat(_type: JavaType) = {
      log.warn(s"Not able to generate jsonSchema-info for type: ${_type} - probably using custom serializer which does not override acceptJsonFormatVisitor")
      new JsonAnyFormatVisitor {
      }

    }

    override def expectIntegerFormat(_type: JavaType) = {
      l("expectIntegerFormat")

      node.put("type", "integer")

      new JsonIntegerFormatVisitor with EnumSupport {
        val _node = node
        override def numberType(_type: NumberType): Unit = l(s"JsonIntegerFormatVisitor.numberType: ${_type}")
        override def format(format: JsonValueFormat): Unit = l(s"JsonIntegerFormatVisitor.format: ${format}")
      }
    }

    override def expectNullFormat(_type: JavaType) = new JsonNullFormatVisitor {
      l("expectNullFormat")
      ???
    }


    override def expectBooleanFormat(_type: JavaType) = {
      l("expectBooleanFormat")

      node.put("type", "boolean")

      new JsonBooleanFormatVisitor with EnumSupport {
        val _node = node
        override def format(format: JsonValueFormat): Unit = l(s"JsonBooleanFormatVisitor.format: ${format}")
      }
    }

    override def expectMapFormat(_type: JavaType) = {
      l("expectMapFormat")

      ???

      new JsonMapFormatVisitor with MySerializerProvider {
        override def keyFormat(handler: JsonFormatVisitable, keyType: JavaType): Unit = ???

        override def valueFormat(handler: JsonFormatVisitable, valueType: JavaType): Unit = ???
      }
    }


    override def expectObjectFormat(_type: JavaType) = {

      val subTypes: List[SubTypeAndTypeName[_]] = Option(_type.getRawClass.getDeclaredAnnotation(classOf[JsonSubTypes])).map {
        ann: JsonSubTypes => ann.value().map {
          t: JsonSubTypes.Type =>
            SubTypeAndTypeName(t.value(), t.name())
        }.toList
      }.getOrElse(List())

      if (subTypes.nonEmpty) {
        //l(s"polymorphism - subTypes: $subTypes")

        val anyOfArrayNode = JsonNodeFactory.instance.arrayNode()
        node.set("oneOf", anyOfArrayNode)

        val subTypeSpecifierPropertyName: String = _type.getRawClass.getDeclaredAnnotation(classOf[JsonTypeInfo]).property()

        subTypes.foreach {
          subType: SubTypeAndTypeName[_] =>
            l(s"polymorphism - subType: $subType")

            val thisOneOfNode = JsonNodeFactory.instance.objectNode()

            // Set the title = subTypeName
            thisOneOfNode.put("title", subType.subTypeName)

            anyOfArrayNode.add(thisOneOfNode)

            val childVisitor = createChild(thisOneOfNode)
            objectMapper.acceptJsonFormatVisitor(subType.clazz, childVisitor)

            // must inject the 'type'-param and value as enum with only one possible value
            val propertiesNode = thisOneOfNode.get("properties").asInstanceOf[ObjectNode]

            val enumValuesNode = JsonNodeFactory.instance.arrayNode()
            enumValuesNode.add(subType.subTypeName)

            val enumObjectNode = JsonNodeFactory.instance.objectNode()
            enumObjectNode.set("enum", enumValuesNode)
            enumObjectNode.put("default", subType.subTypeName)


            propertiesNode.set(subTypeSpecifierPropertyName, enumObjectNode)
        }

        null // Returning null to stop jackson from visiting this object since we have done it manually

      } else {
        node.put("type", "object")

        val propertiesNode = JsonNodeFactory.instance.objectNode()
        node.set("properties", propertiesNode)

        new JsonObjectFormatVisitor with MySerializerProvider {
          override def optionalProperty(writer: BeanProperty): Unit = {
            val propertyName = writer.getName
            val propertyType = writer.getType
            l(s"JsonObjectFormatVisitor - ${propertyName}: ${propertyType}")

            val thisPropertyNode = JsonNodeFactory.instance.objectNode()
            propertiesNode.set(propertyName, thisPropertyNode)

            val childNode = JsonNodeFactory.instance.objectNode()

            objectMapper.acceptJsonFormatVisitor(propertyType, createChild(thisPropertyNode))

          }

          override def optionalProperty(name: String, handler: JsonFormatVisitable, propertyTypeHint: JavaType): Unit = {
            l(s"JsonObjectFormatVisitor.optionalProperty: name:${name} handler:${handler} propertyTypeHint:${propertyTypeHint}")
          }

          override def property(writer: BeanProperty): Unit = l(s"JsonObjectFormatVisitor.property: name:${writer}")

          override def property(name: String, handler: JsonFormatVisitable, propertyTypeHint: JavaType): Unit = {
            l(s"JsonObjectFormatVisitor.property: name:${name} handler:${handler} propertyTypeHint:${propertyTypeHint}")
          }
        }

      }

    }

  }


  def generateJsonSchema[T <: Any](clazz: Class[T]): JsonNode = {

    val rootNode = JsonNodeFactory.instance.objectNode()

    // Specify that this is a v4 json schema
    rootNode.put("$schema", "http://json-schema.org/draft-04/schema#")

    val rootVisitor = new MyJsonFormatVisitorWrapper(rootObjectMapper, node = rootNode)
    rootObjectMapper.acceptJsonFormatVisitor(clazz, rootVisitor)

    rootNode
  }

}
