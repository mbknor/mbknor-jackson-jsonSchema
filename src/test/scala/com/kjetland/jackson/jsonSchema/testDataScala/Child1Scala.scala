package com.kjetland.jackson.jsonSchema.testDataScala

import com.fasterxml.jackson.annotation.JsonProperty

case class Child1Scala
(
  parentString:String,
  child1String:String,

  @JsonProperty("_child1String2")
  child1String2:String,

  @JsonProperty(value = "_child1String3", required = true)
  child1String3:String
) extends ParentScala
