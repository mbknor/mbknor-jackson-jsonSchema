package com.kjetland.jackson.jsonSchema

import java.time.{LocalDate, LocalDateTime, OffsetDateTime}
import java.util
import java.util.{Collections, Optional, TimeZone}

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.{ArrayNode, MissingNode, ObjectNode}
import com.fasterxml.jackson.databind.{JavaType, JsonNode, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.kjetland.jackson.jsonSchema.testData.GenericClass.GenericClassVoid
import com.kjetland.jackson.jsonSchema.testData.MapLike.GenericMapLike
import com.kjetland.jackson.jsonSchema.testData._
import com.kjetland.jackson.jsonSchema.testData.mixin.{MixinChild1, MixinModule, MixinParent}
import com.kjetland.jackson.jsonSchema.testData.polymorphism1.{Child1, Child2, Parent}
import com.kjetland.jackson.jsonSchema.testData.polymorphism2.polymorphism1.{Child21, Child22, Parent2}
import com.kjetland.jackson.jsonSchema.testData.polymorphism3.{Child31, Child32, Parent3}
import com.kjetland.jackson.jsonSchema.testData.polymorphism4.{Child41, Child42, Parent4}
import com.kjetland.jackson.jsonSchema.testDataScala._
import com.kjetland.jackson.jsonSchema.testData_issue_24.EntityWrapper
import io.github.classgraph.ClassGraph
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConverters._

class JsonSchemaGeneratorTest extends FunSuite with Matchers {

  val _objectMapper = new ObjectMapper()
  val _objectMapperScala = new ObjectMapper()
  _objectMapperScala.registerModule(new DefaultScalaModule)

  val mixinModule = new MixinModule

  List(_objectMapper, _objectMapperScala).foreach {
    om =>
      val simpleModule = new SimpleModule()
      simpleModule.addSerializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerSerializer)
      simpleModule.addDeserializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerDeserializer)
      om.registerModule(simpleModule)

      om.registerModule(new JavaTimeModule)
      om.registerModule(new Jdk8Module)
      om.registerModule(new JodaModule)

      // For the mixin-test
      om.registerModule(mixinModule)

      om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      om.setTimeZone(TimeZone.getDefault)
  }



  val jsonSchemaGenerator = new JsonSchemaGenerator(_objectMapper, debug = true)
  val jsonSchemaGeneratorHTML5 = new JsonSchemaGenerator(_objectMapper, debug = true, config = JsonSchemaConfig.html5EnabledSchema)
  val jsonSchemaGeneratorScala = new JsonSchemaGenerator(_objectMapperScala, debug = true)
  val jsonSchemaGeneratorScalaHTML5 = new JsonSchemaGenerator(_objectMapperScala, debug = true, config = JsonSchemaConfig.html5EnabledSchema)

  val vanillaJsonSchemaDraft4WithIds = JsonSchemaConfig.vanillaJsonSchemaDraft4.copy(useTypeIdForDefinitionName = true)
  val jsonSchemaGeneratorWithIds = new JsonSchemaGenerator(_objectMapperScala, debug = true, vanillaJsonSchemaDraft4WithIds)

  val jsonSchemaGeneratorNullable = new JsonSchemaGenerator(_objectMapper, debug = true, config = JsonSchemaConfig.nullableJsonSchemaDraft4)
  val jsonSchemaGeneratorHTML5Nullable = new JsonSchemaGenerator(_objectMapper, debug = true,
                                                                 config = JsonSchemaConfig.html5EnabledSchema.copy(useOneOfForNullables = true))
  val jsonSchemaGeneratorWithIdsNullable = new JsonSchemaGenerator(_objectMapperScala, debug = true,
                                                                   vanillaJsonSchemaDraft4WithIds.copy(useOneOfForNullables = true))

  val testData = new TestData{}

  def asPrettyJson(node:JsonNode, om:ObjectMapper):String = {
    om.writerWithDefaultPrettyPrinter().writeValueAsString(node)
  }


  // Asserts that we're able to go from object => json => equal object
  def assertToFromJson(g:JsonSchemaGenerator, o:Any): JsonNode = {
    assertToFromJson(g, o, o.getClass)
  }

  // Asserts that we're able to go from object => json => equal object
  // desiredType might be a class which o extends (polymorphism)
  def assertToFromJson(g:JsonSchemaGenerator, o:Any, desiredType:Class[_]): JsonNode = {
    val json = g.rootObjectMapper.writeValueAsString(o)
    println(s"json: $json")
    val jsonNode = g.rootObjectMapper.readTree(json)
    val r = g.rootObjectMapper.treeToValue(jsonNode, desiredType)
    assert(o == r)
    jsonNode
  }

  def useSchema(jsonSchema:JsonNode, jsonToTestAgainstSchema:Option[JsonNode] = None): Unit = {
    val schemaValidator = JsonSchemaFactory.byDefault().getJsonSchema(jsonSchema)
    jsonToTestAgainstSchema.foreach {
      node =>
        val r = schemaValidator.validate(node)
        if (!r.isSuccess) {
          throw new Exception("json does not validate against schema: " + r)
        }

    }
  }

  // Generates schema, validates the schema using external schema validator and
  // Optionally tries to validate json against the schema.
  def generateAndValidateSchema(g:JsonSchemaGenerator, clazz:Class[_], jsonToTestAgainstSchema:Option[JsonNode] = None):JsonNode = {
    val schema = g.generateJsonSchema(clazz)

    println("--------------------------------------------")
    println(asPrettyJson(schema, g.rootObjectMapper))

    assert(JsonSchemaGenerator.JSON_SCHEMA_DRAFT_4_URL == schema.at("/$schema").asText())

    useSchema(schema, jsonToTestAgainstSchema)

    schema
  }

  // Generates schema, validates the schema using external schema validator and
  // Optionally tries to validate json against the schema.
  def generateAndValidateSchemaUsingJavaType(g:JsonSchemaGenerator, javaType:JavaType, jsonToTestAgainstSchema:Option[JsonNode] = None):JsonNode = {
    val schema = g.generateJsonSchema(javaType)

    println("--------------------------------------------")
    println(asPrettyJson(schema, g.rootObjectMapper))

    assert(JsonSchemaGenerator.JSON_SCHEMA_DRAFT_4_URL == schema.at("/$schema").asText())

    useSchema(schema, jsonToTestAgainstSchema)

    schema
  }

  def assertJsonSubTypesInfo(node:JsonNode, typeParamName:String, typeName:String, html5Checks:Boolean = false): Unit ={
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
    assert(node.at(s"/properties/$typeParamName/type").asText() == "string")
    assert(node.at(s"/properties/$typeParamName/enum/0").asText() == typeName)
    assert(node.at(s"/properties/$typeParamName/default").asText() == typeName)
    assert(node.at(s"/title").asText() == typeName)
    assertPropertyRequired(node, typeParamName, required = true)

    if (html5Checks) {
      assert(node.at(s"/properties/$typeParamName/options/hidden").asBoolean())
      assert(node.at(s"/options/multiple_editor_select_via_property/property").asText() == typeParamName)
      assert(node.at(s"/options/multiple_editor_select_via_property/value").asText() == typeName)
    } else {
      assert(node.at(s"/options/multiple_editor_select_via_property/property").isInstanceOf[MissingNode])

    }
  }

  def getArrayNodeAsListOfStrings(node:JsonNode):List[String] = {
    node match {
      case _:MissingNode => List()
      case x:ArrayNode   => x.asScala.toList.map(_.asText())
    }
  }

  def getRequiredList(node:JsonNode):List[String] = {
    getArrayNodeAsListOfStrings(node.at(s"/required"))
  }

  def assertPropertyRequired(schema:JsonNode, propertyName:String, required:Boolean): Unit = {
    if (required) {
      assert(getRequiredList(schema).contains(propertyName))
    } else {
      assert(!getRequiredList(schema).contains(propertyName))
    }
  }

  def getNodeViaRefs(root:JsonNode, pathToArrayOfRefs:String, definitionName:String):JsonNode = {
    val arrayItemNodes:List[JsonNode] = root.at(pathToArrayOfRefs) match {
      case arrayNode:ArrayNode   => arrayNode.iterator().asScala.toList
      case objectNode:ObjectNode => List(objectNode)

    }
    val ref = arrayItemNodes.map(_.get("$ref").asText()).find(_.endsWith(s"/$definitionName")).get
    // use ref to look the node up
    val fixedRef = ref.substring(1) // Removing starting #
    root.at(fixedRef)
  }

  def getNodeViaRefs(root:JsonNode, nodeWithRef:JsonNode, definitionName:String):ObjectNode = {
    val ref = nodeWithRef.at("/$ref").asText()
    assert(ref.endsWith(s"/$definitionName"))
    // use ref to look the node up
    val fixedRef = ref.substring(1) // Removing starting #
    root.at(fixedRef).asInstanceOf[ObjectNode]
  }

  test("Generate scheme for plain class not using @JsonTypeInfo") {

    val enumList = MyEnum.values().toList.map(_.toString)

    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/someString/type").asText() == "string")

      assert(schema.at("/properties/myEnum/type").asText() == "string")
      assert(getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")) == enumList)
    }

    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assertNullableType(schema, "/properties/someString", "string")

      assertNullableType(schema, "/properties/myEnum", "string")
      assert(getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/oneOf/1/enum")) == enumList)
    }

    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.classNotExtendingAnythingScala)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.classNotExtendingAnythingScala.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/someString/type").asText() == "string")

      assert(schema.at("/properties/myEnum/type").asText() == "string")
      assert(getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")) == enumList)
      assert(getArrayNodeAsListOfStrings(schema.at("/properties/myEnumO/enum")) == enumList)
    }
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.genericClassVoid)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.genericClassVoid.getClass, Some(jsonNode))
      assert(schema.at("/type").asText() == "object")
      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/content/type").asText() == "null")
      assert(schema.at("/properties/list/type").asText() == "array")
      assert(schema.at("/properties/list/items/type").asText() == "null")
    }
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.genericMapLike)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.genericMapLike.getClass, Some(jsonNode))
      assert(schema.at("/type").asText() == "object")
      assert(schema.at("/additionalProperties/type").asText() == "string")
    }
  }

  test("Generating schema for concrete class which happens to extend class using @JsonTypeInfo") {

    def doTest(pojo:Object, clazz:Class[_], g:JsonSchemaGenerator): Unit = {
      val jsonNode = assertToFromJson(g, pojo)
      val schema = generateAndValidateSchema(g, clazz, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/parentString/type").asText() == "string")
      assertJsonSubTypesInfo(schema, "type", "child1")
    }

    doTest(testData.child1, testData.child1.getClass, jsonSchemaGenerator)
    doTest(testData.child1Scala, testData.child1Scala.getClass, jsonSchemaGeneratorScala)
  }

  test("Generate schema for regular class which has a property of class annotated with @JsonTypeInfo") {

    def assertDefaultValues(schema:JsonNode): Unit ={
      assert(schema.at("/properties/stringWithDefault/type").asText() == "string")
      assert(schema.at("/properties/stringWithDefault/default").asText() == "x")
      assert(schema.at("/properties/intWithDefault/type").asText() == "integer")
      assert(schema.at("/properties/intWithDefault/default").asInt() == 12)
      assert(schema.at("/properties/booleanWithDefault/type").asText() == "boolean")
      assert(schema.at("/properties/booleanWithDefault/default").asBoolean())
    }

    def assertNullableDefaultValues(schema:JsonNode): Unit = {
      assert(schema.at("/properties/stringWithDefault/oneOf/0/type").asText() == "null")
      assert(schema.at("/properties/stringWithDefault/oneOf/0/title").asText() == "Not included")
      assert(schema.at("/properties/stringWithDefault/oneOf/1/type").asText() == "string")
      assert(schema.at("/properties/stringWithDefault/oneOf/1/default").asText() == "x")

      assert(schema.at("/properties/intWithDefault/type").asText() == "integer")
      assert(schema.at("/properties/intWithDefault/default").asInt() == 12)
      assert(schema.at("/properties/booleanWithDefault/type").asText() == "boolean")
      assert(schema.at("/properties/booleanWithDefault/default").asBoolean())
    }

    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoWithParent)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoWithParent.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/pojoValue/type").asText() == "boolean")
      assertDefaultValues(schema)

      assertChild1(schema, "/properties/child/oneOf")
      assertChild2(schema, "/properties/child/oneOf")
    }

    // Java - html5
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.pojoWithParent)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoWithParent.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/pojoValue/type").asText() == "boolean")
      assertDefaultValues(schema)

      assertChild1(schema, "/properties/child/oneOf", html5Checks = true)
      assertChild2(schema, "/properties/child/oneOf", html5Checks = true)
    }

    // Java - html5/nullable
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5Nullable, testData.pojoWithParent)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.pojoWithParent.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assertNullableType(schema, "/properties/pojoValue", "boolean")
      assertNullableDefaultValues(schema)

      assertNullableChild1(schema, "/properties/child/oneOf/1/oneOf", html5Checks = true)
      assertNullableChild2(schema, "/properties/child/oneOf/1/oneOf", html5Checks = true)
    }

    //Using fully-qualified class names
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorWithIds, testData.pojoWithParent)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorWithIds, testData.pojoWithParent.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/pojoValue/type").asText() == "boolean")
      assertDefaultValues(schema)

      assertChild1(schema, "/properties/child/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child1")
      assertChild2(schema, "/properties/child/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child2")
    }

    // Using fully-qualified class names and nullable types
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorWithIdsNullable, testData.pojoWithParent)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorWithIdsNullable, testData.pojoWithParent.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assertNullableType(schema, "/properties/pojoValue", "boolean")
      assertNullableDefaultValues(schema)

      assertNullableChild1(schema, "/properties/child/oneOf/1/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child1")
      assertNullableChild2(schema, "/properties/child/oneOf/1/oneOf", "com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child2")
    }

    // Scala
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.pojoWithParentScala)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.pojoWithParentScala.getClass, Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/pojoValue/type").asText() == "boolean")
      assertDefaultValues(schema)

      assertChild1(schema, "/properties/child/oneOf", "Child1Scala")
      assertChild2(schema, "/properties/child/oneOf", "Child2Scala")
    }
  }

  def assertChild1(node:JsonNode, path:String, defName:String = "Child1", typeParamName:String = "type", typeName:String = "child1", html5Checks:Boolean = false): Unit ={
    val child1 = getNodeViaRefs(node, path, defName)
    assertJsonSubTypesInfo(child1, typeParamName, typeName, html5Checks)
    assert(child1.at("/properties/parentString/type").asText() == "string")
    assert(child1.at("/properties/child1String/type").asText() == "string")
    assert(child1.at("/properties/_child1String2/type").asText() == "string")
    assert(child1.at("/properties/_child1String3/type").asText() == "string")
    assertPropertyRequired(child1, "_child1String3", required = true)
  }

  def assertNullableChild1(node:JsonNode, path:String, defName:String = "Child1", html5Checks:Boolean = false): Unit ={
    val child1 = getNodeViaRefs(node, path, defName)
    assertJsonSubTypesInfo(child1, "type", "child1", html5Checks)
    assertNullableType(child1, "/properties/parentString", "string")
    assertNullableType(child1, "/properties/child1String", "string")
    assertNullableType(child1, "/properties/_child1String2", "string")
    assert(child1.at("/properties/_child1String3/type").asText() == "string")
    assertPropertyRequired(child1, "_child1String3", required = true)
  }

  def assertChild2(node:JsonNode, path:String, defName:String = "Child2", typeParamName:String = "type", typeName:String = "child2", html5Checks:Boolean = false): Unit ={
    val child2 = getNodeViaRefs(node, path, defName)
    assertJsonSubTypesInfo(child2, typeParamName, typeName, html5Checks)
    assert(child2.at("/properties/parentString/type").asText() == "string")
    assert(child2.at("/properties/child2int/type").asText() == "integer")
  }

  def assertNullableChild2(node:JsonNode, path:String, defName:String = "Child2", html5Checks:Boolean = false): Unit = {
    val child2 = getNodeViaRefs(node, path, defName)
    assertJsonSubTypesInfo(child2, "type", "child2", html5Checks)
    assertNullableType(child2, "/properties/parentString", "string")
    assertNullableType(child2, "/properties/child2int", "integer")
  }

  def assertNullableType(node:JsonNode, path:String, expectedType:String): Unit = {
    val nullType = node.at(path).at("/oneOf/0")
    assert(nullType.at("/type").asText() == "null")
    assert(nullType.at("/title").asText() == "Not included")

    val valueType = node.at(path).at("/oneOf/1")
    assert(valueType.at("/type").asText() == expectedType)

    Option(getRequiredList(node)).map(xs => assert(!xs.contains(path.split('/').last)))
  }

  test("Generate schema for super class annotated with @JsonTypeInfo - use = JsonTypeInfo.Id.NAME") {

    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.child1)
      assertToFromJson(jsonSchemaGenerator, testData.child1, classOf[Parent])

      val schema = generateAndValidateSchema(jsonSchemaGenerator, classOf[Parent], Some(jsonNode))

      assertChild1(schema, "/oneOf")
      assertChild2(schema, "/oneOf")
    }

    // Java + Nullables
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.child1)
      assertToFromJson(jsonSchemaGeneratorNullable, testData.child1, classOf[Parent])

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

  test("Generate schema for super class annotated with @JsonTypeInfo - use = JsonTypeInfo.Id.CLASS") {

    // Java
    {

      val config = JsonSchemaConfig.vanillaJsonSchemaDraft4
      val g = new JsonSchemaGenerator(_objectMapper, debug = true, config)

      val jsonNode = assertToFromJson(g, testData.child21)
      assertToFromJson(g, testData.child21, classOf[Parent2])

      val schema = generateAndValidateSchema(g, classOf[Parent2], Some(jsonNode))

      assertChild1(schema, "/oneOf", "Child21", typeParamName = "clazz", typeName = "com.kjetland.jackson.jsonSchema.testData.polymorphism2.polymorphism1.Child21")
      assertChild2(schema, "/oneOf", "Child22", typeParamName = "clazz", typeName = "com.kjetland.jackson.jsonSchema.testData.polymorphism2.polymorphism1.Child22")
    }
  }

  test("Generate schema for super class annotated with @JsonTypeInfo - include = JsonTypeInfo.As.EXISTING_PROPERTY") {

    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.child31)
      assertToFromJson(jsonSchemaGenerator, testData.child31, classOf[Parent3])

      val schema = generateAndValidateSchema(jsonSchemaGenerator, classOf[Parent3], Some(jsonNode))

      assertChild1(schema, "/oneOf", "Child31", typeName = "child31")
      assertChild2(schema, "/oneOf", "Child32", typeName = "child32")
    }
  }

  test("Generate schema for super class annotated with @JsonTypeInfo - include = JsonTypeInfo.As.CUSTOM") {

    // Java
    {

      val jsonNode1 = assertToFromJson(jsonSchemaGenerator, testData.child41)
      val jsonNode2 = assertToFromJson(jsonSchemaGenerator, testData.child42)

      val schema1 = generateAndValidateSchema(jsonSchemaGenerator, classOf[Child41], Some(jsonNode1))
      val schema2 = generateAndValidateSchema(jsonSchemaGenerator, classOf[Child42], Some(jsonNode2))

      assertJsonSubTypesInfo(schema1, "type", "Child41")
      assertJsonSubTypesInfo(schema2, "type", "Child42")
    }
  }

  test("primitives") {

    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.manyPrimitives)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.manyPrimitives.getClass, Some(jsonNode))

      assert(schema.at("/properties/_string/type").asText() == "string")

      assert(schema.at("/properties/_integer/type").asText() == "integer")
      assertPropertyRequired(schema, "_integer", required = false) // Should allow null by default

      assert(schema.at("/properties/_int/type").asText() == "integer")
      assertPropertyRequired(schema, "_int", required = true) // Must have a value

      assert(schema.at("/properties/_booleanObject/type").asText() == "boolean")
      assertPropertyRequired(schema, "_booleanObject", required = false) // Should allow null by default

      assert(schema.at("/properties/_booleanPrimitive/type").asText() == "boolean")
      assertPropertyRequired(schema, "_booleanPrimitive", required = true) // Must be required since it must have true or false - not null

      assert(schema.at("/properties/_booleanObjectWithNotNull/type").asText() == "boolean")
      assertPropertyRequired(schema, "_booleanObjectWithNotNull", required = true)

      assert(schema.at("/properties/_doubleObject/type").asText() == "number")
      assertPropertyRequired(schema, "_doubleObject", required = false)// Should allow null by default

      assert(schema.at("/properties/_doublePrimitive/type").asText() == "number")
      assertPropertyRequired(schema, "_doublePrimitive", required = true) // Must be required since it must have a value - not null

      assert(schema.at("/properties/myEnum/type").asText() == "string")
      assert(getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")) == MyEnum.values().toList.map(_.toString))
      assert(schema.at("/properties/myEnum/JsonSchemaInjectOnEnum").asText() == "true")
    }

    // Java with nullable types
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.manyPrimitivesNulls)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.manyPrimitivesNulls.getClass, Some(jsonNode))

      assertNullableType(schema, "/properties/_string", "string")
      assertNullableType(schema, "/properties/_integer", "integer")
      assertNullableType(schema, "/properties/_booleanObject", "boolean")
      assertNullableType(schema, "/properties/_doubleObject", "number")

      // We're actually going to test this elsewhere, because if we set this to null here it'll break the "generateAndValidateSchema"
      // test. What's fun is that the type system will allow you to set the value as null, but the schema won't (because there's a @NotNull annotation on it).
      assert(schema.at("/properties/_booleanObjectWithNotNull/type").asText() == "boolean")
      assertPropertyRequired(schema, "_booleanObjectWithNotNull", required = true)

      assert(schema.at("/properties/_int/type").asText() == "integer")
      assertPropertyRequired(schema, "_int", required = true)

      assert(schema.at("/properties/_booleanPrimitive/type").asText() == "boolean")
      assertPropertyRequired(schema, "_booleanPrimitive", required = true)

      assert(schema.at("/properties/_doublePrimitive/type").asText() == "number")
      assertPropertyRequired(schema, "_doublePrimitive", required = true)

      assertNullableType(schema, "/properties/myEnum", "string")
      assert(getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/oneOf/1/enum")) == MyEnum.values().toList.map(_.toString))
    }

    // Scala
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.manyPrimitivesScala)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.manyPrimitivesScala.getClass, Some(jsonNode))

      assert(schema.at("/properties/_string/type").asText() == "string")

      assert(schema.at("/properties/_integer/type").asText() == "integer")
      assertPropertyRequired(schema, "_integer", required = true) // Should allow null by default

      assert(schema.at("/properties/_boolean/type").asText() == "boolean")
      assertPropertyRequired(schema, "_boolean", required = true) // Should allow null by default

      assert(schema.at("/properties/_double/type").asText() == "number")
      assertPropertyRequired(schema, "_double", required = true) // Should allow null by default
    }
  }

  test("scala using option") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.pojoUsingOptionScala)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorScala, testData.pojoUsingOptionScala.getClass, Some(jsonNode))

    assert(schema.at("/properties/_string/type").asText() == "string")
    assertPropertyRequired(schema, "_string", required = false) // Should allow null by default

    assert(schema.at("/properties/_integer/type").asText() == "integer")
    assertPropertyRequired(schema, "_integer", required = false) // Should allow null by default

    assert(schema.at("/properties/_boolean/type").asText() == "boolean")
    assertPropertyRequired(schema, "_boolean", required = false) // Should allow null by default

    assert(schema.at("/properties/_double/type").asText() == "number")
    assertPropertyRequired(schema, "_double", required = false) // Should allow null by default

    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1"), "Child1Scala")

    assertJsonSubTypesInfo(child1, "type", "child1")
    assert(child1.at("/properties/parentString/type").asText() == "string")
    assert(child1.at("/properties/child1String/type").asText() == "string")
    assert(child1.at("/properties/_child1String2/type").asText() == "string")
    assert(child1.at("/properties/_child1String3/type").asText() == "string")

    assert(schema.at("/properties/optionalList/type").asText() == "array")
    assert(schema.at("/properties/optionalList/items/$ref").asText() == "#/definitions/ClassNotExtendingAnythingScala")
  }

  test("java using option") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingOptionalJava)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingOptionalJava.getClass, Some(jsonNode))

    assert(schema.at("/properties/_string/type").asText() == "string")
    assertPropertyRequired(schema, "_string", required = false) // Should allow null by default

    assert(schema.at("/properties/_integer/type").asText() == "integer")
    assertPropertyRequired(schema, "_integer", required = false) // Should allow null by default

    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1"), "Child1")

    assertJsonSubTypesInfo(child1, "type", "child1")
    assert(child1.at("/properties/parentString/type").asText() == "string")
    assert(child1.at("/properties/child1String/type").asText() == "string")
    assert(child1.at("/properties/_child1String2/type").asText() == "string")
    assert(child1.at("/properties/_child1String3/type").asText() == "string")

    assert(schema.at("/properties/optionalList/type").asText() == "array")
    assert(schema.at("/properties/optionalList/items/$ref").asText() == "#/definitions/ClassNotExtendingAnything")
  }

  test("nullable Java using option") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.pojoUsingOptionalJava)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.pojoUsingOptionalJava.getClass, Some(jsonNode))

    assertNullableType(schema, "/properties/_string", "string")
    assertNullableType(schema, "/properties/_integer", "integer")

    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1")

    assertJsonSubTypesInfo(child1, "type", "child1")
    assertNullableType(child1, "/properties/parentString", "string")
    assertNullableType(child1, "/properties/child1String", "string")
    assertNullableType(child1, "/properties/_child1String2", "string")
    assert(child1.at("/properties/_child1String3/type").asText() == "string")

    assertNullableType(schema, "/properties/optionalList", "array")
    assert(schema.at("/properties/optionalList/oneOf/1/items/$ref").asText() == "#/definitions/ClassNotExtendingAnything")
  }

  test("custom serializer not overriding JsonSerializer.acceptJsonFormatVisitor") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoWithCustomSerializer)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoWithCustomSerializer.getClass, Some(jsonNode))
    assert(schema.asInstanceOf[ObjectNode].fieldNames().asScala.toList == List("$schema", "title")) // Empty schema due to custom serializer
  }

  test("object with property using custom serializer not overriding JsonSerializer.acceptJsonFormatVisitor") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.objectWithPropertyWithCustomSerializer)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.objectWithPropertyWithCustomSerializer.getClass, Some(jsonNode))
    assert(schema.at("/properties/s/type").asText() == "string")
    assert(schema.at("/properties/child").asInstanceOf[ObjectNode].fieldNames().asScala.toList == List())
  }

  test("pojoWithArrays") {

    def doTest(pojo:Object, clazz:Class[_], g:JsonSchemaGenerator, html5Checks:Boolean): Unit ={

      val jsonNode = assertToFromJson(g, pojo)
      val schema = generateAndValidateSchema(g, clazz, Some(jsonNode))

      assert(schema.at("/properties/intArray1/type").asText() == "array")
      assert(schema.at("/properties/intArray1/items/type").asText() == "integer")

      assert(schema.at("/properties/stringArray/type").asText() == "array")
      assert(schema.at("/properties/stringArray/items/type").asText() == "string")

      assert(schema.at("/properties/stringList/type").asText() == "array")
      assert(schema.at("/properties/stringList/items/type").asText() == "string")
      assert(schema.at("/properties/stringList/minItems").asInt() == 1)
      assert(schema.at("/properties/stringList/maxItems").asInt() == 10)

      assert(schema.at("/properties/polymorphismList/type").asText() == "array")
      assertChild1(schema, "/properties/polymorphismList/items/oneOf", html5Checks = html5Checks)
      assertChild2(schema, "/properties/polymorphismList/items/oneOf", html5Checks = html5Checks)

      assert(schema.at("/properties/polymorphismArray/type").asText() == "array")
      assertChild1(schema, "/properties/polymorphismArray/items/oneOf", html5Checks = html5Checks)
      assertChild2(schema, "/properties/polymorphismArray/items/oneOf", html5Checks = html5Checks)

      assert(schema.at("/properties/listOfListOfStrings/type").asText() == "array")
      assert(schema.at("/properties/listOfListOfStrings/items/type").asText() == "array")
      assert(schema.at("/properties/listOfListOfStrings/items/items/type").asText() == "string")

      assert(schema.at("/properties/setOfUniqueValues/type").asText() == "array")
      assert(schema.at("/properties/setOfUniqueValues/items/type").asText() == "string")

      if (html5Checks) {
        assert(schema.at("/properties/setOfUniqueValues/uniqueItems").asText() == "true")
        assert(schema.at("/properties/setOfUniqueValues/format").asText() == "checkbox")
      }
    }

    doTest(testData.pojoWithArrays, testData.pojoWithArrays.getClass, jsonSchemaGenerator, html5Checks = false)
    doTest(testData.pojoWithArraysScala, testData.pojoWithArraysScala.getClass, jsonSchemaGeneratorScala, html5Checks = false)
    doTest(testData.pojoWithArraysScala, testData.pojoWithArraysScala.getClass, jsonSchemaGeneratorScalaHTML5, html5Checks = true)
    doTest(testData.pojoWithArrays, testData.pojoWithArrays.getClass, jsonSchemaGeneratorScalaHTML5, html5Checks = true)
  }

  test("pojoWithArraysNullable") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.pojoWithArraysNullable)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.pojoWithArraysNullable.getClass, Some(jsonNode))

    assertNullableType(schema, "/properties/intArray1", "array")
    assert(schema.at("/properties/intArray1/oneOf/1/items/type").asText() == "integer")

    assertNullableType(schema, "/properties/stringArray", "array")
    assert(schema.at("/properties/stringArray/oneOf/1/items/type").asText() == "string")

    assertNullableType(schema, "/properties/stringList", "array")
    assert(schema.at("/properties/stringList/oneOf/1/items/type").asText() == "string")

    assertNullableType(schema, "/properties/polymorphismList", "array")
    assertNullableChild1(schema, "/properties/polymorphismList/oneOf/1/items/oneOf")
    assertNullableChild2(schema, "/properties/polymorphismList/oneOf/1/items/oneOf")

    assertNullableType(schema, "/properties/polymorphismArray", "array")
    assertNullableChild1(schema, "/properties/polymorphismArray/oneOf/1/items/oneOf")
    assertNullableChild2(schema, "/properties/polymorphismArray/oneOf/1/items/oneOf")

    assertNullableType(schema, "/properties/listOfListOfStrings", "array")
    assert(schema.at("/properties/listOfListOfStrings/oneOf/1/items/type").asText() == "array")
    assert(schema.at("/properties/listOfListOfStrings/oneOf/1/items/items/type").asText() == "string")
  }

  test("recursivePojo") {
    // Non-nullable Java types
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.recursivePojo)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.recursivePojo.getClass, Some(jsonNode))

      assert(schema.at("/properties/myText/type").asText() == "string")

      assert(schema.at("/properties/children/type").asText() == "array")
      val defViaRef = getNodeViaRefs(schema, schema.at("/properties/children/items"), "RecursivePojo")

      assert(defViaRef.at("/properties/myText/type").asText() == "string")
      assert(defViaRef.at("/properties/children/type").asText() == "array")
      val defViaRef2 = getNodeViaRefs(schema, defViaRef.at("/properties/children/items"), "RecursivePojo")

      assert(defViaRef == defViaRef2)
    }

    // Nullable Java types
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.recursivePojo)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.recursivePojo.getClass, Some(jsonNode))

      assertNullableType(schema, "/properties/myText", "string")

      assertNullableType(schema, "/properties/children", "array")
      val defViaRef = getNodeViaRefs(schema, schema.at("/properties/children/oneOf/1/items"), "RecursivePojo")

      assertNullableType(defViaRef, "/properties/myText", "string")
      assertNullableType(defViaRef, "/properties/children", "array")
      val defViaRef2 = getNodeViaRefs(schema, defViaRef.at("/properties/children/oneOf/1/items"), "RecursivePojo")

      assert(defViaRef == defViaRef2)
    }
  }

  test("pojo using Maps") {
    // Use our standard Java validator
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingMaps)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingMaps.getClass, Some(jsonNode))

      assert(schema.at("/properties/string2Integer/type").asText() == "object")
      assert(schema.at("/properties/string2Integer/additionalProperties/type").asText() == "integer")

      assert(schema.at("/properties/string2String/type").asText() == "object")
      assert(schema.at("/properties/string2String/additionalProperties/type").asText() == "string")

      assert(schema.at("/properties/string2PojoUsingJsonTypeInfo/type").asText() == "object")
      assert(schema.at("/properties/string2PojoUsingJsonTypeInfo/additionalProperties/oneOf/0/$ref").asText() == "#/definitions/Child1")
      assert(schema.at("/properties/string2PojoUsingJsonTypeInfo/additionalProperties/oneOf/1/$ref").asText() == "#/definitions/Child2")
    }

    // Try it with nullable types.
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.pojoUsingMaps)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.pojoUsingMaps.getClass, Some(jsonNode))

      assertNullableType(schema, "/properties/string2Integer", "object")
      assert(schema.at("/properties/string2Integer/oneOf/1/additionalProperties/type").asText() == "integer")

      assertNullableType(schema, "/properties/string2String", "object")
      assert(schema.at("/properties/string2String/oneOf/1/additionalProperties/type").asText() == "string")

      assertNullableType(schema, "/properties/string2PojoUsingJsonTypeInfo", "object")
      assert(schema.at("/properties/string2PojoUsingJsonTypeInfo/oneOf/1/additionalProperties/oneOf/0/$ref").asText() == "#/definitions/Child1")
      assert(schema.at("/properties/string2PojoUsingJsonTypeInfo/oneOf/1/additionalProperties/oneOf/1/$ref").asText() == "#/definitions/Child2")
    }
  }

  test("pojo Using Custom Annotations") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingFormat)
    val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.pojoUsingFormat.getClass, Some(jsonNode))
    val schemaHTML5Date = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoUsingFormat.getClass, Some(jsonNode))
    val schemaHTML5DateNullable = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.pojoUsingFormat.getClass, Some(jsonNode))

    assert(schema.at("/format").asText() == "grid")
    assert(schema.at("/description").asText() == "This is our pojo")
    assert(schema.at("/title").asText() == "Pojo using format")


    assert(schema.at("/properties/emailValue/type").asText() == "string")
    assert(schema.at("/properties/emailValue/format").asText() == "email")
    assert(schema.at("/properties/emailValue/description").asText() == "This is our email value")
    assert(schema.at("/properties/emailValue/title").asText() == "Email value")

    assert(schema.at("/properties/choice/type").asText() == "boolean")
    assert(schema.at("/properties/choice/format").asText() == "checkbox")

    assert(schema.at("/properties/dateTime/type").asText() == "string")
    assert(schema.at("/properties/dateTime/format").asText() == "date-time")
    assert(schema.at("/properties/dateTime/description").asText() == "This is description from @JsonPropertyDescription")
    assert(schemaHTML5Date.at("/properties/dateTime/format").asText() == "datetime")
    assert(schemaHTML5DateNullable.at("/properties/dateTime/oneOf/1/format").asText() == "datetime")


    assert(schema.at("/properties/dateTimeWithAnnotation/type").asText() == "string")
    assert(schema.at("/properties/dateTimeWithAnnotation/format").asText() == "text")

    // Make sure autoGenerated title is correct
    assert(schemaHTML5Date.at("/properties/dateTimeWithAnnotation/title").asText() == "Date Time With Annotation")
  }

  test("using JavaType") {
    val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.pojoUsingFormat)
    val schema = generateAndValidateSchemaUsingJavaType( jsonSchemaGenerator, _objectMapper.constructType(testData.pojoUsingFormat.getClass), Some(jsonNode))

    assert(schema.at("/format").asText() == "grid")
    assert(schema.at("/description").asText() == "This is our pojo")
    assert(schema.at("/title").asText() == "Pojo using format")


    assert(schema.at("/properties/emailValue/type").asText() == "string")
    assert(schema.at("/properties/emailValue/format").asText() == "email")
    assert(schema.at("/properties/emailValue/description").asText() == "This is our email value")
    assert(schema.at("/properties/emailValue/title").asText() == "Email value")

    assert(schema.at("/properties/choice/type").asText() == "boolean")
    assert(schema.at("/properties/choice/format").asText() == "checkbox")

    assert(schema.at("/properties/dateTime/type").asText() == "string")
    assert(schema.at("/properties/dateTime/format").asText() == "date-time")
    assert(schema.at("/properties/dateTime/description").asText() == "This is description from @JsonPropertyDescription")


    assert(schema.at("/properties/dateTimeWithAnnotation/type").asText() == "string")
    assert(schema.at("/properties/dateTimeWithAnnotation/format").asText() == "text")

  }

  test("scala using option with HTML5") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorScalaHTML5, testData.pojoUsingOptionScala)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorScalaHTML5, testData.pojoUsingOptionScala.getClass, Some(jsonNode))

    assertNullableType(schema, "/properties/_string", "string")
    assert(schema.at("/properties/_string/title").asText() == "_string")

    assertNullableType(schema, "/properties/_integer", "integer")
    assert(schema.at("/properties/_integer/title").asText() == "_integer")

    assertNullableType(schema, "/properties/_boolean", "boolean")
    assert(schema.at("/properties/_boolean/title").asText() == "_boolean")

    assertNullableType(schema, "/properties/_double", "number")
    assert(schema.at("/properties/_double/title").asText() == "_double")

    assert(schema.at("/properties/child1/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/child1/oneOf/0/title").asText() == "Not included")
    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1Scala")
    assert(schema.at("/properties/child1/title").asText() == "Child 1")

    assertJsonSubTypesInfo(child1, "type", "child1", html5Checks = true)
    assert(child1.at("/properties/parentString/type").asText() == "string")
    assert(child1.at("/properties/child1String/type").asText() == "string")
    assert(child1.at("/properties/_child1String2/type").asText() == "string")
    assert(child1.at("/properties/_child1String3/type").asText() == "string")

    assert(schema.at("/properties/optionalList/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/optionalList/oneOf/0/title").asText() == "Not included")
    assert(schema.at("/properties/optionalList/oneOf/1/type").asText() == "array")
    assert(schema.at("/properties/optionalList/oneOf/1/items/$ref").asText() == "#/definitions/ClassNotExtendingAnythingScala")
    assert(schema.at("/properties/optionalList/title").asText() == "Optional List")
  }

  test("java using optional with HTML5") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5, testData.pojoUsingOptionalJava)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5, testData.pojoUsingOptionalJava.getClass, Some(jsonNode))

    assertNullableType(schema, "/properties/_string", "string")
    assert(schema.at("/properties/_string/title").asText() == "_string")

    assertNullableType(schema, "/properties/_integer", "integer")
    assert(schema.at("/properties/_integer/title").asText() == "_integer")

    assert(schema.at("/properties/child1/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/child1/oneOf/0/title").asText() == "Not included")
    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1")
    assert(schema.at("/properties/child1/title").asText() == "Child 1")

    assertJsonSubTypesInfo(child1, "type", "child1", html5Checks = true)
    assert(child1.at("/properties/parentString/type").asText() == "string")
    assert(child1.at("/properties/child1String/type").asText() == "string")
    assert(child1.at("/properties/_child1String2/type").asText() == "string")
    assert(child1.at("/properties/_child1String3/type").asText() == "string")

    assertNullableType(schema, "/properties/optionalList", "array")
    assert(schema.at("/properties/optionalList/oneOf/1/items/$ref").asText() == "#/definitions/ClassNotExtendingAnything")
    assert(schema.at("/properties/optionalList/title").asText() == "Optional List")
  }

  test("java using optional with HTML5+nullable") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5Nullable, testData.pojoUsingOptionalJava)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.pojoUsingOptionalJava.getClass, Some(jsonNode))

    assertNullableType(schema, "/properties/_string", "string")
    assertNullableType(schema, "/properties/_integer", "integer")

    assert(schema.at("/properties/child1/oneOf/0/type").asText() == "null")
    assert(schema.at("/properties/child1/oneOf/0/title").asText() == "Not included")
    val child1 = getNodeViaRefs(schema, schema.at("/properties/child1/oneOf/1"), "Child1")

    assertJsonSubTypesInfo(child1, "type", "child1", html5Checks = true)
    assertNullableType(child1, "/properties/parentString", "string")
    assertNullableType(child1, "/properties/child1String", "string")
    assertNullableType(child1, "/properties/_child1String2", "string")

    // This is required as we have a @JsonProperty marking it as so.
    assert(child1.at("/properties/_child1String3/type").asText() == "string")
    assertPropertyRequired(child1, "_child1String3", required = true)

    assertNullableType(schema, "/properties/optionalList", "array")
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

    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorHTML5Nullable, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorHTML5Nullable, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert(schema.at("/properties/someString/propertyOrder").asInt() == 1)
      assert(schema.at("/properties/myEnum/propertyOrder").asInt() == 2)
    }

    // Make sure propertyOrder is not enabled when not using html5
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGenerator, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert(schema.at("/properties/someString/propertyOrder").isMissingNode)
    }

    // Same with the non-html5 nullable
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.classNotExtendingAnything.getClass, Some(jsonNode))

      assert(schema.at("/properties/someString/propertyOrder").isMissingNode)
    }
  }

  test("dates") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorScalaHTML5, testData.manyDates)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorScalaHTML5, testData.manyDates.getClass, Some(jsonNode))

    assert(schema.at("/properties/javaLocalDateTime/format").asText() == "datetime-local")
    assert(schema.at("/properties/javaOffsetDateTime/format").asText() == "datetime")
    assert(schema.at("/properties/javaLocalDate/format").asText() == "date")
    assert(schema.at("/properties/jodaLocalDate/format").asText() == "date")

  }

  test("validation") {
    // Scala
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScalaHTML5, testData.classUsingValidation)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScalaHTML5, testData.classUsingValidation.getClass, Some(jsonNode))

      verifyStringProperty(schema, "stringUsingNotNull", Some(1), None, None, required = true)
      verifyStringProperty(schema, "stringUsingNotBlank", Some(1), None, Some("^.*\\S+.*$"), required = true)
      verifyStringProperty(schema, "stringUsingNotBlankAndNotNull", Some(1), None, Some("^.*\\S+.*$"), required = true)
      verifyStringProperty(schema, "stringUsingNotEmpty", Some(1), None, None, required = true)
      verifyStringProperty(schema, "stringUsingSize", Some(1), Some(20), None, required = false)
      verifyStringProperty(schema, "stringUsingSizeOnlyMin", Some(1), None, None, required = false)
      verifyStringProperty(schema, "stringUsingSizeOnlyMax", None, Some(30), None, required = false)
      verifyStringProperty(schema, "stringUsingPattern", None, None, Some("_stringUsingPatternA|_stringUsingPatternB"), required = false)
      verifyStringProperty(schema, "stringUsingPatternList", None, None, Some("^(?=^_stringUsing.*)(?=.*PatternList$).*$"), required = false)

      verifyNumericProperty(schema, "intMin", Some(1), None, required = true)
      verifyNumericProperty(schema, "intMax", None, Some(10), required = true)
      verifyNumericProperty(schema, "doubleMin", Some(1), None, required = true)
      verifyNumericProperty(schema, "doubleMax", None, Some(10), required = true)

      verifyArrayProperty(schema, "notEmptyStringArray", Some(1), None, required = true)

      verifyObjectProperty(schema, "notEmptyMap", "string", Some(1), None, required = true)
    }

    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorScalaHTML5, testData.pojoUsingValidation)
      val schema = generateAndValidateSchema(jsonSchemaGeneratorScalaHTML5, testData.pojoUsingValidation.getClass, Some(jsonNode))

      verifyStringProperty(schema, "stringUsingNotNull", Some(1), None, None, required = true)
      verifyStringProperty(schema, "stringUsingNotBlank", Some(1), None, Some("^.*\\S+.*$"), required = true)
      verifyStringProperty(schema, "stringUsingNotBlankAndNotNull", Some(1), None, Some("^.*\\S+.*$"), required = true)
      verifyStringProperty(schema, "stringUsingNotEmpty", Some(1), None, None, required = true)
      verifyStringProperty(schema, "stringUsingSize", Some(1), Some(20), None, required = false)
      verifyStringProperty(schema, "stringUsingSizeOnlyMin", Some(1), None, None, required = false)
      verifyStringProperty(schema, "stringUsingSizeOnlyMax", None, Some(30), None, required = false)
      verifyStringProperty(schema, "stringUsingPattern", None, None, Some("_stringUsingPatternA|_stringUsingPatternB"), required = false)
      verifyStringProperty(schema, "stringUsingPatternList", None, None, Some("^(?=^_stringUsing.*)(?=.*PatternList$).*$"), required = false)

      verifyNumericProperty(schema, "intMin", Some(1), None, required = true)
      verifyNumericProperty(schema, "intMax", None, Some(10), required = true)
      verifyNumericProperty(schema, "doubleMin", Some(1), None, required = true)
      verifyNumericProperty(schema, "doubleMax", None, Some(10), required = true)

      verifyArrayProperty(schema, "notEmptyStringArray", Some(1), None, required = true)
      verifyArrayProperty(schema, "notEmptyStringList", Some(1), None, required = true)

      verifyObjectProperty(schema, "notEmptyStringMap", "string", Some(1), None, required = true)
    }

    def verifyStringProperty(schema:JsonNode, propertyName:String, minLength:Option[Int], maxLength:Option[Int], pattern:Option[String], required:Boolean): Unit = {
      assertNumericPropertyValidation(schema, propertyName, "minLength", minLength)
      assertNumericPropertyValidation(schema, propertyName, "maxLength", maxLength)

      val matchNode = schema.at(s"/properties/$propertyName/pattern")
      pattern match  {
        case Some(_) => assert(matchNode.asText == pattern.get)
        case None => assert(matchNode.isMissingNode)
      }

      assertPropertyRequired(schema, propertyName, required)
    }

    def verifyNumericProperty(schema:JsonNode, propertyName:String, minimum:Option[Int], maximum:Option[Int], required:Boolean): Unit = {
      assertNumericPropertyValidation(schema, propertyName, "minimum", minimum)
      assertNumericPropertyValidation(schema, propertyName, "maximum", maximum)
      assertPropertyRequired(schema, propertyName, required)
    }

    def verifyArrayProperty(schema:JsonNode, propertyName:String, minItems:Option[Int], maxItems:Option[Int], required:Boolean): Unit = {
      assertNumericPropertyValidation(schema, propertyName, "minItems", minItems)
      assertNumericPropertyValidation(schema, propertyName, "maxItems", maxItems)
      assertPropertyRequired(schema, propertyName, required)
    }

    def verifyObjectProperty(schema:JsonNode, propertyName:String, additionalPropertiesType:String, minProperties:Option[Int], maxProperties:Option[Int], required:Boolean): Unit = {
      assert(schema.at(s"/properties/$propertyName/additionalProperties/type").asText() == additionalPropertiesType)
      assertNumericPropertyValidation(schema, propertyName, "minProperties", minProperties)
      assertNumericPropertyValidation(schema, propertyName, "maxProperties", maxProperties)
      assertPropertyRequired(schema, propertyName, required)
    }

    def assertNumericPropertyValidation(schema:JsonNode, propertyName:String, validationName:String, value:Option[Int]): Unit = {
      val jsonNode = schema.at(s"/properties/$propertyName/$validationName")
      value match {
        case Some(_) => assert(jsonNode.asInt == value.get)
        case None => assert(jsonNode.isMissingNode)
      }
    }
  }

  test("Polymorphism using mixin") {
    // Java
    {
      val jsonNode = assertToFromJson(jsonSchemaGenerator, testData.mixinChild1)
      assertToFromJson(jsonSchemaGenerator, testData.mixinChild1, classOf[MixinParent])

      val schema = generateAndValidateSchema(jsonSchemaGenerator, classOf[MixinParent], Some(jsonNode))

      assertChild1(schema, "/oneOf", defName = "MixinChild1")
      assertChild2(schema, "/oneOf", defName = "MixinChild2")
    }

    // Java + Nullable types
    {
      val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.mixinChild1)
      assertToFromJson(jsonSchemaGeneratorNullable, testData.mixinChild1, classOf[MixinParent])

      val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, classOf[MixinParent], Some(jsonNode))

      assertNullableChild1(schema, "/oneOf", defName = "MixinChild1")
      assertNullableChild2(schema, "/oneOf", defName = "MixinChild2")
    }
  }

  test("issue 24") {
    jsonSchemaGenerator.generateJsonSchema(classOf[EntityWrapper])
    jsonSchemaGeneratorNullable.generateJsonSchema(classOf[EntityWrapper])
  }

  test("Polymorphism oneOf-ordering") {
    val schema = generateAndValidateSchema(jsonSchemaGeneratorScalaHTML5, classOf[PolymorphismOrderingParentScala], None)
    val oneOfList:List[String] = schema.at("/oneOf").asInstanceOf[ArrayNode].iterator().asScala.toList.map(_.at("/$ref").asText)
    assert(List("#/definitions/PolymorphismOrderingChild3", "#/definitions/PolymorphismOrderingChild1", "#/definitions/PolymorphismOrderingChild4", "#/definitions/PolymorphismOrderingChild2") == oneOfList)
  }

  test("@NotNull annotations and nullable types") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorNullable, testData.notNullableButNullBoolean)
    val schema = generateAndValidateSchema(jsonSchemaGeneratorNullable, testData.notNullableButNullBoolean.getClass, None)

    val exception = intercept[Exception] {
      useSchema(schema, Some(jsonNode))
    }

    // While our compiler will let us do what we're about to do, the validator should give us a message that looks like this...
    assert(exception.getMessage.contains("json does not validate against schema"))
    assert(exception.getMessage.contains("error: instance type (null) does not match any allowed primitive type (allowed: [\"boolean\"])"))

    assert(schema.at("/properties/notNullBooleanObject/type").asText() == "boolean")
    assertPropertyRequired(schema, "notNullBooleanObject", required = true)
  }

  test("nestedPolymorphism") {
    val jsonNode = assertToFromJson(jsonSchemaGeneratorScala, testData.nestedPolymorphism)
    assertToFromJson(jsonSchemaGeneratorScala, testData.nestedPolymorphism, classOf[NestedPolymorphism1Base])

    generateAndValidateSchema(jsonSchemaGeneratorScala, classOf[NestedPolymorphism1Base], Some(jsonNode))
  }

  test("PolymorphismAndTitle") {
    val schema = jsonSchemaGeneratorScala.generateJsonSchema(classOf[PolymorphismAndTitleBase])

    println("--------------------------------------------")
    println(asPrettyJson(schema, jsonSchemaGeneratorScala.rootObjectMapper))

    assert( schema.at("/oneOf/0/$ref").asText() == "#/definitions/PolymorphismAndTitle1")
    assert( schema.at("/oneOf/0/title").asText() == "CustomTitle1")
  }

  test("UsingJsonSchemaOptions") {

    {
      val schema = jsonSchemaGeneratorScala.generateJsonSchema(classOf[UsingJsonSchemaOptions])

      println("--------------------------------------------")
      println(asPrettyJson(schema, jsonSchemaGeneratorScala.rootObjectMapper))

      assert(schema.at("/options/classOption").asText() == "classOptionValue")
      assert(schema.at("/properties/propertyUsingOneProperty/options/o1").asText() == "v1")
    }

    {
      val schema = jsonSchemaGeneratorScala.generateJsonSchema(classOf[UsingJsonSchemaOptionsBase])

      println("--------------------------------------------")
      println(asPrettyJson(schema, jsonSchemaGeneratorScala.rootObjectMapper))

      assert(schema.at("/definitions/UsingJsonSchemaOptionsChild1/options/classOption1").asText() == "classOptionValue1")
      assert(schema.at("/definitions/UsingJsonSchemaOptionsChild1/properties/propertyUsingOneProperty/options/o1").asText() == "v1")

      assert(schema.at("/definitions/UsingJsonSchemaOptionsChild2/options/classOption2").asText() == "classOptionValue2")
      assert(schema.at("/definitions/UsingJsonSchemaOptionsChild2/properties/propertyUsingOneProperty/options/o1").asText() == "v1")
    }
  }

  test("UsingJsonSchemaInject") {
    {

      val customUserNameLoaderVariable = "xx"
      val customUserNamesLoader = new CustomUserNamesLoader(customUserNameLoaderVariable)

      val config = JsonSchemaConfig.vanillaJsonSchemaDraft4.copy(jsonSuppliers = Map("myCustomUserNamesLoader" -> customUserNamesLoader))
      val _jsonSchemaGeneratorScala = new JsonSchemaGenerator(_objectMapperScala, debug = true, config)
      val schema = _jsonSchemaGeneratorScala.generateJsonSchema(classOf[UsingJsonSchemaInject])

      println("--------------------------------------------")
      println(asPrettyJson(schema, _jsonSchemaGeneratorScala.rootObjectMapper))

      assert(schema.at("/patternProperties/^s[a-zA-Z0-9]+/type").asText() == "string")
      assert(schema.at("/patternProperties/^i[a-zA-Z0-9]+/type").asText() == "integer")
      assert(schema.at("/properties/sa/type").asText() == "string")
      assert(schema.at("/properties/sa/options/hidden").asText() == "true")
      assert(schema.at("/properties/saMergeFalse/type").asText() == "integer")
      assert(schema.at("/properties/saMergeFalse/default").asText() == "12")
      assert(schema.at("/properties/saMergeFalse/pattern").isMissingNode)
      assert(schema.at("/properties/ib/type").asText() == "integer")
      assert(schema.at("/properties/ib/multipleOf").asInt() == 7)
      assert(schema.at("/properties/ib/exclusiveMinimum").asBoolean())
      assert(schema.at("/properties/uns/items/enum/0").asText() == "foo")
      assert(schema.at("/properties/uns/items/enum/1").asText() == "bar")
      assert(schema.at("/properties/uns2/items/enum/0").asText() == "foo_" + customUserNameLoaderVariable)
      assert(schema.at("/properties/uns2/items/enum/1").asText() == "bar_" + customUserNameLoaderVariable)
    }
  }

  test("Preventing polymorphism by using classTypeReMapping") {

    val config = JsonSchemaConfig.vanillaJsonSchemaDraft4.copy(classTypeReMapping = Map(classOf[Parent] -> classOf[Child1]))
    val _jsonSchemaGenerator = new JsonSchemaGenerator(_objectMapper, debug = true, config)


    // Class with property
    {
      def assertDefaultValues(schema: JsonNode): Unit = {
        assert(schema.at("/properties/stringWithDefault/type").asText() == "string")
        assert(schema.at("/properties/stringWithDefault/default").asText() == "x")
        assert(schema.at("/properties/intWithDefault/type").asText() == "integer")
        assert(schema.at("/properties/intWithDefault/default").asInt() == 12)
        assert(schema.at("/properties/booleanWithDefault/type").asText() == "boolean")
        assert(schema.at("/properties/booleanWithDefault/default").asBoolean())
      }

      // PojoWithParent has a property of type Parent (which uses polymorphism).
      // Default rendering schema will make this property oneOf Child1 and Child2.
      // In this test we're preventing this by remapping Parent to Child1.
      // Now, when generating the schema, we should generate it as if the property where of type Child1

      val jsonNode = assertToFromJson(_jsonSchemaGenerator, testData.pojoWithParent)
      assertToFromJson(_jsonSchemaGenerator, testData.pojoWithParent, classOf[PojoWithParent])

      val schema = generateAndValidateSchema(_jsonSchemaGenerator, classOf[PojoWithParent], Some(jsonNode))

      assert(!schema.at("/additionalProperties").asBoolean())
      assert(schema.at("/properties/pojoValue/type").asText() == "boolean")
      assertDefaultValues(schema)

      assertChild1(schema, "/properties/child")
    }

    // remapping root class
    {
      def doTest(pojo:Object, clazz:Class[_], g:JsonSchemaGenerator): Unit = {
        val jsonNode = assertToFromJson(g, pojo)
        val schema = generateAndValidateSchema(g, clazz, Some(jsonNode))

        assert(!schema.at("/additionalProperties").asBoolean())
        assert(schema.at("/properties/parentString/type").asText() == "string")
        assertJsonSubTypesInfo(schema, "type", "child1")
      }

      doTest(testData.child1, classOf[Parent], _jsonSchemaGenerator)

    }

    //remapping arrays
    {
      def doTest(pojo:Object, clazz:Class[_], g:JsonSchemaGenerator, html5Checks:Boolean): Unit ={

        val jsonNode = assertToFromJson(g, pojo)
        val schema = generateAndValidateSchema(g, clazz, Some(jsonNode))

        assert(schema.at("/properties/intArray1/type").asText() == "array")
        assert(schema.at("/properties/intArray1/items/type").asText() == "integer")

        assert(schema.at("/properties/stringArray/type").asText() == "array")
        assert(schema.at("/properties/stringArray/items/type").asText() == "string")

        assert(schema.at("/properties/stringList/type").asText() == "array")
        assert(schema.at("/properties/stringList/items/type").asText() == "string")
        assert(schema.at("/properties/stringList/minItems").asInt() == 1)
        assert(schema.at("/properties/stringList/maxItems").asInt() == 10)

        assert(schema.at("/properties/polymorphismList/type").asText() == "array")
        assertChild1(schema, "/properties/polymorphismList/items", html5Checks = html5Checks)


        assert(schema.at("/properties/polymorphismArray/type").asText() == "array")
        assertChild1(schema, "/properties/polymorphismArray/items", html5Checks = html5Checks)

        assert(schema.at("/properties/listOfListOfStrings/type").asText() == "array")
        assert(schema.at("/properties/listOfListOfStrings/items/type").asText() == "array")
        assert(schema.at("/properties/listOfListOfStrings/items/items/type").asText() == "string")

        assert(schema.at("/properties/setOfUniqueValues/type").asText() == "array")
        assert(schema.at("/properties/setOfUniqueValues/items/type").asText() == "string")

        if (html5Checks) {
          assert(schema.at("/properties/setOfUniqueValues/uniqueItems").asText() == "true")
          assert(schema.at("/properties/setOfUniqueValues/format").asText() == "checkbox")
        }
      }

      val c = new Child1()
      c.parentString = "pv"
      c.child1String = "cs"
      c.child1String2 = "cs2"
      c.child1String3 = "cs3"

      val _classNotExtendingAnything = {
        val o = new ClassNotExtendingAnything
        o.someString = "Something"
        o.myEnum = MyEnum.C
        o
      }

      val _pojoWithArrays = new PojoWithArrays(
        Array(1,2,3),
        Array("a1","a2","a3"),
        List("l1", "l2", "l3").asJava,
        List[Parent](c, c).asJava,
        List[Parent](c, c).toArray,
        List(_classNotExtendingAnything, _classNotExtendingAnything).asJava,
        PojoWithArrays._listOfListOfStringsValues, // It was difficult to construct this from scala :)
        Set(MyEnum.B).asJava
      )

      doTest(_pojoWithArrays, _pojoWithArrays.getClass, _jsonSchemaGenerator, html5Checks = false)

    }

  }
}

