package com.kjetland.jackson.jsonSchema.testData;

public class PojoWithParent {

    public Boolean pojoValue;
    public Parent child;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PojoWithParent that = (PojoWithParent) o;

        if (pojoValue != null ? !pojoValue.equals(that.pojoValue) : that.pojoValue != null) return false;
        return child != null ? child.equals(that.child) : that.child == null;

    }

    @Override
    public int hashCode() {
        int result = pojoValue != null ? pojoValue.hashCode() : 0;
        result = 31 * result + (child != null ? child.hashCode() : 0);
        return result;
    }
}
