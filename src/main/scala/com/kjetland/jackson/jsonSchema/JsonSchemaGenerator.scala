package com.kjetland.jackson.jsonSchema

import java.util
import java.util.Optional
import javax.validation.constraints._

import com.fasterxml.jackson.annotation.{JsonPropertyDescription, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.jsonFormatVisitors._
import com.fasterxml.jackson.databind.introspect.AnnotatedClass
import com.fasterxml.jackson.databind.node.{ArrayNode, JsonNodeFactory, ObjectNode}
import com.kjetland.jackson.jsonSchema.annotations._
import org.reflections.Reflections
import org.slf4j.LoggerFactory

object JsonSchemaGenerator {
  val JSON_SCHEMA_DRAFT_4_URL = "http://json-schema.org/draft-04/schema#"
}

object JsonSchemaConfig {

  val vanillaJsonSchemaDraft4 = JsonSchemaConfig(
    autoGenerateTitleForProperties = false,
    defaultArrayFormat = None,
    useOneOfForOption = false,
    useOneOfForNullables = false,
    usePropertyOrdering = false,
    hidePolymorphismTypeProperty = false,
    disableWarnings = false,
    useMinLengthForNotNull = false,
    useTypeIdForDefinitionName = false,
    customType2FormatMapping = Map(),
    useMultipleEditorSelectViaProperty = false,
    uniqueItemClasses = Set()
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
    useOneOfForNullables = false,
    usePropertyOrdering = true,
    hidePolymorphismTypeProperty = true,
    disableWarnings = false,
    useMinLengthForNotNull = true,
    useTypeIdForDefinitionName = false,
    customType2FormatMapping = Map[String,String](
      // Java7 dates
      "java.time.LocalDateTime" -> "datetime-local",
      "java.time.OffsetDateTime" -> "datetime",
      "java.time.LocalDate" -> "date",

      // Joda-dates
      "org.joda.time.LocalDate" -> "date"
    ),
    useMultipleEditorSelectViaProperty = true,
    uniqueItemClasses = Set(
      classOf[scala.collection.immutable.Set[_]],
      classOf[scala.collection.mutable.Set[_]],
      classOf[java.util.Set[_]]
    )
  )

  /**
    * This configuration is exactly like the vanilla JSON schema generator, except that "nullables" have been turned on:
    * `useOneOfForOption` and `useOneForNullables` have both been set to `true`.  With this configuration you can either
    * use `Optional` or `Option`, or a standard nullable Java type and get back a schema that allows nulls.
    *
    *
    * If you need to mix nullable and non-nullable types, you may override the nullability of the type by either setting
    * a `NotNull` annotation on the given property, or setting the `required` attribute of the `JsonProperty` annotation.
    */
  val nullableJsonSchemaDraft4 = JsonSchemaConfig (
    autoGenerateTitleForProperties = false,
    defaultArrayFormat = None,
    useOneOfForOption = true,
    useOneOfForNullables = true,
    usePropertyOrdering = false,
    hidePolymorphismTypeProperty = false,
    disableWarnings = false,
    useMinLengthForNotNull = false,
    useTypeIdForDefinitionName = false,
    customType2FormatMapping = Map(),
    useMultipleEditorSelectViaProperty = false,
    uniqueItemClasses = Set()
  )

  // Java-API
  def create(
              autoGenerateTitleForProperties:Boolean,
              defaultArrayFormat:Optional[String],
              useOneOfForOption:Boolean,
              useOneOfForNullables:Boolean,
              usePropertyOrdering:Boolean,
              hidePolymorphismTypeProperty:Boolean,
              disableWarnings:Boolean,
              useMinLengthForNotNull:Boolean,
              useTypeIdForDefinitionName:Boolean,
              customType2FormatMapping:java.util.Map[String, String],
              useMultipleEditorSelectViaProperty:Boolean,
              uniqueItemClasses:java.util.Set[Class[_]]
            ):JsonSchemaConfig = {

    import scala.collection.JavaConverters._

    JsonSchemaConfig(
      autoGenerateTitleForProperties,
      Option(defaultArrayFormat.orElse(null)),
      useOneOfForOption,
      useOneOfForNullables,
      usePropertyOrdering,
      hidePolymorphismTypeProperty,
      disableWarnings,
      useMinLengthForNotNull,
      useTypeIdForDefinitionName,
      customType2FormatMapping.asScala.toMap,
      useMultipleEditorSelectViaProperty,
      uniqueItemClasses.asScala.toSet
    )
  }

}

case class JsonSchemaConfig
(
  autoGenerateTitleForProperties:Boolean,
  defaultArrayFormat:Option[String],
  useOneOfForOption:Boolean,
  useOneOfForNullables:Boolean,
  usePropertyOrdering:Boolean,
  hidePolymorphismTypeProperty:Boolean,
  disableWarnings:Boolean,
  useMinLengthForNotNull:Boolean,
  useTypeIdForDefinitionName:Boolean,
  customType2FormatMapping:Map[String, String],
  useMultipleEditorSelectViaProperty:Boolean, // https://github.com/jdorn/json-editor/issues/709
  uniqueItemClasses:Set[Class[_]] // If rendering array and type is instanceOf class in this set, then we add 'uniqueItems": true' to schema - See // https://github.com/jdorn/json-editor for more info
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

  import scala.collection.JavaConverters._

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

      enums.asScala.foreach {
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
    private var workInProgress:Option[WorkInProgress] = None

    private var workInProgressStack = List[Option[WorkInProgress]]()

    def pushWorkInProgress(): Unit ={
      workInProgressStack = workInProgress :: workInProgressStack
      workInProgress = None
    }

    def popworkInProgress(): Unit ={
      workInProgress = workInProgressStack.head
      workInProgressStack = workInProgressStack.tail
    }


    def getDefinitionName (clazz:Class[_]) = { if (config.useTypeIdForDefinitionName) clazz.getName else clazz.getSimpleName }

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
          var shortRef = getDefinitionName(clazz)
          var longRef = "#/definitions/" + shortRef
          while( class2Ref.values.toList.contains(longRef)) {
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
          // Look for @Pattern
          Option(p.getAnnotation(classOf[Pattern])).map {
            pattern =>
              node.put("pattern", pattern.regexp())
          }

          // Look for @JsonSchemaDefault
          Option(p.getAnnotation(classOf[JsonSchemaDefault])).map {
            defaultValue =>
              node.put("default", defaultValue.value())
          }

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

      if (config.uniqueItemClasses.exists( c => _type.getRawClass.isAssignableFrom(c))) {
        // Adding '"uniqueItems": true' to be used with https://github.com/jdorn/json-editor
        node.put("uniqueItems", true)
        setFormat(node, "checkbox")
      } else {
        // Try to set default format
        config.defaultArrayFormat.foreach {
          format => setFormat(node, format)
        }
      }

      // Look for @Size
      currentProperty.map {
        p =>
          Option(p.getAnnotation(classOf[Size])).map {
            size =>
              node.put("minItems", size.min())
              node.put("maxItems", size.max())
          }
      }


      val itemsNode = JsonNodeFactory.instance.objectNode()
      node.set("items", itemsNode)

      // We get improved result while processing scala-collections by getting elementType this way
      // instead of using the one which we receive in JsonArrayFormatVisitor.itemsFormat
      // This approach also works for Java
      val preferredElementType:JavaType = _type.getContentType

      new JsonArrayFormatVisitor with MySerializerProvider {
        override def itemsFormat(handler: JsonFormatVisitable, _elementType: JavaType): Unit = {
          l(s"expectArrayFormat - handler: $handler - elementType: ${_elementType} - preferredElementType: $preferredElementType")
          objectMapper.acceptJsonFormatVisitor(preferredElementType, createChild(itemsNode, currentProperty = None))
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

      // Look for @Min, @Max => minumum, maximum
      currentProperty.map {
        p =>
          Option(p.getAnnotation(classOf[Min])).map {
            min =>
              node.put("minimum", min.value())
          }

          Option(p.getAnnotation(classOf[Max])).map {
            max =>
              node.put("maximum", max.value())
          }

          // Look for @JsonSchemaDefault
          Option(p.getAnnotation(classOf[JsonSchemaDefault])).map {
            defaultValue =>
              node.put("default", defaultValue.value().toLong )
          }
      }

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

      // Look for @Min, @Max => minumum, maximum
      currentProperty.map {
        p =>
          Option(p.getAnnotation(classOf[Min])).map {
            min =>
              node.put("minimum", min.value())
          }

          Option(p.getAnnotation(classOf[Max])).map {
            max =>
              node.put("maximum", max.value())
          }

          // Look for @JsonSchemaDefault
          Option(p.getAnnotation(classOf[JsonSchemaDefault])).map {
            defaultValue =>
              node.put("default", defaultValue.value().toInt)
          }
      }


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

      currentProperty.map {
        p =>
          // Look for @JsonSchemaDefault
          Option(p.getAnnotation(classOf[JsonSchemaDefault])).map {
            defaultValue =>
              node.put("default", defaultValue.value().toBoolean)
          }
      }

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

      val additionalPropsObject = JsonNodeFactory.instance.objectNode()
      node.set("additionalProperties", additionalPropsObject)

      definitionsHandler.pushWorkInProgress()

      val childVisitor = createChild(additionalPropsObject, None)
      objectMapper.acceptJsonFormatVisitor(_type.containedType(1), childVisitor)
      definitionsHandler.popworkInProgress()


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

    private def getOptionsNode(objectNode:ObjectNode):ObjectNode = {
      Option(objectNode.get("options")).map(_.asInstanceOf[ObjectNode]).getOrElse {
        val o = JsonNodeFactory.instance.objectNode()
        objectNode.set("options", o)
        o
      }
    }
    case class PolymorphismInfo(typePropertyName:String, subTypeName:String)

    private def extractPolymorphismInfo(_type:JavaType):Option[PolymorphismInfo] = {
      // look for @JsonTypeInfo
      val ac = AnnotatedClass.construct(_type, objectMapper.getDeserializationConfig())
      Option(ac.getAnnotations.get(classOf[JsonTypeInfo])).map {
        jsonTypeInfo =>

          jsonTypeInfo.include() match {
            case JsonTypeInfo.As.PROPERTY          => // supported
            case JsonTypeInfo.As.EXISTING_PROPERTY => // supported
            case x                                 => throw new Exception(s"We do not support polymorphism using jsonTypeInfo.include() = $x")
          }

          val propertyName = jsonTypeInfo.property()

          // Must find out what this current class should be called

          val subTypeName:String = jsonTypeInfo.use match {
            case JsonTypeInfo.Id.NAME =>
              objectMapper.getSubtypeResolver.collectAndResolveSubtypesByClass(objectMapper.getDeserializationConfig, ac).asScala.toList
                .filter(_.getType == _type.getRawClass)
                .find(p => true) // find first
                .get.getName

            case JsonTypeInfo.Id.CLASS => _type.getRawClass.getName
            case JsonTypeInfo.Id.MINIMAL_CLASS => "." + _type.getRawClass.getName
            case x => throw new Exception(s"Polymorphism using jsonTypeInfo.use == $x not supported")
          }

          PolymorphismInfo(propertyName, subTypeName)
      }

    }

    private def extractSubTypes(_type: JavaType):List[Class[_]] = {

      val ac = AnnotatedClass.construct(_type, objectMapper.getDeserializationConfig())

      Option(ac.getAnnotation(classOf[JsonTypeInfo])).map {
        jsonTypeInfo: JsonTypeInfo =>

          jsonTypeInfo.use() match {
            case JsonTypeInfo.Id.NAME =>
              // First we try to resolve types via manually finding annotations (if success, it will preserve the order), if not we fallback to use collectAndResolveSubtypesByClass()
              val subTypes: List[Class[_]] = Option(_type.getRawClass.getDeclaredAnnotation(classOf[JsonSubTypes])).map {
                ann: JsonSubTypes =>
                  // We found it via @JsonSubTypes-annotation
                  ann.value().map {
                    t: JsonSubTypes.Type => t.value()
                  }.toList
              }.getOrElse {
                // We did not find it via @JsonSubTypes-annotation (Probably since it is using mixin's) => Must fallback to using collectAndResolveSubtypesByClass
                val resolvedSubTypes = objectMapper.getSubtypeResolver.collectAndResolveSubtypesByClass(objectMapper.getDeserializationConfig, ac).asScala.toList
                resolvedSubTypes.map( _.getType)
                  .filter( c => _type.getRawClass.isAssignableFrom(c) && _type.getRawClass != c)
              }

              subTypes

            case JsonTypeInfo.Id.CLASS =>
              // Just find all subclasses
              new Reflections().getSubTypesOf(_type.getRawClass).asScala.toList
            case JsonTypeInfo.Id.MINIMAL_CLASS =>
              // Just find all subclasses
              new Reflections().getSubTypesOf(_type.getRawClass).asScala.toList

            case x => throw new Exception("We do not support @jsonTypeInfo.use = " + x)
          }

      }.getOrElse(List())
    }

    override def expectObjectFormat(_type: JavaType) = {

      val subTypes: List[Class[_]] = extractSubTypes(_type)

      // Check if we have subtypes
      if (subTypes.nonEmpty) {
        // We have subtypes
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

            // If class is annotated with JsonSchemaTitle, we should add it
            Option(subType.getDeclaredAnnotation(classOf[JsonSchemaTitle])).map(_.value()).foreach {
              title =>
                thisOneOfNode.put("title", title)
            }

            anyOfArrayNode.add(thisOneOfNode)

        }

        null // Returning null to stop jackson from visiting this object since we have done it manually

      } else {
        // We do not have subtypes

        val objectBuilder:ObjectNode => Option[JsonObjectFormatVisitor] = {
          thisObjectNode:ObjectNode =>

            thisObjectNode.put("type", "object")
            thisObjectNode.put("additionalProperties", false)

            // If class is annotated with JsonSchemaFormat, we should add it
            val ac = AnnotatedClass.construct(_type, objectMapper.getDeserializationConfig())
            resolvePropertyFormat(_type, objectMapper).foreach {
              format =>
                setFormat(thisObjectNode, format)
            }

            // If class is annotated with JsonSchemaDescription, we should add it
            Option(ac.getAnnotations.get(classOf[JsonSchemaDescription])).map(_.value())
              .orElse(Option(ac.getAnnotations.get(classOf[JsonPropertyDescription])).map(_.value))
              .foreach {
                description: String =>
                  thisObjectNode.put("description", description)
              }

            // If class is annotated with JsonSchemaTitle, we should add it
            Option(ac.getAnnotations.get(classOf[JsonSchemaTitle])).map(_.value()).foreach {
              title =>
                thisObjectNode.put("title", title)
            }

            // If class is annotated with JsonSchemaOptions, we should add it
            Option(ac.getAnnotations.get(classOf[JsonSchemaOptions])).map(_.items()).foreach {
              items =>
                val optionsNode = getOptionsNode(thisObjectNode)
                items.foreach {
                  item =>
                    optionsNode.put(item.name, item.value)
                }
            }

            // Optionally add JsonSchemaInject
            Option(ac.getAnnotations.get(classOf[JsonSchemaInject])).foreach {
              a =>
                // Must parse json
                val injectJsonNode = objectMapper.readTree(a.json())
                Option(a.jsonSupplier())
                  .flatMap(cls => Option(cls.newInstance().get()))
                  .foreach(json => merge(injectJsonNode, json))
                a.strings().foreach(v => injectJsonNode.visit(v.path(), (o, n) => o.put(n, v.value())))
                a.ints().foreach(v => injectJsonNode.visit(v.path(), (o, n) => o.put(n, v.value())))
                a.bools().foreach(v => injectJsonNode.visit(v.path(), (o, n) => o.put(n, v.value())))

                merge(thisObjectNode, injectJsonNode)
            }


            val propertiesNode = JsonNodeFactory.instance.objectNode()
            thisObjectNode.set("properties", propertiesNode)

            extractPolymorphismInfo(_type).map {
              case pi:PolymorphismInfo =>
                // This class is a child in a polymorphism config..
                // Set the title = subTypeName
                thisObjectNode.put("title", pi.subTypeName)

                // must inject the 'type'-param and value as enum with only one possible value
                // This is done to make sure the json generated from the schema using this oneOf
                // contains the correct "type info"
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

                if(config.useMultipleEditorSelectViaProperty) {
                  // https://github.com/jdorn/json-editor/issues/709
                  // Generate info to help generated editor to select correct oneOf-type
                  // when populating the gui/schema with existing data
                  val multipleEditorSelectViaPropertyNode = JsonNodeFactory.instance.objectNode()
                  multipleEditorSelectViaPropertyNode.put("property", pi.typePropertyName)
                  multipleEditorSelectViaPropertyNode.put("value", pi.subTypeName)

                  val objectOptionsNode = JsonNodeFactory.instance.objectNode()
                  objectOptionsNode.set("multiple_editor_select_via_property", multipleEditorSelectViaPropertyNode)
                  thisObjectNode.set("options", objectOptionsNode)
                }

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

                // Check if we should set this property as required
                val rawClass = propertyType.getRawClass
                val requiredProperty:Boolean = if (rawClass.isPrimitive) {
                  // primitive types MUST have a value
                  true
                } else if(jsonPropertyRequired || prop.isDefined && prop.get.getAnnotation(classOf[NotNull]) != null) {
                  // Has a @JsonProperty has "required" set to true, or is marked @NotNull
                  true
                } else {
                  false
                }

                val thisPropertyNode:PropertyNode = {
                  val thisPropertyNode = JsonNodeFactory.instance.objectNode()
                  propertiesNode.set(propertyName, thisPropertyNode)

                  if ( config.usePropertyOrdering ) {
                    thisPropertyNode.put("propertyOrder", nextPropertyOrderIndex)
                    nextPropertyOrderIndex = nextPropertyOrderIndex + 1
                  }

                  // Figure out if the type is considered optional by either the Java or Scala.
                  val optionalType:Boolean = classOf[Option[_]].isAssignableFrom(propertyType.getRawClass) ||
                                             classOf[Optional[_]].isAssignableFrom(propertyType.getRawClass)

                  // If the property is not required, and our configuration allows it, let's go ahead and mark the type as nullable.
                  if (!requiredProperty && ((config.useOneOfForOption && optionalType) ||
                                            (config.useOneOfForNullables && !optionalType))) {
                    // We support this type being null, insert a oneOf consisting of a sentinel "null" and the real type.
                    val oneOfArray = JsonNodeFactory.instance.arrayNode()
                    thisPropertyNode.set("oneOf", oneOfArray)

                    // Create our sentinel "null" value for the case no value is provided.
                    val oneOfNull = JsonNodeFactory.instance.objectNode()
                    oneOfNull.put("type", "null")
                    oneOfNull.put("title", "Not included")
                    oneOfArray.add(oneOfNull)

                    // If our nullable/optional type has a value, it'll be this.
                    val oneOfReal = JsonNodeFactory.instance.objectNode()
                    oneOfArray.add(oneOfReal)

                    // Return oneOfReal which, from now on, will be used as the node representing this property
                    PropertyNode(oneOfReal, thisPropertyNode)
                  } else {
                    // Our type must not be null: primitives, @NotNull annotations, @JsonProperty annotations marked required etc.
                    PropertyNode(thisPropertyNode, thisPropertyNode)
                  }
                }

                // Continue processing this property
                val childVisitor = createChild(thisPropertyNode.main, currentProperty = prop)


                // Push current work in progress since we're about to start working on a new class
                definitionsHandler.pushWorkInProgress()

                if( (classOf[Option[_]].isAssignableFrom(propertyType.getRawClass) || classOf[Optional[_]].isAssignableFrom(propertyType.getRawClass) ) && propertyType.containedTypeCount() >= 1) {

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

                // Pop back the work we were working on..
                definitionsHandler.popworkInProgress()

                prop.flatMap( resolvePropertyFormat(_) ).foreach {
                  format =>
                    setFormat(thisPropertyNode.main, format)
                }

                // Optionally add description
                prop.flatMap {
                  p: BeanProperty =>
                    Option(p.getAnnotation(classOf[JsonSchemaDescription])).map(_.value())
                      .orElse(Option(p.getAnnotation(classOf[JsonPropertyDescription])).map(_.value()))
                }.map {
                  description =>
                    thisPropertyNode.meta.put("description", description)
                }

                // If this property is required, add it to our array of required properties.
                if (requiredProperty) {
                  getRequiredArrayNode(thisObjectNode).add(propertyName)
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

                // Optionally add options
                prop.flatMap {
                  p:BeanProperty =>
                    Option(p.getAnnotation(classOf[JsonSchemaOptions]))
                }.map(_.items()).foreach {
                  items =>
                    val optionsNode = getOptionsNode(thisPropertyNode.meta)
                    items.foreach {
                      item =>
                        optionsNode.put(item.name, item.value)

                    }
                }

                // Optionally add JsonSchemaInject
                prop.flatMap {
                  p:BeanProperty =>
                    Option(p.getAnnotation(classOf[JsonSchemaInject]))
                }.foreach {
                  a =>
                    // Must parse json
                    val injectJsonNode = objectMapper.readTree(a.json())
                    Option(a.jsonSupplier())
                      .flatMap(cls => Option(cls.newInstance().get()))
                      .foreach(json => merge(injectJsonNode, json))
                    a.strings().foreach(v => injectJsonNode.visit(v.path(), (o, n) => o.put(n, v.value())))
                    a.ints().foreach(v => injectJsonNode.visit(v.path(), (o, n) => o.put(n, v.value())))
                    a.bools().foreach(v => injectJsonNode.visit(v.path(), (o, n) => o.put(n, v.value())))

                    merge(thisPropertyNode.meta, injectJsonNode)
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

  private def merge(mainNode:JsonNode, updateNode:JsonNode):Unit = {
    val fieldNames = updateNode.fieldNames()
    while (fieldNames.hasNext()) {

      val fieldName = fieldNames.next()
      val jsonNode = mainNode.get(fieldName)
      // if field exists and is an embedded object
      if (jsonNode != null && jsonNode.isObject()) {
        merge(jsonNode, updateNode.get(fieldName))
      }
      else {
        if (mainNode.isInstanceOf[ObjectNode]) {
          // Overwrite field
          val value = updateNode.get(fieldName)
          mainNode.asInstanceOf[ObjectNode].set(fieldName, value)
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

  def resolvePropertyFormat(_type: JavaType, objectMapper:ObjectMapper):Option[String] = {
    val ac = AnnotatedClass.construct(_type, objectMapper.getDeserializationConfig())
    resolvePropertyFormat(Option(ac.getAnnotation(classOf[JsonSchemaFormat])), _type.getRawClass.getName)
  }

  def resolvePropertyFormat(prop: BeanProperty):Option[String] = {
    // Prefer format specified in annotation
    resolvePropertyFormat(Option(prop.getAnnotation(classOf[JsonSchemaFormat])), prop.getType.getRawClass.getName)
  }

  def resolvePropertyFormat(jsonSchemaFormatAnnotation:Option[JsonSchemaFormat], rawClassName:String):Option[String] = {
    // Prefer format specified in annotation
    jsonSchemaFormatAnnotation.map {
      jsonSchemaFormat =>
        jsonSchemaFormat.value()
    }.orElse {
      config.customType2FormatMapping.get(rawClassName)
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

  def generateJsonSchema[T <: Any](clazz: Class[T]): JsonNode = generateJsonSchema(clazz, None, None)
  def generateJsonSchema[T <: Any](clazz: Class[T], title:String, description:String): JsonNode = generateJsonSchema(clazz, Option(title), Option(description))
  def generateJsonSchema[T <: Any](clazz: Class[T], title:Option[String], description:Option[String]): JsonNode = {

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

  implicit class JsonNodeExtension(o:JsonNode) {
    def visit(path: String, f: (ObjectNode, String) => Unit) = {
      var p = o

      val split = path.split('/')
      for (name <- split.dropRight(1)) {
        p = Option(p.get(name)).getOrElse(p.asInstanceOf[ObjectNode].putObject(name))
      }
      f(p.asInstanceOf[ObjectNode], split.last)
    }
  }
}
