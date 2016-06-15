package com.kjetland.jackson.jsonSchema.testData;


public class PojoWithCustomSerializer {

    public String myString;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PojoWithCustomSerializer child1 = (PojoWithCustomSerializer) o;

        return myString != null ? myString.equals(child1.myString) : child1.myString == null;

    }

    @Override
    public int hashCode() {
        return myString != null ? myString.hashCode() : 0;
    }
}


