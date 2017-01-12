package com.kjetland.jackson.jsonSchema.testDataScala

import javax.validation.constraints.{NotNull, Size}

import com.kjetland.jackson.jsonSchema.annotations.{JsonSchemaIntValue, JsonSchemaValues, JsonSchemaStringValue}
import com.kjetland.jackson.jsonSchema.testData.{ClassNotExtendingAnything, MyEnum, Parent}

case class PojoWithArraysScala
(
  @NotNull
  intArray1:Option[List[Integer]], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  @NotNull
  stringArray:List[String], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  @NotNull
  @Size(min = 1, max = 10)
  stringList:List[String],
  @NotNull
  polymorphismList:List[Parent],
  @NotNull
  polymorphismArray:List[Parent], // We never use array in scala - use list instead to make it compatible with PojoWithArrays (java)
  @NotNull
  regularObjectList:List[ClassNotExtendingAnything],
  @NotNull
  listOfListOfStrings:List[List[String]],
  @NotNull
  setOfUniqueValues:Set[MyEnum],
  @NotNull
  @JsonSchemaValues(stringValues = Array(new JsonSchemaStringValue(path = "items.pattern", value = "_stringUsingPatternA|_stringUsingPatternB")))
  setOfStringsUsingPattern:Set[String],
  @JsonSchemaValues(intValues = Array(new JsonSchemaIntValue(path = "items.minimum", value = 3)))
  setOfIntsUsingMin:Set[Integer]
)
