package com.kjetland.jackson.jsonSchema.testDataScala

import com.kjetland.jackson.jsonSchema.testData.{ClassNotExtendingAnything, Parent}

case class PojoWithArraysScala
(
  intArray1:Option[List[Integer]], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  stringArray:List[String], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  stringList:List[String],
  polymorphismList:List[Parent],
  polymorphismArray:List[Parent], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  regularObjectList:List[ClassNotExtendingAnything],
  listOfListOfStrings:List[List[String]]
)
