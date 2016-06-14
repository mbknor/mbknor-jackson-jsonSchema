package com.kjetland.jackson.jsonSchema

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.kjetland.jackson.jsonSchema.testData.{Child1, Parent, PojoWithParent}
import org.scalatest.{FunSuite, Matchers}

class JsonSchemaGeneratorTest extends FunSuite with Matchers with TestData {

  val objectMapper = new ObjectMapper()

  val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper)

  def asPrettyJson(node:JsonNode):String = {
    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
  }


  def assertToFromJson(o:Any): Unit = {
    assertToFromJson(o, o.getClass)
  }

  def assertToFromJson(o:Any, deserType:Class[_]): Unit = {
    val json = objectMapper.writeValueAsString(o)
    println(s"json: $json")
    val r = objectMapper.readValue(json, deserType)
    assert( o == r)
  }

  test("polymorphism") {
    assertToFromJson(pojoWithParent)

    val schema = jsonSchemaGenerator.generateJsonSchema(pojoWithParent.getClass)

    val schemaAsJson = asPrettyJson(schema)
    println("--------------------------------------------")
    println(schemaAsJson)

  }

  test("polymorphism - first Level") {
    assertToFromJson(child1)
    assertToFromJson(child1, classOf[Parent])

    val schema = jsonSchemaGenerator.generateJsonSchema(classOf[Parent])

    val schemaAsJson = asPrettyJson(schema)
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
