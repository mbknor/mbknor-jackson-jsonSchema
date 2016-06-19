package com.kjetland.jackson.jsonSchema

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.kjetland.jackson.jsonSchema.testData._
import org.scalatest.{FunSuite, Matchers}

import scala.collection.JavaConversions._

class JsonSchemaGeneratorTest extends FunSuite with Matchers {

  val objectMapper = new ObjectMapper()
  val simpleModule = new SimpleModule()
  simpleModule.addSerializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerSerializer)
  simpleModule.addDeserializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerDeserializer)
  objectMapper.registerModule(simpleModule)

  val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper)

  val testData = new TestData{}

  def asPrettyJson(node:JsonNode):String = {
    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
  }


  // Asserts that we're able to go from object => json => equal object
  def assertToFromJson(o:Any): JsonNode = {
    assertToFromJson(o, o.getClass)
  }

  // Asserts that we're able to go from object => json => equal object
  // deserType might be a class which o extends (polymorphism)
  def assertToFromJson(o:Any, deserType:Class[_]): JsonNode = {
    val json = objectMapper.writeValueAsString(o)
    println(s"json: $json")
    val jsonNode = objectMapper.readTree(json)
    val r = objectMapper.treeToValue(jsonNode, deserType)
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
  def generateAndValidateSchema(clazz:Class[_], jsonToTestAgainstSchema:Option[JsonNode] = None):JsonNode = {
    val schema = jsonSchemaGenerator.generateJsonSchema(clazz)

    println("--------------------------------------------")
    println(asPrettyJson(schema))

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
    node.asInstanceOf[ArrayNode].iterator().toList.map(_.asText())
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
    val jsonNode = assertToFromJson(testData.classNotExtendingAnything)

    val schema = generateAndValidateSchema((testData.classNotExtendingAnything).getClass, Some(jsonNode))

    assert( false == schema.at("/additionalProperties").asBoolean())
    assert( schema.at("/properties/someString/type").asText() == "string")

  }

  test("Generating schema for concrete class which happens to extend class using @JsonTypeInfo") {
    val jsonNode = assertToFromJson(testData.child1)

    val schema = generateAndValidateSchema(testData.child1.getClass, Some(jsonNode))

    assert( false == schema.at("/additionalProperties").asBoolean())
    assert( schema.at("/properties/parentString/type").asText() == "string")
    assertJsonSubTypesInfo(schema, "type", "child1")

  }

  test("Generate schema for regular class which has a property of class annotated with @JsonTypeInfo") {
    val jsonNode = assertToFromJson(testData.pojoWithParent)
    val schema = generateAndValidateSchema(testData.pojoWithParent.getClass, Some(jsonNode))

    assert( false == schema.at("/additionalProperties").asBoolean())
    assert( schema.at("/properties/pojoValue/type").asText() == "boolean")

    assertChild1(schema, "/properties/child/oneOf")
    assertChild2(schema, "/properties/child/oneOf")


  }

  def assertChild1(node:JsonNode, path:String): Unit ={
    val child1 = getNodeViaArrayOfRefs(node, path, "Child1")
    assertJsonSubTypesInfo(child1, "type", "child1")
    assert( child1.at("/properties/parentString/type").asText() == "string" )
    assert( child1.at("/properties/child1String/type").asText() == "string" )
  }

  def assertChild2(node:JsonNode, path:String): Unit ={
    val child2 = getNodeViaArrayOfRefs(node, path, "Child2")
    assertJsonSubTypesInfo(child2, "type", "child2")
    assert( child2.at("/properties/parentString/type").asText() == "string" )
    assert( child2.at("/properties/child2int/type").asText() == "integer" )
  }

  test("Generate schema for super class annotated with @JsonTypeInfo") {
    val jsonNode = assertToFromJson(testData.child1)
    assertToFromJson(testData.child1, classOf[Parent])

    val schema = generateAndValidateSchema(classOf[Parent], Some(jsonNode))

    assertChild1(schema, "/oneOf")
    assertChild2(schema, "/oneOf")

  }

  test("primitives") {
    val jsonNode = assertToFromJson(testData.manyPrimitives)
    val schema = generateAndValidateSchema(testData.manyPrimitives.getClass, Some(jsonNode))

    assert( schema.at("/properties/_string/type").asText() == "string" )

    assert( schema.at("/properties/_integer/type").asText() == "integer" )
    assert( !getRequiredList(schema).contains("_integer")) // Should allow null by default

    assert( schema.at("/properties/_int/type").asText() == "integer" )
    assert( getRequiredList(schema).contains("_int")) // Must have a value

    assert( schema.at("/properties/_booleanObject/type").asText() == "boolean" )
    assert( !getRequiredList(schema).contains("_booleanObject")) // Should allow null by default

    assert( schema.at("/properties/_booleanPrimitive/type").asText() == "boolean" )
    assert( getRequiredList(schema).contains("_booleanPrimitive")) // Must be required since it must have true or false - not null

    assert( schema.at("/properties/_booleanObjectWithNotNull/type").asText() == "boolean" )
    assert( getRequiredList(schema).contains("_booleanObjectWithNotNull"))

    assert( schema.at("/properties/_doubleObject/type").asText() == "number" )
    assert( !getRequiredList(schema).contains("_doubleObject")) // Should allow null by default

    assert( schema.at("/properties/_doublePrimitive/type").asText() == "number" )
    assert( getRequiredList(schema).contains("_doublePrimitive")) // Must be required since it must have a value - not null

    assert( schema.at("/properties/myEnum/type").asText() == "string")
    assert( getArrayNodeAsListOfStrings(schema.at("/properties/myEnum/enum")) == List("A", "B", "C") )


  }

  test("custom serializer not overriding JsonSerializer.acceptJsonFormatVisitor") {

    val jsonNode = assertToFromJson(testData.pojoWithCustomSerializer)
    val schema = generateAndValidateSchema(testData.pojoWithCustomSerializer.getClass, Some(jsonNode))
    assert( schema.asInstanceOf[ObjectNode].fieldNames().toList == List("$schema")) // Empyt schema due to custom serializer
  }

  test("object with property using custom serializer not overriding JsonSerializer.acceptJsonFormatVisitor") {

    val jsonNode = assertToFromJson(testData.objectWithPropertyWithCustomSerializer)
    val schema = generateAndValidateSchema(testData.objectWithPropertyWithCustomSerializer.getClass, Some(jsonNode))
    assert( schema.at("/properties/s/type").asText() == "string")
    assert( schema.at("/properties/child").asInstanceOf[ObjectNode].fieldNames().toList.size == 0)
  }


  test("pojoWithArrays") {

    val jsonNode = assertToFromJson(testData.pojoWithArrays)
    val schema = generateAndValidateSchema(testData.pojoWithArrays.getClass, Some(jsonNode))

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

  test("recursivePojo") {
    val jsonNode = assertToFromJson(testData.recursivePojo)
    val schema = generateAndValidateSchema(testData.recursivePojo.getClass, Some(jsonNode))

    assert( schema.at("/properties/myText/type").asText() == "string")

    assert( schema.at("/properties/children/type").asText() == "array")
    val defViaRef = getNodeViaRefs(schema, schema.at("/properties/children/items"), "RecursivePojo")

    assert( defViaRef.at("/properties/myText/type").asText() == "string")
    assert( defViaRef.at("/properties/children/type").asText() == "array")
    val defViaRef2 = getNodeViaRefs(schema, defViaRef.at("/properties/children/items"), "RecursivePojo")

    assert( defViaRef == defViaRef2)

  }


}

trait TestData {
  import scala.collection.JavaConversions._
  val child1 = {
    val c = new Child1()
    c.parentString = "pv"
    c.child1String = "cs"
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
    p
  }

  val classNotExtendingAnything = {
    val o = new ClassNotExtendingAnything
    o.someString = "Something"
    o
  }

  val manyPrimitives = new ManyPrimitives("s1", 1, 2, true, false, true, 0.1, 0.2, MyEnum.B)

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
    List(child1, child2).toArray

  )

  val recursivePojo = new RecursivePojo("t1", List(new RecursivePojo("c1", null)))
}
