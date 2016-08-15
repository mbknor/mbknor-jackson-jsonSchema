package com.kjetland.jackson.jsonSchema

import java.time.OffsetDateTime
import java.util
import java.util.{Optional, TimeZone}

import com.fasterxml.jackson.annotation.{JsonProperty, JsonSubTypes, JsonTypeInfo, JsonValue}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.{ArrayNode, MissingNode, NullNode, ObjectNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.kjetland.jackson.jsonSchema.testData._
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConversions._

class JsonSchemaGeneratorTest extends FunSuite with Matchers {

  val _objectMapper = new ObjectMapper()
  val _objectMapperScala = new ObjectMapper()
  _objectMapperScala.registerModule(new DefaultScalaModule)

  List(_objectMapper, _objectMapperScala).foreach {
    om =>
      val simpleModule = new SimpleModule()
      simpleModule.addSerializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerSerializer)
      simpleModule.addDeserializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerDeserializer)
      om.registerModule(simpleModule)

      om.registerModule(new JavaTimeModule)
      om.registerModule(new Jdk8Module )

      om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      om.setTimeZone(TimeZone.getDefault())
  }



  val jsonSchemaGenerator = new JsonSchemaGenerator(_objectMapper, debug = true)
  val jsonSchemaGeneratorHTML5 = new JsonSchemaGenerator(_objectMapper, debug = true, config = JsonSchemaConfig.html5EnabledSchema)
  val jsonSchemaGeneratorScala = new JsonSchemaGenerator(_objectMapperScala, debug = true)
  val jsonSchemaGeneratorScalaHTML5 = new JsonSchemaGenerator(_objectMapperScala, debug = true, config = JsonSchemaConfig.html5EnabledSchema)

  val testData = new TestData{}

  def asPrettyJson(node:JsonNode, om:ObjectMapper):String = {
    om.writerWithDefaultPrettyPrinter().writeValueAsString(node)
  }


  // Asserts that we're able to go from object => json => equal object
  def assertToFromJson(g:JsonSchemaGenerator, o:Any): JsonNode = {
    assertToFromJson(g, o, o.getClass)
  }

  // Asserts that we're able to go from object => json => equal object
  // deserType might be a class which o extends (polymorphism)
  def assertToFromJson(g:JsonSchemaGenerator, o:Any, deserType:Class[_]): JsonNode = {
    val json = g.rootObjectMapper.writeValueAsString(o)
    println(s"json: $json")
    val jsonNode = g.rootObjectMapper.readTree(json)
    val r = g.rootObjectMapper.treeToValue(jsonNode, deserType)
    assert( o == r)
    jsonNode
  }

  def useSchema(jsonSchema:JsonNode, jsonToTestAgainstSchema:Option[JsonNode] = None): Unit = {
    val schemaValidator = JsonSchemaFactory.byDefault().getJsonSchema(jsonSchema)
    jsonToTestAgainstSchema.foreach {
      node =>
        val r = schemaValidator.validate(node)
        if ( !r.isSuccess ) {
          throw new Exception("json does not validate agains schema: " + r)
        }

    }
  }

  // Generates schema, validates the schema using external schema validator and
  // Optionally tries to validate json against the schema.
  def generateAndValidateSchema(g:JsonSchemaGenerator, clazz:Class[_], jsonToTestAgainstSchema:Option[JsonNode] = None):JsonNode = {
    val schema = g.generateJsonSchema(clazz)

    println("--------------------------------------------")
    println(asPrettyJson(schema, g.rootObjectMapper))

    assert( JsonSchemaGenerator.JSON_SCHEMA_DRAFT_4_URL == schema.at("/$schema").asText())

    useSchema(schema, jsonToTestAgainstSchema)

    schema
  }

  def assertJsonSubTypesInfo(node:JsonNode, typeParamName:String, typeName:String): Unit ={
    /*
      "properties" : {
        "type" : {
          "type" : "string",
          "enum" : [ "child1" ],
          "default" : "child1"
        },
      },
      "title" : "child1",
      "required" : [ "type" ]
    */
    assert( node.at(s"/properties/$typeParamName/type").asText() == "string" )
    assert( node.at(s"/properties/$typeParamName/enum/0").asText() == typeName)
    assert( node.at(s"/properties/$typeParamName/default").asText() == typeName)
    assert( node.at(s"/title").asText() == typeName)
    assert( getRequiredList(node).contains(typeParamName))

  }

  def getArrayNodeAsListOfStrings(node:JsonNode):List[String] = {
    node match {
      case x:MissingNode => List()
      case x:ArrayNode   => x.toList.map(_.asText())
    }
  }

  def getRequiredList(node:JsonNode):List[String] = {
    getArrayNodeAsListOfStrings(node.at(s"/required"))
  }

  def getNodeViaArrayOfRefs(root:JsonNode, pathToArrayOfRefs:String, definitionName:String):JsonNode = {
    val nodeWhereArrayOfRefsIs:ArrayNode = root.at(pathToArrayOfRefs).asInstanceOf[ArrayNode]
    val arrayItemNodes = nodeWhereArrayOfRefsIs.iterator().toList
    val ref = arrayItemNodes.map(_.get("$ref").asText()).find( _.endsWith(s"/$definitionName")).get
    // use ref to look the node up
    val fixedRef = ref.substring(1) // Removing starting #
    root.at(fixedRef)
  }

  def getNodeViaRefs(root:JsonNode, nodeWithRef:JsonNode, definitionName:String):ObjectNode = {
    val ref = nodeWithRef.at("/$ref").asText()
    assert( ref.endsWith(s"/$definitionName"))
    // use ref to look the node up
    val fixedRef = ref.substring(1) // Removing starting #
    root.at(fixedRef).asInstanceOf[ObjectNode]
  }

  test("Generate scheme for plain class not using @JsonTypeInfo") {

    {

      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert( false == schema.at("/additionalProperties").asBoolean())
      assert( schema.at("/properties/someString/type").asText() == "string")

      assert( schema.at("/properties/myEnum/type").asText() == "string")
      assert( getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")) == List("A","B","C"))
    }

    {

      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.classNotExtendingAnythingScala)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.classNotExtendingAnythingScala.getClass, Some(jsonNode))

      assert( false == schema.at("/additionalProperties").asBoolean())
      assert( schema.at("/properties/someString/type").asText() == "string")

      assert( schema.at("/properties/myEnum/type").asText() == "string")
      assert( getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")) == List("A","B","C"))
      assert( getArrayNodeAsListOfStrings(schema.at("/properties/myEnumO/enum")) == List("A","B","C"))
    }

  }

  test("Generating schema for concrete class which happens to extend class using @JsonTypeInfo") {

    def doTest(pojo:Object, clazz:Class[_], g:JsonSchemaGenerator): Unit = {
      val jsonNode = assertToFromJson(g, pojo)
      val schema = generateAndValidateSchema(g, clazz, Some(jsonNode))

      assert( false == schema.at("/additionalProperties").asBoolean())
      assert( schema.at("/properties/parentString/type").asText() == "string")
      assertJsonSubTypesInfo(schema, "type", "child1")
    }

    doTest(testData.child1, testData.child1.getClass, jsonSchemaGenerator)
    doTest(testData.child1Scala, testData.child1Scala.getClass, jsonSchemaGeneratorScala)
  }

  test("Generate schema for regular class which has a property of class annotated with @JsonTypeInfo") {

    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoWithParent)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoWithParent.getClass, Some(jsonNode))

      assert(false == schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/pojoValue/type").asText() == "boolean")

      assertChild1(schema, "/properties/child/oneOf")
      assertChild2(schema, "/properties/child/oneOf")

    }

    // Scala
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.pojoWithParentScala)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.pojoWithParentScala.getClass, Some(jsonNode))

      assert(false == schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/pojoValue/type").asText() == "boolean")

      assertChild1(schema, "/properties/child/oneOf", "Child1Scala")
      assertChild2(schema, "/properties/child/oneOf", "Child2Scala")

    }

  }

  def assertChild1(node:JsonNode, path:String, defName:String = "Child1"): Unit ={
    val child1 = getNodeViaArrayOfRefs(node, path, defName)
    assertJsonSubTypesInfo(child1, "type", "child1")
    assert( child1.at("/properties/parentString/type").asText() == "string" )
    assert( child1.at("/properties/child1String/type").asText() == "string" )
    assert( child1.at("/properties/_child1String2/type").asText() == "string" )
    assert( child1.at("/properties/_child1String3/type").asText() == "string" )
    assert(getRequiredList(child1).contains("_child1String3"))
  }

  def assertChild2(node:JsonNode, path:String, defName:String = "Child2"): Unit ={
    val child2 = getNodeViaArrayOfRefs(node, path, defName)
    assertJsonSubTypesInfo(child2, "type", "child2")
    assert( child2.at("/properties/parentString/type").asText() == "string" )
    assert( child2.at("/properties/child2int/type").asText() == "integer" )
  }

  test("Generate schema for super class annotated with @JsonTypeInfo") {

    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.child1)
      assertToFromJson(jsonSchemaGenerator, testData.child1, classOf[Parent])

      val schema = generateAndValidateSchema(jsonSchemaGenerator, classOf[Parent], Some(jsonNode))

      assertChild1(schema, "/oneOf")
      assertChild2(schema, "/oneOf")
    }

    // Scala
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.child1Scala)
      assertToFromJson(jsonSchemaGeneratorScala, testData.child1Scala, classOf[ParentScala])

      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, classOf[ParentScala], Some(jsonNode))

      assertChild1(schema, "/oneOf", "Child1Scala")
      assertChild2(schema, "/oneOf", "Child2Scala")
    }

  }

  test("primitives") {

    // java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.manyPrimitives)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.manyPrimitives.getClass, Some(jsonNode))

      assert(schema.at("/properties/_string/type").asText() == "string")

      assert(schema.at("/properties/_integer/type").asText() == "integer")
      assert(!getRequiredList(schema).contains("_integer")) // Should allow null by default

      assert(schema.at("/properties/_int/type").asText() == "integer")
      assert(getRequiredList(schema).contains("_int")) // Must have a value

      assert(schema.at("/properties/_booleanObject/type").asText() == "boolean")
      assert(!getRequiredList(schema).contains("_booleanObject")) // Should allow null by default

      assert(schema.at("/properties/_booleanPrimitive/type").asText() == "boolean")
      assert(getRequiredList(schema).contains("_booleanPrimitive")) // Must be required since it must have true or false - not null

      assert(schema.at("/properties/_booleanObjectWithNotNull/type").asText() == "boolean")
      assert(getRequiredList(schema).contains("_booleanObjectWithNotNull"))

      assert(schema.at("/properties/_doubleObject/type").asText() == "number")
      assert(!getRequiredList(schema).contains("_doubleObject")) // Should allow null by default

      assert(schema.at("/properties/_doublePrimitive/type").asText() == "number")
      assert(getRequiredList(schema).contains("_doublePrimitive")) // Must be required since it must have a value - not null

      assert(schema.at("/properties/myEnum/type").asText() == "string")
      assert(getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")) == List("A", "B", "C"))
    }

    // scala
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.manyPrimitivesScala)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.manyPrimitivesScala.getClass, Some(jsonNode))

      assert(schema.at("/properties/_string/type").asText() == "string")

      assert(schema.at("/properties/_integer/type").asText() == "integer")
      assert(getRequiredList(schema).contains("_integer")) // Should allow null by default

      assert(schema.at("/properties/_boolean/type").asText() == "boolean")
      assert(getRequiredList(schema).contains("_boolean")) // Should allow null by default

      assert(schema.at("/properties/_double/type").asText() == "number")
      assert(getRequiredList(schema).contains("_double")) // Should allow null by default
    }
  }

  test("scala using option") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.pojoUsingOptionScala)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.pojoUsingOptionScala.getClass, Some(jsonNode))

    assert(schema.at("/properties/_string/type").asText() == "string")
    assert(!getRequiredList(schema).contains("_string")) // Should allow null by default

    assert(schema.at("/properties/_integer/type").asText() == "integer")
    assert(!getRequiredList(schema).contains("_integer")) // Should allow null by default

    assert(schema.at("/properties/_boolean/type").asText() == "boolean")
    assert(!getRequiredList(schema).contains("_boolean")) // Should allow null by default

    assert(schema.at("/properties/_double/type").asText() == "number")
    assert(!getRequiredList(schema).contains("_double")) // Should allow null by default

    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1"), "Child1Scala")

    assertJsonSubTypesInfo(child1, "type", "child1")
    assert( child1.at("/properties/parentString/type").asText() == "string" )
    assert( child1.at("/properties/child1String/type").asText() == "string" )
    assert( child1.at("/properties/_child1String2/type").asText() == "string" )
    assert( child1.at("/properties/_child1String3/type").asText() == "string" )

    assert(schema.at("/properties/optionalList/type").asText() == "array")
    assert(schema.at("/properties/optionalList/items/$ref").asText() == "#/definitions/ClassNotExtendingAnythingScala")

  }

  test("java using option") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingOptionalJava)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingOptionalJava.getClass, Some(jsonNode))

    assert(schema.at("/properties/_string/type").asText() == "string")
    assert(!getRequiredList(schema).contains("_string")) // Should allow null by default

    assert(schema.at("/properties/_integer/type").asText() == "integer")
    assert(!getRequiredList(schema).contains("_integer")) // Should allow null by default

    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1"), "Child1")

    assertJsonSubTypesInfo(child1, "type", "child1")
    assert( child1.at("/properties/parentString/type").asText() == "string" )
    assert( child1.at("/properties/child1String/type").asText() == "string" )
    assert( child1.at("/properties/_child1String2/type").asText() == "string" )
    assert( child1.at("/properties/_child1String3/type").asText() == "string" )

    assert(schema.at("/properties/optionalList/type").asText() == "array")
    assert(schema.at("/properties/optionalList/items/$ref").asText() == "#/definitions/ClassNotExtendingAnything")

  }

  test("custom serializer not overriding JsonSerializer.acceptJsonFormatVisitor") {

    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoWithCustomSerializer)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoWithCustomSerializer.getClass, Some(jsonNode))
    assert( schema.asInstanceOf[ObjectNode].fieldNames().toList == List("$schema")) // Empyt schema due to custom serializer
  }

  test("object with property using custom serializer not overriding JsonSerializer.acceptJsonFormatVisitor") {

    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.objectWithPropertyWithCustomSerializer)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.objectWithPropertyWithCustomSerializer.getClass, Some(jsonNode))
    assert( schema.at("/properties/s/type").asText() == "string")
    assert( schema.at("/properties/child").asInstanceOf[ObjectNode].fieldNames().toList == List())
  }


  test("pojoWithArrays") {

    def doTest(pojo:Object, clazz:Class[_], g:JsonSchemaGenerator): Unit ={

      val jsonNode = assertToFromJson(g, pojo)
      val schema = generateAndValidateSchema(g, clazz, Some(jsonNode))

      assert(schema.at("/properties/intArray1/type").asText() == "array")
      assert(schema.at("/properties/intArray1/items/type").asText() == "integer")

      assert(schema.at("/properties/stringArray/type").asText() == "array")
      assert(schema.at("/properties/stringArray/items/type").asText() == "string")

      assert(schema.at("/properties/stringList/type").asText() == "array")
      assert(schema.at("/properties/stringList/items/type").asText() == "string")

      assert(schema.at("/properties/polymorphismList/type").asText() == "array")
      assertChild1(schema, "/properties/polymorphismList/items/oneOf")
      assertChild2(schema, "/properties/polymorphismList/items/oneOf")

      assert(schema.at("/properties/polymorphismArray/type").asText() == "array")
      assertChild1(schema, "/properties/polymorphismArray/items/oneOf")
      assertChild2(schema, "/properties/polymorphismArray/items/oneOf")
    }

    doTest( testData.pojoWithArrays, testData.pojoWithArrays.getClass, jsonSchemaGenerator)
    doTest( testData.pojoWithArraysScala, testData.pojoWithArraysScala.getClass, jsonSchemaGeneratorScala)

  }

  test("recursivePojo") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.recursivePojo)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.recursivePojo.getClass, Some(jsonNode))

    assert( schema.at("/properties/myText/type").asText() == "string")

    assert( schema.at("/properties/children/type").asText() == "array")
    val defViaRef = getNodeViaRefs(schema, schema.at("/properties/children/items"), "RecursivePojo")

    assert( defViaRef.at("/properties/myText/type").asText() == "string")
    assert( defViaRef.at("/properties/children/type").asText() == "array")
    val defViaRef2 = getNodeViaRefs(schema, defViaRef.at("/properties/children/items"), "RecursivePojo")

    assert( defViaRef == defViaRef2)

  }

  test("pojo using Maps") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingMaps)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingMaps.getClass, Some(jsonNode))

    assert( schema.at("/properties/string2Integer/type").asText() == "object")
    assert( schema.at("/properties/string2Integer/additionalProperties").asBoolean() == true)

    assert( schema.at("/properties/string2String/type").asText() == "object")
    assert( schema.at("/properties/string2String/additionalProperties").asBoolean() == true)

    assert( schema.at("/properties/string2PojoUsingJsonTypeInfo/type").asText() == "object")
    assert( schema.at("/properties/string2PojoUsingJsonTypeInfo/additionalProperties").asBoolean() == true)
  }

  test("pojo Using Custom Annotations") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingFormat)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingFormat.getClass, Some(jsonNode))
    val schemaHTML5Date = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoUsingFormat.getClass, Some(jsonNode))

    assert( schema.at("/format").asText() == "grid")
    assert( schema.at("/description").asText() == "This is our pojo")
    assert( schema.at("/title").asText() == "Pojo using format")


    assert( schema.at("/properties/emailValue/type").asText() == "string")
    assert( schema.at("/properties/emailValue/format").asText() == "email")
    assert( schema.at("/properties/emailValue/description").asText() == "This is our email value")
    assert( schema.at("/properties/emailValue/title").asText() == "Email value")

    assert( schema.at("/properties/choice/type").asText() == "boolean")
    assert( schema.at("/properties/choice/format").asText() == "checkbox")

    assert( schema.at("/properties/dateTime/type").asText() == "string")
    assert( schema.at("/properties/dateTime/format").asText() == "date-time")
    assert( schemaHTML5Date.at("/properties/dateTime/format").asText() == "datetime-local")


    assert( schema.at("/properties/dateTimeWithAnnotation/type").asText() == "string")
    assert( schema.at("/properties/dateTimeWithAnnotation/format").asText() == "text")

    // Make sure autoGenerated title is correct
    assert( schemaHTML5Date.at("/properties/dateTimeWithAnnotation/title").asText() == "Date Time With Annotation")


  }

  test("scala using option with HTML5") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorScalaHTML5, testData.pojoUsingOptionScala)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorScalaHTML5, testData.pojoUsingOptionScala.getClass, Some(jsonNode))

    assert(schema.at("/properties/_string/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/_string/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/_string/oneOf/1/type").asText() == "string")
    assert(!getRequiredList(schema).contains("_string")) // Should allow null by default
    assert(schema.at("/properties/_string/title").asText() == "_string")

    assert(schema.at("/properties/_integer/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/_integer/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/_integer/oneOf/1/type").asText() == "integer")
    assert(!getRequiredList(schema).contains("_integer")) // Should allow null by default
    assert(schema.at("/properties/_integer/title").asText() == "_integer")

    assert(schema.at("/properties/_boolean/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/_boolean/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/_boolean/oneOf/1/type").asText() == "boolean")
    assert(!getRequiredList(schema).contains("_boolean")) // Should allow null by default
    assert(schema.at("/properties/_boolean/title").asText() == "_boolean")

    assert(schema.at("/properties/_double/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/_double/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/_double/oneOf/1/type").asText() == "number")
    assert(!getRequiredList(schema).contains("_double")) // Should allow null by default
    assert(schema.at("/properties/_double/title").asText() == "_double")

    assert(schema.at("/properties/child1/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/child1/oneOf/0/title").asText() == "Not included")
    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1Scala")
    assert(schema.at("/properties/child1/title").asText() == "Child 1")

    assertJsonSubTypesInfo(child1, "type", "child1")
    assert( child1.at("/properties/parentString/type").asText() == "string" )
    assert( child1.at("/properties/child1String/type").asText() == "string" )
    assert( child1.at("/properties/_child1String2/type").asText() == "string" )
    assert( child1.at("/properties/_child1String3/type").asText() == "string" )

    assert(schema.at("/properties/optionalList/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/optionalList/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/optionalList/oneOf/1/type").asText() == "array")
    assert(schema.at("/properties/optionalList/oneOf/1/items/$ref").asText() == "#/definitions/ClassNotExtendingAnythingScala")
    assert(schema.at("/properties/optionalList/title").asText() == "Optional List")
  }

  test("java using optional with HTML5") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.pojoUsingOptionalJava)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoUsingOptionalJava.getClass, Some(jsonNode))

    assert(schema.at("/properties/_string/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/_string/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/_string/oneOf/1/type").asText() == "string")
    assert(!getRequiredList(schema).contains("_string")) // Should allow null by default
    assert(schema.at("/properties/_string/title").asText() == "_string")

    assert(schema.at("/properties/_integer/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/_integer/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/_integer/oneOf/1/type").asText() == "integer")
    assert(!getRequiredList(schema).contains("_integer")) // Should allow null by default
    assert(schema.at("/properties/_integer/title").asText() == "_integer")


    assert(schema.at("/properties/child1/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/child1/oneOf/0/title").asText() == "Not included")
    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1")
    assert(schema.at("/properties/child1/title").asText() == "Child 1")

    assertJsonSubTypesInfo(child1, "type", "child1")
    assert( child1.at("/properties/parentString/type").asText() == "string" )
    assert( child1.at("/properties/child1String/type").asText() == "string" )
    assert( child1.at("/properties/_child1String2/type").asText() == "string" )
    assert( child1.at("/properties/_child1String3/type").asText() == "string" )

    assert(schema.at("/properties/optionalList/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/optionalList/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/optionalList/oneOf/1/type").asText() == "array")
    assert(schema.at("/properties/optionalList/oneOf/1/items/$ref").asText() == "#/definitions/ClassNotExtendingAnything")
    assert(schema.at("/properties/optionalList/title").asText() == "Optional List")
  }

  test("propertyOrdering") {
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert(schema.at("/properties/someString/propertyOrder").asInt() == 1)
      assert(schema.at("/properties/myEnum/propertyOrder").asInt() == 2)
    }

    // Make sure propertyOrder is not enabled when not using html5
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert(schema.at("/properties/someString/propertyOrder").isMissingNode == true)
    }
  }

}

