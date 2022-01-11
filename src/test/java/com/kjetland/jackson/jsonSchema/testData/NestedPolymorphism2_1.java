package com.kjetland.jackson.jsonSchema.testData;

import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NestedPolymorphism2_1 extends NestedPolymorphism2Base { 
    String a; 
    Optional<NestedPolymorphism3> pojo; 
}
