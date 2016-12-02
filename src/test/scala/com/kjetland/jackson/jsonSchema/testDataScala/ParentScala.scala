package com.kjetland.jackson.jsonSchema.testDataScala

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(Array(new JsonSubTypes.Type(value = classOf[Child1Scala], name = "child1"), new JsonSubTypes.Type(value = classOf[Child2Scala], name = "child2")))
trait ParentScala
