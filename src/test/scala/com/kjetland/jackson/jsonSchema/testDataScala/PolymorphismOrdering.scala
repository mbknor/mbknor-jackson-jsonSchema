package com.kjetland.jackson.jsonSchema.testDataScala

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(
  new JsonSubTypes.Type(value = classOf[PolymorphismOrderingChild3], name = "PolymorphismOrderingChild3"),
  new JsonSubTypes.Type(value = classOf[PolymorphismOrderingChild1], name = "PolymorphismOrderingChild1"),
  new JsonSubTypes.Type(value = classOf[PolymorphismOrderingChild4], name = "PolymorphismOrderingChild4"),
  new JsonSubTypes.Type(value = classOf[PolymorphismOrderingChild2], name = "PolymorphismOrderingChild2")))
trait PolymorphismOrderingParentScala

case class PolymorphismOrderingChild1() extends PolymorphismOrderingParentScala
case class PolymorphismOrderingChild2() extends PolymorphismOrderingParentScala
case class PolymorphismOrderingChild3() extends PolymorphismOrderingParentScala
case class PolymorphismOrderingChild4() extends PolymorphismOrderingParentScala



