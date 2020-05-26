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
  @JsonSchemaDefault("2.5")
  @JsonSchemaExamples(Array("1.5", "3.0"))
  weight:Double,

  @JsonProperty( defaultValue = "ds")
  defaultStringViaJsonValue:String,
  @JsonProperty( defaultValue = "1")
  defaultIntViaJsonValue:Int,
  @JsonProperty( defaultValue = "true")
  defaultBoolViaJsonValue:Boolean,
  @JsonProperty( defaultValue = "1.3")
  defaultNumberViaJsonValue:Double
)
