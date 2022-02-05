package com.kjetland.jackson.jsonSchema.testData.polymorphism4;

public class Child41 extends Parent4 {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        return o != null && getClass() == o.getClass();
    }
}
