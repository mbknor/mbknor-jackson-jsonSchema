package com.kjetland.jackson.jsonSchema.testDataScala

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

case class PojoUsingOptionScala(
                                 _string:Option[String],
                                 @JsonDeserialize(contentAs = classOf[Int])     _integer:Option[Int],
                                 @JsonDeserialize(contentAs = classOf[Boolean]) _boolean:Option[Boolean],
                                 @JsonDeserialize(contentAs = classOf[Double])  _double:Option[Double],
                                 child1:Option[Child1Scala],
                                 optionalList:Option[List[ClassNotExtendingAnythingScala]]
                                 //, parent:Option[ParentScala] - Not using this one: jackson-scala-module does not support Option combined with Polymorphism
                               )
