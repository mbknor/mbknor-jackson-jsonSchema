package com.kjetland.jackson.jsonSchema

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.kjetland.jackson.jsonSchema.testData.{Child1, Parent, PojoWithParent}
import org.scalatest.{FunSuite, Matchers}

class JsonSchemaGeneratorTest extends FunSuite with Matchers with TestData {

  val objectMapper = new ObjectMapper()

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
    useSchema(schema, jsonToTestAgainstSchema)

    asPrettyJson(schema)
  }

  test("polymorphism") {
    val jsonNode = assertToFromJson(pojoWithParent)

    val schemaAsJson = generateAndValidateSchema(pojoWithParent.getClass, Some(jsonNode))
    println("--------------------------------------------")
    println(schemaAsJson)

  }

  test("polymorphism - first Level") {
    val jsonNode = assertToFromJson(child1)
    assertToFromJson(child1, classOf[Parent])

    val schemaAsJson = generateAndValidateSchema(classOf[Parent], Some(jsonNode))
    println("--------------------------------------------")
    println(schemaAsJson)
  }


}

trait TestData {
  val child1 = {
    val c = new Child1()
    c.parentString = "pv"
    c.child1String = "cs"
    c
  }

  val pojoWithParent = {
    val p = new PojoWithParent
    p.pojoValue = true
    p.child = child1
    p
  }
}