trait TestData {
  import scala.collection.JavaConverters._
  val child1 = {
    val c = new Child1()
    c.parentString = "pv"
    c.child1String = "cs"
    c.child1String2 = "cs2"
    c.child1String3 = "cs3"
    c
  }
  val child2 = {
    val c = new Child2()
    c.parentString = "pv"
    c.child2int = 12
    c
  }
  val pojoWithParent = {
    val p = new PojoWithParent
    p.pojoValue = true
    p.child = child1
    p.stringWithDefault = "y"
    p.intWithDefault = 13
    p.booleanWithDefault = true
    p
  }

  val child21 = {
    val c = new Child21()
    c.parentString = "pv"
    c.child1String = "cs"
    c.child1String2 = "cs2"
    c.child1String3 = "cs3"
    c
  }
  val child22 = {
    val c = new Child22()
    c.parentString = "pv"
    c.child2int = 12
    c
  }

  val child31 = {
    val c = new Child31()
    c.parentString = "pv"
    c.child1String = "cs"
    c.child1String2 = "cs2"
    c.child1String3 = "cs3"
    c
  }
  val child32 = {
    val c = new Child32()
    c.parentString = "pv"
    c.child2int = 12
    c
  }

  val child41 = new Child41()
  val child42 = new Child42()

  val child2Scala = Child2Scala("pv", 12)
  val child1Scala = Child1Scala("pv", "cs", "cs2", "cs3")
  val pojoWithParentScala = PojoWithParentScala(pojoValue = true, child1Scala, "y", 13, booleanWithDefault = true)

