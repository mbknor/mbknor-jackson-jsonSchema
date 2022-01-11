package com.kjetland.jackson.jsonSchema.testData.polymorphism4;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(TypeIdResolverBySimpleNameInPackage.class)
public class Parent4 {
}
