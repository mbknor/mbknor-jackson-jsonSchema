package com.kjetland.jackson.jsonSchema.testData.polymorphism6;

public class Child61 extends Parent6 {

    public String child1String;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Child61)) return false;
        if (!super.equals(o)) return false;

        Child61 child1 = (Child61) o;

        return child1String != null ? child1String.equals(child1.child1String) : child1.child1String == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (child1String != null ? child1String.hashCode() : 0);
        return result;
    }
}
