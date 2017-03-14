package com.kjetland.jackson.jsonSchema.testDataScala

import java.util.function.Supplier
import javax.validation.constraints.Min

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.kjetland.jackson.jsonSchema.annotations.{JsonSchemaBool, JsonSchemaInject, JsonSchemaInt, JsonSchemaString}


@JsonSchemaInject(
    json=
      """
        {
          "patternProperties": {
            "^s[a-zA-Z0-9]+": {
              "type": "string"
            }
          }
        }
      """,
    strings = Array(new JsonSchemaString(path = "patternProperties/^i[a-zA-Z0-9]+/type", value = "integer"))
)
case class UsingJsonSchemaInject
(
  @JsonSchemaInject(
      json=
        """
          {
             "options": {
                "hidden": true
             }
          }
        """)
  sa:String,

  @JsonSchemaInject(
    bools = Array(new JsonSchemaBool(path = "exclusiveMinimum", value = true)),
    ints = Array(new JsonSchemaInt(path = "multipleOf", value = 7))
  )
  @Min(5)
  ib:Int,

  @JsonSchemaInject(jsonSupplier = classOf[UserNamesLoader])
  uns:Set[String],

  @JsonSchemaInject(jsonSupplierViaLookup = "myCustomUserNamesLoader")
  uns2:Set[String]
)

class UserNamesLoader extends Supplier[JsonNode] {
  val _objectMapper = new ObjectMapper()

  override def get(): JsonNode = {
    val schema = _objectMapper.createObjectNode()
    val values = schema.putObject("items").putArray("enum")
    values.add("foo")
    values.add("bar")

    schema
  }
}

class CustomUserNamesLoader(custom:String) extends Supplier[JsonNode] {
  val _objectMapper = new ObjectMapper()

  override def get(): JsonNode = {
    val schema = _objectMapper.createObjectNode()
    val values = schema.putObject("items").putArray("enum")
    values.add("foo_"+custom)
    values.add("bar_"+custom)

    schema
  }
}
  










