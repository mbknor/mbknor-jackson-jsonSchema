package com.kjetland.jackson.jsonSchema

import java.lang.reflect.{Field, Method, ParameterizedType}
import java.time.{LocalDate, LocalDateTime, LocalTime, OffsetDateTime}
import java.util
import java.util.Optional
import javax.validation.constraints.{NotNull, Size}

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

object JsonSchemaConfig {

  val vanillaJsonSchemaDraft4 = JsonSchemaConfig(
    autoGenerateTitleForProperties = false,
    defaultArrayFormat = None,
    useOneOfForOption = false,
    usePropertyOrdering = false,
    hidePolymorphismTypeProperty = false,
    disableWarnings = false,
    useImprovedDateFormatMapping = false,
    useMinLengthForNotNull = false
  )

  /**
    * Use this configuration if using the JsonSchema to generate HTML5 GUI, eg. by using https://github.com/jdorn/json-editor
    *
    * autoGenerateTitleForProperties - If property is named "someName", we will add {"title": "Some Name"}
    * defaultArrayFormat - this will result in a better gui than te default one.

    */
  val html5EnabledSchema = JsonSchemaConfig(
    autoGenerateTitleForProperties = true,
    defaultArrayFormat = Some("table"),
    useOneOfForOption = true,
    usePropertyOrdering = true,
    hidePolymorphismTypeProperty = true,
    disableWarnings = false,
    useImprovedDateFormatMapping = true,
    useMinLengthForNotNull = true
  )

}

case class JsonSchemaConfig
(
  autoGenerateTitleForProperties:Boolean,
  defaultArrayFormat:Option[String],
  useOneOfForOption:Boolean,
  usePropertyOrdering:Boolean,
  hidePolymorphismTypeProperty:Boolean,
  disableWarnings:Boolean,
  useImprovedDateFormatMapping:Boolean,
  useMinLengthForNotNull:Boolean
)



/**
  * Json Schema Generator
  * @param rootObjectMapper pre-configured ObjectMapper
  * @param debug Default = false - set to true if generator should log some debug info while generating the schema
  * @param config default = vanillaJsonSchemaDraft4. Please use html5EnabledSchema if generating HTML5 GUI, e.g. using https://github.com/jdorn/json-editor
  */
