package com.kjetland.jackson.jsonSchema.testDataScala

import com.fasterxml.jackson.annotation.JsonProperty
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;

case class DefaultAndExamples
(
  @JsonSchemaExamples(Array("user@example.com"))
  emailValue:String,
  @JsonSchemaDefault("12")
  @JsonSchemaExamples(Array("10", "14", "18"))
  fontSize:Int,

  @JsonProperty( defaultValue = "ds")
  defaultStringViaJsonValue:String,
  @JsonProperty( defaultValue = "1")
  defaultIntViaJsonValue:Int,
  @JsonProperty( defaultValue = "true")
  defaultBoolViaJsonValue:Boolean
)