trait TestData {
  import scala.collection.JavaConversions._
  val child1 = {
    val c = new Child1()
    c.parentString = "pv"
    c.child1String = "cs"
    c.child1String2 = "cs2"
    c.child1String3 = "cs3"
    c
  }

  val child1Scala = Child1Scala("pv", "cs", "cs2", "cs3")

  val child2 = {
    val c = new Child2()
    c.parentString = "pv"
    c.child2int = 12
    c
  }

  val child2Scala = Child2Scala("pv", 12)

  val pojoWithParent = {
    val p = new PojoWithParent
    p.pojoValue = true
    p.child = child1
    p
  }

  val pojoWithParentScala = PojoWithParentScala(true, child1Scala)

  val classNotExtendingAnything = {
    val o = new ClassNotExtendingAnything
    o.someString = "Something"
    o.myEnum = MyEnum.C
    o
  }

  val classNotExtendingAnythingScala = ClassNotExtendingAnythingScala("Something", MyEnum.C, Some(MyEnum.A))

  val manyPrimitives = new ManyPrimitives("s1", 1, 2, true, false, true, 0.1, 0.2, MyEnum.B)

  val manyPrimitivesScala = ManyPrimitivesScala("s1", 1, true, 0.1)

  val pojoUsingOptionScala = PojoUsingOptionScala(Some("s1"), Some(1), Some(true), Some(0.1), Some(child1Scala), Some(List(classNotExtendingAnythingScala)))

