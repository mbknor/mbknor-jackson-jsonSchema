package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = NestedPolymorphism1_1.class, name = "NestedPolymorphism1_1"),
  @JsonSubTypes.Type(value = NestedPolymorphism1_2.class, name = "NestedPolymorphism1_2")
})
public abstract class NestedPolymorphism1Base {}
