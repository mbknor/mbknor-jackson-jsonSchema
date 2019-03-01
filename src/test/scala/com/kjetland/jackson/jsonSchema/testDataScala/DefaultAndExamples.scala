package com.kjetland.jackson.jsonSchema.testDataScala

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;

case class DefaultAndExamples
(
  @JsonSchemaExamples(Array("user@example.com"))
  emailValue:String,
  @JsonSchemaDefault("12")
  @JsonSchemaExamples(Array("10", "14", "18"))
  fontSize:Int
)
