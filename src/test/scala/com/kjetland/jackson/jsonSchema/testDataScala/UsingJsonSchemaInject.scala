package com.kjetland.jackson.jsonSchema.testDataScala

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject


@JsonSchemaInject(
    json=
      """
        {
          "patternProperties": {
            "^[a-zA-Z0-9]+": {
              "type": "string"
            }
          }
        }
      """)
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
  a:String

)










