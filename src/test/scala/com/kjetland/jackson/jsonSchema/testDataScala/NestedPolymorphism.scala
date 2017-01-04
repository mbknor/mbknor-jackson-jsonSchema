package com.kjetland.jackson.jsonSchema.testDataScala

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

case class NestedPolymorphism3(b:String)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[NestedPolymorphism2_1], name = "NestedPolymorphism2_1"),
  new JsonSubTypes.Type(value = classOf[NestedPolymorphism2_2], name = "NestedPolymorphism2_2")
))
trait NestedPolymorphism2Base

case class NestedPolymorphism2_1(a:String, pojo:Option[NestedPolymorphism3]) extends NestedPolymorphism2Base
case class NestedPolymorphism2_2(a:String, pojo:Option[NestedPolymorphism3]) extends NestedPolymorphism2Base

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[NestedPolymorphism1_1], name = "NestedPolymorphism1_1"),
  new JsonSubTypes.Type(value = classOf[NestedPolymorphism1_2], name = "NestedPolymorphism1_2")
))
trait NestedPolymorphism1Base

case class NestedPolymorphism1_1(a:String, pojo:NestedPolymorphism2Base) extends NestedPolymorphism1Base
case class NestedPolymorphism1_2(a:String, pojo:NestedPolymorphism2Base) extends NestedPolymorphism1Base
