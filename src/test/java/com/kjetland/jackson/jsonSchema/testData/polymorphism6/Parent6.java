package com.kjetland.jackson.jsonSchema.testData.polymorphism6;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "clazz")
public interface Parent6 {

}
