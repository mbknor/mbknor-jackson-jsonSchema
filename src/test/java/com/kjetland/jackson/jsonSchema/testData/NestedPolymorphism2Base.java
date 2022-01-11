package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = NestedPolymorphism2_1.class, name = "NestedPolymorphism2_1"),
  @JsonSubTypes.Type(value = NestedPolymorphism2_2.class, name = "NestedPolymorphism2_2")
})
public abstract class NestedPolymorphism2Base {}
