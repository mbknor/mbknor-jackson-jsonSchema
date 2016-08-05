package com.kjetland.jackson.jsonSchema

import java.lang.reflect.{Field, Method, ParameterizedType}
import java.time.{LocalDate, LocalDateTime, LocalTime, OffsetDateTime}
import java.util
import javax.validation.constraints.NotNull

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.databind.jsonFormatVisitors._
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.introspect.{AnnotatedClass, JacksonAnnotationIntrospector}
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}
import com.kjetland.jackson.jsonSchema.annotations.{JsonSchemaDescription, JsonSchemaFormat, JsonSchemaTitle}
import org.slf4j.LoggerFactory

object JsonSchemaGenerator {
  val JSON_SCHEMA_DRAFT_4_URL = "http://json-schema.org/draft-04/schema#"
}

class JsonSchemaGenerator
(
  val rootObjectMapper: ObjectMapper,
  debug:Boolean = false,
  extraClazz2FormatMapping:Map[Class[_], String] = Map(),
  autoGenerateTitleForProperties:Boolean = true,
  defaultArrayFormat:Option[String] = Some("table")) {

  import scala.collection.JavaConversions._

  val log = LoggerFactory.getLogger(getClass)

  val clazz2FormatMapping = Map[Class[_], String](
    classOf[OffsetDateTime] -> "datetime",
    classOf[LocalDateTime]  -> "datetime-local",
    classOf[LocalDate]      -> "date",
    classOf[LocalTime]      -> "time"
  ) ++ extraClazz2FormatMapping

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


  case class DefinitionInfo(ref:Option[String], jsonObjectFormatVisitor: Option[JsonObjectFormatVisitor])

  // Class that manages creating new defenitions or getting $refs to existing definitions
  class DefinitionsHandler() {
    private var class2Ref = Map[Class[_], String]()
    private val definitionsNode = JsonNodeFactory.instance.objectNode()


    case class WorkInProgress(classInProgress:Class[_], nodeInProgress:ObjectNode)

    // Used when 'combining' multiple invocations to getOrCreateDefinition when processing polymorphism.
    var workInProgress:Option[WorkInProgress] = None

    // Either creates new definitions or return $ref to existing one
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

    def l(s: => String): Unit = {
      if (!debug) return

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

      defaultArrayFormat.foreach {
        format => node.put("format", format)
      }

      val itemsNode = JsonNodeFactory.instance.objectNode()
      node.set("items", itemsNode)

      // When processing scala modules we sometimes get a better elementType here than later in itemsFormat
      val preferredElementType:Option[JavaType] = if ( _type.containedTypeCount() >= 1) Some(_type.containedType(0)) else None

      new JsonArrayFormatVisitor with MySerializerProvider {
        override def itemsFormat(handler: JsonFormatVisitable, _elementType: JavaType): Unit = {
          l(s"expectArrayFormat - handler: $handler - elementType: ${_elementType} - preferredElementType: $preferredElementType")
          val elementType = preferredElementType.getOrElse(_elementType)
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

    override def expectNullFormat(_type: JavaType) = {
      l(s"expectNullFormat - _type: ${_type}")
      new JsonNullFormatVisitor {}
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
      l(s"expectMapFormat - _type: ${_type}")

      // There is no way to specify map in jsonSchema,
      // So we're going to treat it as type=object with additionalProperties = true,
      // so that it can hold whatever the map can hold


      node.put("type", "object")
      node.put("additionalProperties", true)


      new JsonMapFormatVisitor with MySerializerProvider {
        override def keyFormat(handler: JsonFormatVisitable, keyType: JavaType): Unit = {
          l(s"JsonMapFormatVisitor.keyFormat handler: $handler - keyType: $keyType")
        }

        override def valueFormat(handler: JsonFormatVisitable, valueType: JavaType): Unit = {
          l(s"JsonMapFormatVisitor.valueFormat handler: $handler - valueType: $valueType")
        }
      }
    }


    private def getRequiredArrayNode(objectNode:ObjectNode):ArrayNode = {
      Option(objectNode.get("required")).map(_.asInstanceOf[ArrayNode]).getOrElse {
        val rn = JsonNodeFactory.instance.arrayNode()
        objectNode.set("required", rn)
        rn
      }
    }

    case class PolymorphismInfo(typePropertyName:String, subTypeName:String)

    private def extractPolymorphismInfo(_type:JavaType):Option[PolymorphismInfo] = {
      // look for @JsonTypeInfo
      val ac = AnnotatedClass.construct(_type, objectMapper.getDeserializationConfig())
      Option(ac.getAnnotations.get(classOf[JsonTypeInfo])).map {
        jsonTypeInfo =>
          if ( jsonTypeInfo.include() != JsonTypeInfo.As.PROPERTY) throw new Exception("We only support polymorphism using jsonTypeInfo.include() == JsonTypeInfo.As.PROPERTY")
          if ( jsonTypeInfo.use != JsonTypeInfo.Id.NAME) throw new Exception("We only support polymorphism using jsonTypeInfo.use == JsonTypeInfo.Id.NAME")


          val propertyName = jsonTypeInfo.property()

          // must look at the @JsonSubTypes to find what this current class should be called

          val subTypeName:String = Option(ac.getAnnotations.get(classOf[JsonSubTypes])).map {
            ann: JsonSubTypes => ann.value()
              .find {
                t: JsonSubTypes.Type =>
                  t.value() == _type.getRawClass
              }.map(_.name()).getOrElse(throw new Exception(s"Did not find info about the class ${_type.getRawClass} in @JsonSubTypes"))
          }.getOrElse(throw new Exception(s"Did not find @JsonSubTypes"))


          PolymorphismInfo(propertyName, subTypeName)
      }

    }

    override def expectObjectFormat(_type: JavaType) = {

      val subTypes: List[Class[_]] = Option(_type.getRawClass.getDeclaredAnnotation(classOf[JsonSubTypes])).map {
        ann: JsonSubTypes => ann.value().map {
          t: JsonSubTypes.Type => t.value()
        }.toList
      }.getOrElse(List())

      if (subTypes.nonEmpty) {
        //l(s"polymorphism - subTypes: $subTypes")

        val anyOfArrayNode = JsonNodeFactory.instance.arrayNode()
        node.set("oneOf", anyOfArrayNode)

        subTypes.foreach {
          subType: Class[_] =>
            l(s"polymorphism - subType: $subType")

            val definitionInfo: DefinitionInfo = definitionsHandler.getOrCreateDefinition(subType){
              objectNode =>

                val childVisitor = createChild(objectNode)
                objectMapper.acceptJsonFormatVisitor(subType, childVisitor)

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

            // If class is annotated with JsonSchemaFormat, we should add it
            val ac = AnnotatedClass.construct(_type, objectMapper.getDeserializationConfig())
            Option(ac.getAnnotations.get(classOf[JsonSchemaFormat])).map(_.value()).foreach {
              format =>
                thisObjectNode.put("format", format)
            }

            // If class is annotated with JsonSchemaDescription, we should add it
            Option(ac.getAnnotations.get(classOf[JsonSchemaDescription])).map(_.value()).foreach {
              description =>
                thisObjectNode.put("description", description)
            }

            // If class is annotated with JsonSchemaTitle, we should add it
            Option(ac.getAnnotations.get(classOf[JsonSchemaTitle])).map(_.value()).foreach {
              title =>
                thisObjectNode.put("title", title)
            }

            val propertiesNode = JsonNodeFactory.instance.objectNode()
            thisObjectNode.set("properties", propertiesNode)

            extractPolymorphismInfo(_type).map {
              case pi:PolymorphismInfo =>
                // This class is a child in a polymorphism config..
                // Set the title = subTypeName
                thisObjectNode.put("title", pi.subTypeName)

                // must inject the 'type'-param and value as enum with only one possible value
                val enumValuesNode = JsonNodeFactory.instance.arrayNode()
                enumValuesNode.add(pi.subTypeName)

                val enumObjectNode = JsonNodeFactory.instance.objectNode()
                enumObjectNode.put("type", "string")
                enumObjectNode.set("enum", enumValuesNode)
                enumObjectNode.put("default", pi.subTypeName)

                propertiesNode.set(pi.typePropertyName, enumObjectNode)

                getRequiredArrayNode(thisObjectNode).add(pi.typePropertyName)

            }

            Some(new JsonObjectFormatVisitor with MySerializerProvider {
              override def optionalProperty(prop: BeanProperty): Unit = {
                val propertyName = prop.getName
                val propertyType = prop.getType
                l(s"JsonObjectFormatVisitor - ${propertyName}: ${propertyType}")

                val thisPropertyNode = JsonNodeFactory.instance.objectNode()
                propertiesNode.set(propertyName, thisPropertyNode)

                val childNode = JsonNodeFactory.instance.objectNode()


                val childVisitor = createChild(thisPropertyNode)

                // Workaround for scala lists and so on
                if ( (propertyType.isArrayType || propertyType.isCollectionLikeType) && !classOf[Option[_]].isAssignableFrom(propertyType.getRawClass) && propertyType.containedTypeCount() >= 1) {
                  // If visiting a scala list and using default acceptJsonFormatVisitor-approach,
                  // we get java.lang.Object instead of actual type.
                  // By doing it manually like this it works.
                  l(s"JsonObjectFormatVisitor - forcing array for ${prop}")

                  val itemType:JavaType = resolveType(prop, objectMapper)

                  childVisitor.expectArrayFormat(itemType).itemsFormat(null, itemType)
                } else if(classOf[Option[_]].isAssignableFrom(propertyType.getRawClass) && propertyType.containedTypeCount() >= 1) {

                  // Property is scala Option.
                  //
                  // Due to Java's Type Erasure, the type behind Option is lost.
                  // To workaround this, we use the same workaround as jackson-scala-module described here:
                  // https://github.com/FasterXML/jackson-module-scala/wiki/FAQ#deserializing-optionint-and-other-primitive-challenges

                  val optionType:JavaType = resolveType(prop, objectMapper)

                  objectMapper.acceptJsonFormatVisitor(optionType, childVisitor)

                } else {
                  objectMapper.acceptJsonFormatVisitor(propertyType, childVisitor)
                }

                // Check if we should set this property as required
                val rawClass = prop.getType.getRawClass
                val requiredProperty:Boolean = if ( rawClass.isPrimitive ) {
                  // primitive boolean MUST have a value
                  true
                } else if(prop.getAnnotation(classOf[NotNull]) != null) {
                  true
                } else {
                  false
                }

                if ( requiredProperty) {
                  getRequiredArrayNode(thisObjectNode).add(propertyName)
                }

                resolvePropertyFormat(prop).foreach {
                  format =>
                    thisPropertyNode.put("format", format)
                }

                // Optionally add description
                Option(prop.getAnnotation(classOf[JsonSchemaDescription])).map {
                  jsonSchemaDescription =>
                    thisPropertyNode.put("description", jsonSchemaDescription.value())
                }

                // Optionally add title
                Option(prop.getAnnotation(classOf[JsonSchemaTitle])).map(_.value())
                  .orElse {
                    if (autoGenerateTitleForProperties) {
                      // We should generate 'pretty-name' based on propertyName
                      Some(generateTitleFromPropertyName(propertyName))
                    } else None
                  }
                  .map {
                  title =>
                    thisPropertyNode.put("title", title)
                }

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

  def generateTitleFromPropertyName(propertyName:String):String = {
    // Code found here: http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
    val s = propertyName.replaceAll(
      String.format("%s|%s|%s",
        "(?<=[A-Z])(?=[A-Z][a-z])",
        "(?<=[^A-Z])(?=[A-Z])",
        "(?<=[A-Za-z])(?=[^A-Za-z])"
      ),
      " "
    )

    // Make the first letter uppercase
    s.substring(0,1).toUpperCase() + s.substring(1)
  }

  def resolvePropertyFormat(prop: BeanProperty):Option[String] = {
    // Prefer format specified in annotation
    Option(prop.getAnnotation(classOf[JsonSchemaFormat])).map {
      jsonSchemaFormat =>
        jsonSchemaFormat.value()
    }.orElse {
      // Try to resolve format from type
      clazz2FormatMapping.get( prop.getType.getRawClass )
    }
  }

  def resolveType(prop: BeanProperty, objectMapper: ObjectMapper):JavaType = {
    val containedType = prop.getType.containedType(0)

    if ( containedType.getRawClass == classOf[Object] ) {
      // try to resolve it via @JsonDeserialize as described here: https://github.com/FasterXML/jackson-module-scala/wiki/FAQ#deserializing-optionint-and-other-primitive-challenges
      Option(prop.getAnnotation(classOf[JsonDeserialize])).flatMap {
        jsonDeserialize:JsonDeserialize =>
          Option(jsonDeserialize.contentAs()).map {
            clazz =>
              objectMapper.getTypeFactory.uncheckedSimpleType(clazz)
          }
      }.getOrElse( {
        log.warn(s"$prop - Contained type is java.lang.Object and we're unable to extract its Type using fallback-approach looking for @JsonDeserialize")
        containedType
      })

    } else {
      // use containedType as is
      containedType
    }
  }


  def generateJsonSchema[T <: Any](clazz: Class[T]): JsonNode = {

    val rootNode = JsonNodeFactory.instance.objectNode()

    // Specify that this is a v4 json schema
    rootNode.put("$schema", JsonSchemaGenerator.JSON_SCHEMA_DRAFT_4_URL)
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
