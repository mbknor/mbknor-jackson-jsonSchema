package com.kjetland.jackson.jsonSchema

import java.util

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.databind.jsonFormatVisitors._
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}
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

  case class DefinitionInfo(ref:Option[String], jsonObjectFormatVisitor: Option[JsonObjectFormatVisitor])

  class DefinitionsHandler() {
    var class2Ref = Map[Class[_], String]()
    val definitionsNode = JsonNodeFactory.instance.objectNode()


    case class WorkInProgress(classInProgress:Class[_], nodeInProgress:ObjectNode)

    var workInProgress:Option[WorkInProgress] = None

    // returns ref
    def getOrCreateDefinition(clazz:Class[_])(objectDefinitionBuilder:(ObjectNode) => Option[JsonObjectFormatVisitor]):DefinitionInfo = {

      class2Ref.get(clazz) match {
        case Some(ref) =>

          workInProgress match {
            case None =>
              DefinitionInfo(Some(ref), None)

            case Some(w) =>
              // this is a recursive polymorphism call
              if ( clazz != w.classInProgress) throw new Exception(s"Wrong class - working on ${w.classInProgress} - got $clazz")

              DefinitionInfo(None, objectDefinitionBuilder(w.nodeInProgress))
          }

        case None =>

          // new one - must build it
          var retryCount = 0
          var shortRef = clazz.getSimpleName
          var longRef = "#/definitions/"+clazz.getSimpleName
          while( class2Ref.values.contains(longRef)) {
            retryCount = retryCount + 1
            shortRef = clazz.getSimpleName + "_" + retryCount
            longRef = "#/definitions/"+clazz.getSimpleName + "_" + retryCount
          }
          class2Ref = class2Ref + (clazz -> longRef)

          // create definition
          val node = JsonNodeFactory.instance.objectNode()

          // When processing polymorphism, we might get multiple recursive calls to getOrCreateDefinition - this is a wau to combine them
          workInProgress = Some(WorkInProgress(clazz, node))

          definitionsNode.set(shortRef, node)

          val jsonObjectFormatVisitor = objectDefinitionBuilder.apply(node)

          workInProgress = None

          DefinitionInfo(Some(longRef), jsonObjectFormatVisitor)
      }
    }

    def getFinalDefinitionsNode():Option[ObjectNode] = {
      if (class2Ref.isEmpty) None else Some(definitionsNode)
    }

  }

  class MyJsonFormatVisitorWrapper(objectMapper: ObjectMapper, level:Int = 0, val node: ObjectNode = JsonNodeFactory.instance.objectNode(), val definitionsHandler:DefinitionsHandler) extends JsonFormatVisitorWrapper with MySerializerProvider {

    def l(s: String): Unit = {
      var indent = ""
      for( i <- 0 until level) {
        indent = indent + "  "
      }
      println(indent + s)
    }

    def createChild(childNode: ObjectNode): MyJsonFormatVisitorWrapper = new MyJsonFormatVisitorWrapper(objectMapper, level + 1, node = childNode, definitionsHandler = definitionsHandler)

    override def expectStringFormat(_type: JavaType) = {
      l(s"expectStringFormat - _type: ${_type}")

      node.put("type", "string")

      new JsonStringFormatVisitor with EnumSupport {
        val _node = node
        override def format(format: JsonValueFormat): Unit = l(s"JsonStringFormatVisitor.format: ${format}")
      }


    }

    override def expectArrayFormat(_type: JavaType) = {
      l(s"expectArrayFormat - _type: ${_type}")

      node.put("type", "array")

      val itemsNode = JsonNodeFactory.instance.objectNode()
      node.set("items", itemsNode)

      new JsonArrayFormatVisitor with MySerializerProvider {
        override def itemsFormat(handler: JsonFormatVisitable, elementType: JavaType): Unit = {
          l(s"expectArrayFormat - handler: $handler - elementType: $elementType")
          objectMapper.acceptJsonFormatVisitor(elementType, createChild(itemsNode))
        }

        override def itemsFormat(format: JsonFormatTypes): Unit = {
          l(s"itemsFormat - format: $format")
          itemsNode.put("type", format.value())
        }
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

            val definitionInfo: DefinitionInfo = definitionsHandler.getOrCreateDefinition(subType.clazz){
              objectNode =>

                // Set the title = subTypeName
                objectNode.put("title", subType.subTypeName)

                val childVisitor = createChild(objectNode)
                objectMapper.acceptJsonFormatVisitor(subType.clazz, childVisitor)

                // must inject the 'type'-param and value as enum with only one possible value
                val propertiesNode = objectNode.get("properties").asInstanceOf[ObjectNode]

                val enumValuesNode = JsonNodeFactory.instance.arrayNode()
                enumValuesNode.add(subType.subTypeName)

                val enumObjectNode = JsonNodeFactory.instance.objectNode()
                enumObjectNode.put("type", "string")
                enumObjectNode.set("enum", enumValuesNode)
                enumObjectNode.put("default", subType.subTypeName)

                propertiesNode.set(subTypeSpecifierPropertyName, enumObjectNode)

                val requiredNode:ArrayNode = Option(propertiesNode.get("required")).map(_.asInstanceOf[ArrayNode]).getOrElse {
                  val rn = JsonNodeFactory.instance.arrayNode()
                  objectNode.set("required", rn)
                  rn
                }

                requiredNode.add(subTypeSpecifierPropertyName)

                None
            }

            val thisOneOfNode = JsonNodeFactory.instance.objectNode()
            thisOneOfNode.put("$ref", definitionInfo.ref.get)
            anyOfArrayNode.add(thisOneOfNode)



        }

        null // Returning null to stop jackson from visiting this object since we have done it manually

      } else {


        val objectBuilder:ObjectNode => Option[JsonObjectFormatVisitor] = {
          thisObjectNode:ObjectNode =>

            thisObjectNode.put("type", "object")
            thisObjectNode.put("additionalProperties", false)

            val propertiesNode = JsonNodeFactory.instance.objectNode()
            thisObjectNode.set("properties", propertiesNode)

            Some(new JsonObjectFormatVisitor with MySerializerProvider {
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
            })
        }

        if ( level == 0) {
          // This is the first level - we must not use definitions
          objectBuilder(node).orNull
        } else {
          val definitionInfo: DefinitionInfo = definitionsHandler.getOrCreateDefinition(_type.getRawClass)(objectBuilder)

          definitionInfo.ref.foreach {
            r =>
              // Must add ref to def at "this location"
              node.put("$ref", r)
          }

          definitionInfo.jsonObjectFormatVisitor.orNull
        }

      }

    }

  }


  def generateJsonSchema[T <: Any](clazz: Class[T]): JsonNode = {

    val rootNode = JsonNodeFactory.instance.objectNode()

    // Specify that this is a v4 json schema
    rootNode.put("$schema", "http://json-schema.org/draft-04/schema#")
    //rootNode.put("id", "http://my.site/myschema#")

    val definitionsHandler = new DefinitionsHandler
    val rootVisitor = new MyJsonFormatVisitorWrapper(rootObjectMapper, node = rootNode, definitionsHandler = definitionsHandler)
    rootObjectMapper.acceptJsonFormatVisitor(clazz, rootVisitor)

    definitionsHandler.getFinalDefinitionsNode().foreach {
      definitionsNode => rootNode.set("definitions", definitionsNode)
    }

    rootNode
  }

}
