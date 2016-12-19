package com.kjetland.jackson.jsonSchema.testData.polymorphism2.polymorphism1;

public class Child22 extends Parent2 {

    public Integer child2int;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Child22 child2 = (Child22) o;

        return child2int != null ? child2int.equals(child2.child2int) : child2.child2int == null;

    }

    @Override
    public int hashCode() {
        return child2int != null ? child2int.hashCode() : 0;
    }
}
