package com.kjetland.jackson.jsonSchema.testData.polymorphism7;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "polymorphicType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Child71.class),
        @JsonSubTypes.Type(value = Child72.class) })
public class Parent7 {

}
