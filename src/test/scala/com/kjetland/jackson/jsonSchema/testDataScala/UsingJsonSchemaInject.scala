package com.kjetland.jackson.jsonSchema.testDataScala

import javax.validation.constraints.Min

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
  ib:Int
)










