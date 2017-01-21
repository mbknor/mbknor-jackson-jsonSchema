package com.kjetland.jackson.jsonSchema.testDataScala

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaOptions
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}


@JsonSchemaOptions( items = Array(
  new JsonSchemaOptions.Item(name = "classOption", value="classOptionValue")))
case class UsingJsonSchemaOptions
(
  @JsonSchemaOptions( items = Array(
    new JsonSchemaOptions.Item(name = "o1", value="v1")))
  propertyUsingOneProperty:String

)


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
new JsonSubTypes.Type(value = classOf[UsingJsonSchemaOptionsChild1], name = "c1"),
new JsonSubTypes.Type(value = classOf[UsingJsonSchemaOptionsChild2], name = "c2")))
trait UsingJsonSchemaOptionsBase


@JsonSchemaOptions( items = Array(
  new JsonSchemaOptions.Item(name = "classOption1", value="classOptionValue1")))
case class UsingJsonSchemaOptionsChild1
(
  @JsonSchemaOptions( items = Array(
    new JsonSchemaOptions.Item(name = "o1", value="v1")))
  propertyUsingOneProperty:String

)

@JsonSchemaOptions( items = Array(
  new JsonSchemaOptions.Item(name = "classOption2", value="classOptionValue2")))
case class UsingJsonSchemaOptionsChild2
(
  @JsonSchemaOptions( items = Array(
    new JsonSchemaOptions.Item(name = "o1", value="v1")))
  propertyUsingOneProperty:String

)


