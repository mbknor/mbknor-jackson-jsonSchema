package com.kjetland.jackson.jsonSchema.testDataScala

import com.fasterxml.jackson.annotation.{JsonPropertyDescription, JsonSubTypes, JsonTypeInfo}
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[PolymorphismAndTitle1], name = "type_1"),
  new JsonSubTypes.Type(value = classOf[PolymorphismAndTitle2], name = "type_2")))
trait PolymorphismAndTitleBase

@JsonSchemaTitle("CustomTitle1")
@JsonPropertyDescription("Custom title 1 description")
case class PolymorphismAndTitle1(a:String) extends PolymorphismAndTitleBase

@JsonSchemaTitle("CustomTitle2")
@JsonPropertyDescription("Custom title 2 description")
case class PolymorphismAndTitle2(a:String) extends PolymorphismAndTitleBase