  val pojoUsingOptionalJava = new PojoUsingOptionalJava(Optional.of("s"), Optional.of(1), Optional.of(child1), Optional.of(util.Arrays.asList(classNotExtendingAnything)))

  val pojoWithCustomSerializer = {
    val p = new PojoWithCustomSerializer
    p.myString = "xxx"
    p
  }

  val objectWithPropertyWithCustomSerializer = new ObjectWithPropertyWithCustomSerializer("s1", pojoWithCustomSerializer)

  val pojoWithArrays = new PojoWithArrays(
    Array(1,2,3),
    Array("a1","a2","a3"),
    List("l1", "l2", "l3"),
    List(child1, child2),
    List(child1, child2).toArray,
    List(classNotExtendingAnything, classNotExtendingAnything)
  )

  val pojoWithArraysScala = PojoWithArraysScala(
    Some(List(1,2,3)),
    List("a1","a2","a3"),
    List("l1", "l2", "l3"),
    List(child1, child2),
    List(child1, child2),
    List(classNotExtendingAnything, classNotExtendingAnything)
  )

  val recursivePojo = new RecursivePojo("t1", List(new RecursivePojo("c1", null)))

  val pojoUsingMaps = new PojoUsingMaps(
      Map[String, Integer]("a" -> 1, "b" -> 2),
      Map("x" -> "y", "z" -> "w"),
      Map[String, Parent]("1" -> child1, "2" -> child2)
    )