  val classNotExtendingAnything = {
    val o = new ClassNotExtendingAnything
    o.someString = "Something"
    o.myEnum = MyEnum.C
    o
  }

  val classNotExtendingAnythingScala = ClassNotExtendingAnythingScala("Something", MyEnum.C, Some(MyEnum.A))

  val manyPrimitives = new ManyPrimitives("s1", 1, 2, true, false, true, 0.1, 0.2, MyEnum.B)

  val manyPrimitivesNulls = new ManyPrimitives(null, null, 1, null, false, false, null, 0.1, null)

  val manyPrimitivesScala = ManyPrimitivesScala("s1", 1, _boolean = true, 0.1)

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
    List("l1", "l2", "l3").asJava,
    List(child1, child2).asJava,
    List(child1, child2).toArray,
    List(classNotExtendingAnything, classNotExtendingAnything).asJava,
    PojoWithArrays._listOfListOfStringsValues, // It was difficult to construct this from scala :)
    Set(MyEnum.B).asJava
  )

  val pojoWithArraysNullable = new PojoWithArraysNullable(
    Array(1,2,3),
    Array("a1","a2","a3"),
    List("l1", "l2", "l3").asJava,
    List(child1, child2).asJava,
    List(child1, child2).toArray,
    List(classNotExtendingAnything, classNotExtendingAnything).asJava,
    PojoWithArrays._listOfListOfStringsValues, // It was difficult to construct this from scala :)
    Set(MyEnum.B).asJava
  )

  val pojoWithArraysScala = PojoWithArraysScala(
    Some(List(1,2,3)),
    List("a1","a2","a3"),
    List("l1", "l2", "l3"),
    List(child1, child2),
    List(child1, child2),
    List(classNotExtendingAnything, classNotExtendingAnything),
    List(List("l11","l12"), List("l21")),
    setOfUniqueValues = Set(MyEnum.B)
  )

  val recursivePojo = new RecursivePojo("t1", List(new RecursivePojo("c1", null)).asJava)

  val pojoUsingMaps = new PojoUsingMaps(
      Map[String, Integer]("a" -> 1, "b" -> 2).asJava,
      Map("x" -> "y", "z" -> "w").asJava,
      Map[String, Parent]("1" -> child1, "2" -> child2).asJava
    )

  val pojoUsingFormat = new PojoUsingFormat("test@example.com", true, OffsetDateTime.now(), OffsetDateTime.now())
  val manyDates = ManyDates(LocalDateTime.now(), OffsetDateTime.now(), LocalDate.now(), org.joda.time.LocalDate.now())

  val classUsingValidation = ClassUsingValidation(
    "_stringUsingNotNull", "_stringUsingNotBlank", "_stringUsingNotBlankAndNotNull", "_stringUsingNotEmpty", List("l1", "l2", "l3"), Map("mk1" -> "mv1", "mk2" -> "mv2"),
    "_stringUsingSize", "_stringUsingSizeOnlyMin", "_stringUsingSizeOnlyMax", "_stringUsingPatternA", "_stringUsingPatternList",
    1, 2, 1.0, 2.0
  )

  val pojoUsingValidation = new PojoUsingValidation(
    "_stringUsingNotNull", "_stringUsingNotBlank", "_stringUsingNotBlankAndNotNull", "_stringUsingNotEmpty", Array("a1", "a2", "a3"), List("l1", "l2", "l3").asJava,
    Map("mk1" -> "mv1", "mk2" -> "mv2").asJava, "_stringUsingSize", "_stringUsingSizeOnlyMin", "_stringUsingSizeOnlyMax", "_stringUsingPatternA",
    "_stringUsingPatternList", 1, 2, 1.0, 2.0
  )

  val mixinChild1 = {
    val c = new MixinChild1()
    c.parentString = "pv"
    c.child1String = "cs"
    c.child1String2 = "cs2"
    c.child1String3 = "cs3"
    c
  }

  // Test the collision of @NotNull validations and null fields.
  val notNullableButNullBoolean = new PojoWithNotNull(null)

  val nestedPolymorphism = NestedPolymorphism1_1("a1", NestedPolymorphism2_2("a2", Some(NestedPolymorphism3("b3"))))

  val genericClassVoid = new GenericClassVoid()

  val genericMapLike = new GenericMapLike(Collections.singletonMap("foo", "bar"))

}
