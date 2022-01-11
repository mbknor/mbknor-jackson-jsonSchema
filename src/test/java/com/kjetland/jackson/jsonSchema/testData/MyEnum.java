package com.kjetland.jackson.jsonSchema.testData;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;

@JsonSchemaInject(json = "{\"JsonSchemaInjectOnEnum\":true}")
public enum MyEnum {
    E,B,A,D,C
}
