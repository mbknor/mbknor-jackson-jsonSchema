package com.kjetland.jackson.jsonSchema

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.kjetland.jackson.jsonSchema.testData._
import org.scalatest.{FunSuite, Matchers}

class JsonSchemaGeneratorTest extends FunSuite with Matchers with TestData {

  val objectMapper = new ObjectMapper()
  val simpleModule = new SimpleModule()
  simpleModule.addSerializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerSerializer)
  simpleModule.addDeserializer(classOf[PojoWithCustomSerializer], new PojoWithCustomSerializerDeserializer)
  objectMapper.registerModule(simpleModule)

  val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper)

  def asPrettyJson(node:JsonNode):String = {
    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
  }


  def assertToFromJson(o:Any): JsonNode = {
    assertToFromJson(o, o.getClass)
  }

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

  def generateAndValidateSchema(clazz:Class[_], jsonToTestAgainstSchema:Option[JsonNode] = None):String = {
    val schema = jsonSchemaGenerator.generateJsonSchema(clazz)

    println("--------------------------------------------")
    println(asPrettyJson(schema))

    useSchema(schema, jsonToTestAgainstSchema)

    asPrettyJson(schema)
  }

  test("regular object") {
    val jsonNode = assertToFromJson(child1)

    val schemaAsJson = generateAndValidateSchema(child1.getClass, Some(jsonNode))

  }

  test("polymorphism") {
    val jsonNode = assertToFromJson(pojoWithParent)

    val schemaAsJson = generateAndValidateSchema(pojoWithParent.getClass, Some(jsonNode))

  }

  test("polymorphism - first Level") {
    val jsonNode = assertToFromJson(child1)
    assertToFromJson(child1, classOf[Parent])

    val schemaAsJson = generateAndValidateSchema(classOf[Parent], Some(jsonNode))
  }

  test("primitives") {
    val jsonNode = assertToFromJson(manyPrimitives)
    val schemaAsJson = generateAndValidateSchema(manyPrimitives.getClass, Some(jsonNode))
  }

  test("custom serializer not overriding JsonSerializer.acceptJsonFormatVisitor") {

    val jsonNode = assertToFromJson(pojoWithCustomSerializer)
    val schemaAsJson = generateAndValidateSchema(pojoWithCustomSerializer.getClass, Some(jsonNode))
  }

  test("pojoWithArrays") {

    val jsonNode = assertToFromJson(pojoWithArrays)
    val schemaAsJson = generateAndValidateSchema(pojoWithArrays.getClass, Some(jsonNode))
  }

  test("recursivePojo") {
    val jsonNode = assertToFromJson(recursivePojo)
    val schemaAsJson = generateAndValidateSchema(recursivePojo.getClass, Some(jsonNode))

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

  val manyPrimitives = new ManyPrimitives("s1", 1, 2, true, false, true, 0.1, 0.2, MyEnum.B)

  val pojoWithCustomSerializer = {
    val p = new PojoWithCustomSerializer
    p.myString = "xxx"
    p
  }

  val pojoWithArrays = new PojoWithArrays(
    Array(1,2,3),
    Array("a1","a2","a3"),
    List("l1", "l2", "l3"),
    List(child1, child2),
    List(child1, child2).toArray

  )

  val recursivePojo = new RecursivePojo("t1", List(new RecursivePojo("c1", null)))
}
