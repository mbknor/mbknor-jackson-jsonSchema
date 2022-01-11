package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kjetland.jackson.jsonSchema.testData.PolymorphismOrdering.PolymorphismOrderingChild1;
import com.kjetland.jackson.jsonSchema.testData.PolymorphismOrdering.PolymorphismOrderingChild2;
import com.kjetland.jackson.jsonSchema.testData.PolymorphismOrdering.PolymorphismOrderingChild3;
import com.kjetland.jackson.jsonSchema.testData.PolymorphismOrdering.PolymorphismOrderingChild4;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = PolymorphismOrderingChild3.class, name = "PolymorphismOrderingChild3"),
  @JsonSubTypes.Type(value = PolymorphismOrderingChild1.class, name = "PolymorphismOrderingChild1"),
  @JsonSubTypes.Type(value = PolymorphismOrderingChild4.class, name = "PolymorphismOrderingChild4"),
  @JsonSubTypes.Type(value = PolymorphismOrderingChild2.class, name = "PolymorphismOrderingChild2")})
public interface PolymorphismOrdering {
    public static class PolymorphismOrderingChild1 implements PolymorphismOrdering {}
    public static class PolymorphismOrderingChild2 implements PolymorphismOrdering {}
    public static class PolymorphismOrderingChild3 implements PolymorphismOrdering {}
    public static class PolymorphismOrderingChild4 implements PolymorphismOrdering {}
}