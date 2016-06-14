package com.kjetland.jackson.jsonSchema

import java.util

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.databind.jsonFormatVisitors._
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import org.slf4j.LoggerFactory

class JsonSchemaGenerator(rootObjectMapper:ObjectMapper) {

  val log = LoggerFactory.getLogger(getClass)

  trait MySerializerProvider {
    var provider:SerializerProvider = null
    def getProvider: SerializerProvider = provider

    def setProvider(provider: SerializerProvider): Unit = this.provider = provider

  }


  case class SubTypeAndTypeName[T](clazz:Class[T], subTypeName:String)

  class MyJsonFormatVisitorWrapper(objectMapper:ObjectMapper, indent:String = "", val node:ObjectNode = JsonNodeFactory.instance.objectNode()) extends JsonFormatVisitorWrapper with MySerializerProvider {

    def l(s:String): Unit = {
      println(indent + s)
    }

    def createChild(childNode:ObjectNode):MyJsonFormatVisitorWrapper = new MyJsonFormatVisitorWrapper(objectMapper, indent+ "   ", childNode)

    override def expectStringFormat(_type: JavaType) = new JsonStringFormatVisitor {
      override def enumTypes(enums: util.Set[String]): Unit = l(s"enums: $enums")

      override def format(format: JsonValueFormat): Unit = l(s"format: $format")
    }

    override def expectArrayFormat(_type: JavaType) = new JsonArrayFormatVisitor with MySerializerProvider {
      override def itemsFormat(handler: JsonFormatVisitable, elementType: JavaType): Unit = l(s"expectArrayFormat - handler: $handler - elementType: $elementType")

      override def itemsFormat(format: JsonFormatTypes): Unit = l(s"itemsFormat - format: $format")
    }

    override def expectNumberFormat(_type: JavaType) = new JsonNumberFormatVisitor {
      override def numberType(_type: NumberType): Unit = ???

      override def enumTypes(enums: util.Set[String]): Unit = ???

      override def format(format: JsonValueFormat): Unit = ???
    }

    override def expectAnyFormat(_type: JavaType) = new JsonAnyFormatVisitor {

    }

    override def expectIntegerFormat(_type: JavaType) = new JsonIntegerFormatVisitor {
      override def numberType(_type: NumberType): Unit = l(s"expectIntegerFormat - type = ${_type}")

      override def enumTypes(enums: util.Set[String]): Unit = ???

      override def format(format: JsonValueFormat): Unit = ???
    }

    override def expectNullFormat(_type: JavaType) = new JsonNullFormatVisitor {

    }

    override def expectObjectFormat(_type: JavaType) = {
      node.put("type", "object")

      val propertiesNode = JsonNodeFactory.instance.objectNode()
      node.set("properties", propertiesNode)

      new JsonObjectFormatVisitor with MySerializerProvider {
        override def optionalProperty(writer: BeanProperty): Unit = {
          val propertyName = writer.getName
          val propertyType = writer.getType
          l(s"${propertyName}: ${propertyType}")

          // check for polymorphism


          //val polymorphism:Boolean = !propertyType.isConcrete && !propertyType.isArrayType && !propertyType.isCollectionLikeType && !propertyType.isContainerType && propertyType.isAbstract

          val subTypes: List[SubTypeAndTypeName[_]] = Option(propertyType.getRawClass.getDeclaredAnnotation(classOf[JsonSubTypes])).map {
            ann: JsonSubTypes => ann.value().map {
              t: JsonSubTypes.Type =>
                SubTypeAndTypeName(t.value(), t.name())
            }.toList
          }.getOrElse(List())

          if (subTypes.nonEmpty) {
            //l(s"polymorphism - subTypes: $subTypes")

            val anyOfNode = JsonNodeFactory.instance.objectNode()
            propertiesNode.set(propertyName, anyOfNode)

            val anyOfArrayNode = JsonNodeFactory.instance.arrayNode()
            anyOfNode.set("anyOf", anyOfArrayNode)

            val subTypeSpecifierPropertyName: String = propertyType.getRawClass.getDeclaredAnnotation(classOf[JsonTypeInfo]).property()

            subTypes.foreach {
              subType: SubTypeAndTypeName[_] =>
                l(s"polymorphism - subType: $subType")

                val thisPropertyNode = JsonNodeFactory.instance.objectNode()
                anyOfArrayNode.add(thisPropertyNode)

                val childVisitor = createChild( thisPropertyNode )
                objectMapper.acceptJsonFormatVisitor(subType.clazz, childVisitor)

                // must inject the 'type'-param and value as enum with only one possible value
                thisPropertyNode.get("properties").asInstanceOf[ObjectNode].put(subTypeSpecifierPropertyName, subType.subTypeName)
            }

          } else {

            val thisPropertyNode = JsonNodeFactory.instance.objectNode()
            propertiesNode.set(propertyName, thisPropertyNode)

            val childNode = JsonNodeFactory.instance.objectNode()

            objectMapper.acceptJsonFormatVisitor(propertyType, createChild(thisPropertyNode))
          }

        }

        override def optionalProperty(name: String, handler: JsonFormatVisitable, propertyTypeHint: JavaType): Unit = ???

        override def property(writer: BeanProperty): Unit = ???

        override def property(name: String, handler: JsonFormatVisitable, propertyTypeHint: JavaType): Unit = ???
      }
    }

    override def expectBooleanFormat(_type: JavaType) = new JsonBooleanFormatVisitor {
      override def enumTypes(enums: util.Set[String]): Unit = ???

      override def format(format: JsonValueFormat): Unit = ???
    }

    override def expectMapFormat(_type: JavaType) = new JsonMapFormatVisitor with MySerializerProvider {
      override def keyFormat(handler: JsonFormatVisitable, keyType: JavaType): Unit = ???

      override def valueFormat(handler: JsonFormatVisitable, valueType: JavaType): Unit = ???
    }

  }


  def generateJsonSchema[T <: Any](clazz:Class[T]):JsonNode = {
    val rootVisitor = new MyJsonFormatVisitorWrapper(rootObjectMapper)
    rootObjectMapper.acceptJsonFormatVisitor(clazz, rootVisitor)
    rootVisitor.node
  }

}