  val pojoUsingFormat = new PojoUsingFormat("test@example.com", true, OffsetDateTime.now(), OffsetDateTime.now())

}


case class PojoWithArraysScala
(
  intArray1:Option[List[Integer]], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  stringArray:List[String], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  stringList:List[String],
  polymorphismList:List[Parent],
  polymorphismArray:List[Parent], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  regularObjectList:List[ClassNotExtendingAnything]
)

case class ClassNotExtendingAnythingScala(someString:String, myEnum: MyEnum, myEnumO: Option[MyEnum])

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(new JsonSubTypes.Type(value = classOf[Child1Scala], name = "child1"), new JsonSubTypes.Type(value = classOf[Child2Scala], name = "child2")))
trait ParentScala

case class Child1Scala
(
  parentString:String,
  child1String:String,

  @JsonProperty("_child1String2")
  child1String2:String,

  @JsonProperty(value = "_child1String3", required = true)
  child1String3:String
) extends ParentScala

case class Child2Scala(parentString:String, child2int:Int) extends ParentScala

case class PojoWithParentScala(pojoValue:Boolean, child:ParentScala)

case class ManyPrimitivesScala(_string:String, _integer:Int, _boolean:Boolean, _double:Double)

case class PojoUsingOptionScala(
                                 _string:Option[String],
                                 @JsonDeserialize(contentAs = classOf[Int])     _integer:Option[Int],
                                 @JsonDeserialize(contentAs = classOf[Boolean]) _boolean:Option[Boolean],
                                 @JsonDeserialize(contentAs = classOf[Double])  _double:Option[Double],
                                 child1:Option[Child1Scala],
                                 optionalList:Option[List[ClassNotExtendingAnythingScala]]
                                 //, parent:Option[ParentScala] - Not using this one: jackson-scala-module does not support Option combined with Polymorphism
                               )