class JsonSchemaGenerator
(
  val rootObjectMapper: ObjectMapper,
  debug:Boolean = false,
  config:JsonSchemaConfig = JsonSchemaConfig.vanillaJsonSchemaDraft4
) {

  // Java API
  def this(rootObjectMapper: ObjectMapper) = this(rootObjectMapper, false, JsonSchemaConfig.vanillaJsonSchemaDraft4)

  // Java API
  def this(rootObjectMapper: ObjectMapper, config:JsonSchemaConfig) = this(rootObjectMapper, false, config)

  import scala.collection.JavaConversions._

  val log = LoggerFactory.getLogger(getClass)

  val dateFormatMapping = Map[String,String](
    // Java7 dates
    "java.time.LocalDateTime" -> "datetime-local",
    "java.time.OffsetDateTime" -> "datetime",
    "java.time.LocalDate" -> "date",

    // Joda-dates
    "org.joda.time.LocalDate" -> "date"
  )

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

  private def setFormat(node:ObjectNode, format:String): Unit = {
    node.put("format", format)
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

  class MyJsonFormatVisitorWrapper
  (
    objectMapper: ObjectMapper,
    level:Int = 0,
    val node: ObjectNode = JsonNodeFactory.instance.objectNode(),
    val definitionsHandler:DefinitionsHandler,
    currentProperty:Option[BeanProperty] // This property may represent the BeanProperty when we're directly processing beneath the property
  ) extends JsonFormatVisitorWrapper with MySerializerProvider {

    def l(s: => String): Unit = {
      if (!debug) return

      var indent = ""
      for( i <- 0 until level) {
        indent = indent + "  "
      }
      println(indent + s)
    }

    def createChild(childNode: ObjectNode, currentProperty:Option[BeanProperty]): MyJsonFormatVisitorWrapper = {
      new MyJsonFormatVisitorWrapper(objectMapper, level + 1, node = childNode, definitionsHandler = definitionsHandler, currentProperty = currentProperty)
    }

    override def expectStringFormat(_type: JavaType) = {
      l(s"expectStringFormat - _type: ${_type}")

      node.put("type", "string")

      // Check if we should include minLength and/or maxLength
      case class MinAndMaxLength(minLength:Option[Int], maxLength:Option[Int])

      val minAndMaxLength:Option[MinAndMaxLength] = currentProperty.flatMap {
        p =>
          // Look for @Size
          Option(p.getAnnotation(classOf[Size]))
              .map {
                size =>
                  (size.min(), size.max()) match {
                    case (0, max)                 => MinAndMaxLength(None, Some(max))
                    case (min, Integer.MAX_VALUE) => MinAndMaxLength(Some(min), None)
                    case (min, max)               => MinAndMaxLength(Some(min), Some(max))
                  }
              }
              .orElse {
                // We did not find @Size - check if we should include it anyway
                if (config.useMinLengthForNotNull) {
                  Option(p.getAnnotation(classOf[NotNull])).map {
                    notNull =>
                      MinAndMaxLength(Some(1), None)
                  }
                } else None
              }
      }

      minAndMaxLength.map {
        minAndMax:MinAndMaxLength =>
          minAndMax.minLength.map( length => node.put("minLength", length) )
          minAndMax.maxLength.map( length => node.put("maxLength", length) )
      }

      new JsonStringFormatVisitor with EnumSupport {
        val _node = node
        override def format(format: JsonValueFormat): Unit = {
          setFormat(node, format.toString)
        }
      }

    }

    override def expectArrayFormat(_type: JavaType) = {
      l(s"expectArrayFormat - _type: ${_type}")

      node.put("type", "array")

      config.defaultArrayFormat.foreach {
        format => setFormat(node, format)
      }

      val itemsNode = JsonNodeFactory.instance.objectNode()
      node.set("items", itemsNode)

      // When processing scala modules we sometimes get a better elementType here than later in itemsFormat
      val preferredElementType:Option[JavaType] = if ( _type.containedTypeCount() >= 1) Some(_type.containedType(0)) else None

      new JsonArrayFormatVisitor with MySerializerProvider {
        override def itemsFormat(handler: JsonFormatVisitable, _elementType: JavaType): Unit = {
          l(s"expectArrayFormat - handler: $handler - elementType: ${_elementType} - preferredElementType: $preferredElementType")
          val elementType = preferredElementType.getOrElse(_elementType)
          objectMapper.acceptJsonFormatVisitor(elementType, createChild(itemsNode, currentProperty = None))
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
        override def format(format: JsonValueFormat): Unit = {
          setFormat(node, format.toString)
        }
      }
    }

    override def expectAnyFormat(_type: JavaType) = {
      if (!config.disableWarnings) {
        log.warn(s"Not able to generate jsonSchema-info for type: ${_type} - probably using custom serializer which does not override acceptJsonFormatVisitor")
      }


      new JsonAnyFormatVisitor {
      }

    }

    override def expectIntegerFormat(_type: JavaType) = {
      l("expectIntegerFormat")

      node.put("type", "integer")

      new JsonIntegerFormatVisitor with EnumSupport {
        val _node = node
        override def numberType(_type: NumberType): Unit = l(s"JsonIntegerFormatVisitor.numberType: ${_type}")
        override def format(format: JsonValueFormat): Unit = {
          setFormat(node, format.toString)
        }
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
        override def format(format: JsonValueFormat): Unit = {
          setFormat(node, format.toString)
        }
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

                val childVisitor = createChild(objectNode, currentProperty = None)
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
                setFormat(thisObjectNode, format)
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

                if (config.hidePolymorphismTypeProperty) {
                  // Make sure the editor hides this polymorphism-specific property
                  val optionsNode = JsonNodeFactory.instance.objectNode()
                  enumObjectNode.set("options", optionsNode)
                  optionsNode.put("hidden", true)
                }

                propertiesNode.set(pi.typePropertyName, enumObjectNode)

                getRequiredArrayNode(thisObjectNode).add(pi.typePropertyName)

            }

            Some(new JsonObjectFormatVisitor with MySerializerProvider {


              // Used when rendering schema using propertyOrdering as specified here:
              // https://github.com/jdorn/json-editor#property-ordering
              var nextPropertyOrderIndex = 1

              def myPropertyHandler(propertyName:String, propertyType:JavaType, prop: Option[BeanProperty], jsonPropertyRequired:Boolean): Unit = {
                l(s"JsonObjectFormatVisitor - ${propertyName}: ${propertyType}")

                if ( propertiesNode.get(propertyName) != null) {
                  if (!config.disableWarnings) {
                    log.warn(s"Ignoring property '$propertyName' in $propertyType since it has already been added, probably as type-property using polymorphism")
                  }
                  return
                }


                // Need to check for Option/Optional-special-case before we know what node to use here.

                case class PropertyNode(main:ObjectNode, meta:ObjectNode)

                val thisPropertyNode:PropertyNode = {
                  val thisPropertyNode = JsonNodeFactory.instance.objectNode()
                  propertiesNode.set(propertyName, thisPropertyNode)

                  if ( config.usePropertyOrdering ) {
                    thisPropertyNode.put("propertyOrder", nextPropertyOrderIndex)
                    nextPropertyOrderIndex = nextPropertyOrderIndex + 1
                  }

                  // Check for Option/Optional-special-case
                  if ( config.useOneOfForOption &&
                    (    classOf[Option[_]].isAssignableFrom(propertyType.getRawClass)
                      || classOf[Optional[_]].isAssignableFrom(propertyType.getRawClass)) ) {
                    // Need to special-case for property using Option/Optional
                    // Should insert oneOf between 'real one' and 'null'
                    val oneOfArray = JsonNodeFactory.instance.arrayNode()
                    thisPropertyNode.set("oneOf", oneOfArray)


                    // Create the one used when Option is empty
                    val oneOfNull = JsonNodeFactory.instance.objectNode()
                    oneOfNull.put("type", "null")
                    oneOfNull.put("title", "Not included")
                    oneOfArray.add(oneOfNull)

                    // Create the one used when Option is defined with the real value
                    val oneOfReal = JsonNodeFactory.instance.objectNode()
                    oneOfArray.add(oneOfReal)

                    // Return oneOfReal which, from now on, will be used as the node representing this property
                    PropertyNode(oneOfReal, thisPropertyNode)

                  } else {
                    // Not special-casing - using thisPropertyNode as is
                    PropertyNode(thisPropertyNode, thisPropertyNode)
                  }
                }

                // Continue processing this property

                val childVisitor = createChild(thisPropertyNode.main, currentProperty = prop)

                // Workaround for scala lists and so on
                if ( (propertyType.isArrayType || propertyType.isCollectionLikeType) && !classOf[Option[_]].isAssignableFrom(propertyType.getRawClass) && propertyType.containedTypeCount() >= 1) {
                  // If visiting a scala list and using default acceptJsonFormatVisitor-approach,
                  // we get java.lang.Object instead of actual type.
                  // By doing it manually like this it works.
                  l(s"JsonObjectFormatVisitor - forcing array for propertyName:$propertyName, propertyType: $propertyType")

                  val itemType:JavaType = resolveType(propertyType, prop, objectMapper)

                  childVisitor.expectArrayFormat(itemType).itemsFormat(null, itemType)
                } else if( (classOf[Option[_]].isAssignableFrom(propertyType.getRawClass) || classOf[Optional[_]].isAssignableFrom(propertyType.getRawClass) ) && propertyType.containedTypeCount() >= 1) {

                  // Property is scala Option or Java Optional.
                  //
                  // Due to Java's Type Erasure, the type behind Option is lost.
                  // To workaround this, we use the same workaround as jackson-scala-module described here:
                  // https://github.com/FasterXML/jackson-module-scala/wiki/FAQ#deserializing-optionint-and-other-primitive-challenges

                  val optionType:JavaType = resolveType(propertyType, prop, objectMapper)

                  objectMapper.acceptJsonFormatVisitor(optionType, childVisitor)

                } else {
                  objectMapper.acceptJsonFormatVisitor(propertyType, childVisitor)
                }

                // Check if we should set this property as required
                val rawClass = propertyType.getRawClass
                val requiredProperty:Boolean = if ( rawClass.isPrimitive ) {
                  // primitive boolean MUST have a value
                  true
                } else if( jsonPropertyRequired) {
                  // @JsonPropertyRequired is set to true
                  true
                } else if(prop.isDefined && prop.get.getAnnotation(classOf[NotNull]) != null) {
                  true
                } else {
                  false
                }

                if ( requiredProperty) {
                  getRequiredArrayNode(thisObjectNode).add(propertyName)
                }

                prop.flatMap( resolvePropertyFormat(_) ).foreach {
                  format =>
                    setFormat(thisPropertyNode.main, format)
                }

                // Optionally add description
                prop.flatMap {
                  p:BeanProperty =>
                    Option(p.getAnnotation(classOf[JsonSchemaDescription]))
                }.map {
                  jsonSchemaDescription =>
                    thisPropertyNode.meta.put("description", jsonSchemaDescription.value())
                }

                // Optionally add title
                prop.flatMap {
                  p:BeanProperty =>
                    Option(p.getAnnotation(classOf[JsonSchemaTitle]))
                }.map(_.value())
                  .orElse {
                    if (config.autoGenerateTitleForProperties) {
                      // We should generate 'pretty-name' based on propertyName
                      Some(generateTitleFromPropertyName(propertyName))
                    } else None
                  }
                  .map {
                    title =>
                      thisPropertyNode.meta.put("title", title)
                  }

              }

              override def optionalProperty(prop: BeanProperty): Unit = {
                l(s"JsonObjectFormatVisitor.optionalProperty: prop:${prop}")
                myPropertyHandler(prop.getName, prop.getType, Some(prop), jsonPropertyRequired = false)
              }

              override def optionalProperty(name: String, handler: JsonFormatVisitable, propertyTypeHint: JavaType): Unit = {
                l(s"JsonObjectFormatVisitor.optionalProperty: name:${name} handler:${handler} propertyTypeHint:${propertyTypeHint}")
                myPropertyHandler(name, propertyTypeHint, None, jsonPropertyRequired = false)
              }

              override def property(prop: BeanProperty): Unit = {
                l(s"JsonObjectFormatVisitor.property: prop:${prop}")
                myPropertyHandler(prop.getName, prop.getType, Some(prop), jsonPropertyRequired = true)
              }

              override def property(name: String, handler: JsonFormatVisitable, propertyTypeHint: JavaType): Unit = {
                l(s"JsonObjectFormatVisitor.property: name:${name} handler:${handler} propertyTypeHint:${propertyTypeHint}")
                myPropertyHandler(name, propertyTypeHint, None, jsonPropertyRequired = true)
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
      if( config.useImprovedDateFormatMapping ) {
        dateFormatMapping.get(prop.getType.getRawClass.getName)
      } else None
    }
  }

  def resolveType(propertyType:JavaType, prop: Option[BeanProperty], objectMapper: ObjectMapper):JavaType = {
    val containedType = propertyType.containedType(0)

    if ( containedType.getRawClass == classOf[Object] ) {
      // try to resolve it via @JsonDeserialize as described here: https://github.com/FasterXML/jackson-module-scala/wiki/FAQ#deserializing-optionint-and-other-primitive-challenges
      prop.flatMap {
        p:BeanProperty =>
          Option(p.getAnnotation(classOf[JsonDeserialize]))
      }.flatMap {
        jsonDeserialize:JsonDeserialize =>
          Option(jsonDeserialize.contentAs()).map {
            clazz =>
              objectMapper.getTypeFactory.uncheckedSimpleType(clazz)
          }
      }.getOrElse( {
        if (!config.disableWarnings) {
          log.warn(s"$prop - Contained type is java.lang.Object and we're unable to extract its Type using fallback-approach looking for @JsonDeserialize")
        }
        containedType
      })

    } else {
      // use containedType as is
      containedType
    }
  }


  def generateJsonSchema[T <: Any](clazz: Class[T], title:Option[String] = None, description:Option[String] = None): JsonNode = {

    val rootNode = JsonNodeFactory.instance.objectNode()

    // Specify that this is a v4 json schema
    rootNode.put("$schema", JsonSchemaGenerator.JSON_SCHEMA_DRAFT_4_URL)
    //rootNode.put("id", "http://my.site/myschema#")

    // Add schema title
    title.orElse {
      Some(generateTitleFromPropertyName(clazz.getSimpleName))
    }.flatMap {
      title =>
        // Skip it if specified to empty string
        if ( title.isEmpty) None else Some(title)
    }.map {
      title =>
        rootNode.put("title", title)
        // If root class is annotated with @JsonSchemaTitle, it will later override this title
    }

    // Maybe set schema description
    description.map {
      d =>
        rootNode.put("description", d)
        // If root class is annotated with @JsonSchemaDescription, it will later override this description
    }


    val definitionsHandler = new DefinitionsHandler
    val rootVisitor = new MyJsonFormatVisitorWrapper(rootObjectMapper, node = rootNode, definitionsHandler = definitionsHandler, currentProperty = None)
    rootObjectMapper.acceptJsonFormatVisitor(clazz, rootVisitor)

    definitionsHandler.getFinalDefinitionsNode().foreach {
      definitionsNode => rootNode.set("definitions", definitionsNode)
    }

    rootNode
  }

}
