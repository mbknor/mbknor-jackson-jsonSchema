package com.kjetland.jackson.jsonSchema.testDataScala

import com.kjetland.jackson.jsonSchema.testData.MyEnum

case class ClassNotExtendingAnythingScala(someString:String, myEnum: MyEnum, myEnumO: Option[MyEnum])
