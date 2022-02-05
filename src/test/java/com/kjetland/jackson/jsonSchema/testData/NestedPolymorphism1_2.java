package com.kjetland.jackson.jsonSchema.testData;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NestedPolymorphism1_2 extends NestedPolymorphism1Base { 
    String a; 
    NestedPolymorphism2Base pojo; 
}
