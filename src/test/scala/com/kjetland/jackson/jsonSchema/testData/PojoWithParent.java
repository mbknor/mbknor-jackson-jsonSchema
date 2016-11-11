package com.kjetland.jackson.jsonSchema.testData;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault;

public class PojoWithParent {

    public Boolean pojoValue;
    public Parent child;

    @JsonSchemaDefault("x")
    public String stringWithDefault;

    @JsonSchemaDefault("12")
    public int intWithDefault;

    @JsonSchemaDefault("true")
    public boolean booleanWithDefault;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PojoWithParent that = (PojoWithParent) o;

        if (intWithDefault != that.intWithDefault) return false;
        if (booleanWithDefault != that.booleanWithDefault) return false;
        if (pojoValue != null ? !pojoValue.equals(that.pojoValue) : that.pojoValue != null) return false;
        if (child != null ? !child.equals(that.child) : that.child != null) return false;
        return stringWithDefault != null ? stringWithDefault.equals(that.stringWithDefault) : that.stringWithDefault == null;

    }

    @Override
    public int hashCode() {
        int result = pojoValue != null ? pojoValue.hashCode() : 0;
        result = 31 * result + (child != null ? child.hashCode() : 0);
        result = 31 * result + (stringWithDefault != null ? stringWithDefault.hashCode() : 0);
        result = 31 * result + intWithDefault;
        result = 31 * result + (booleanWithDefault ? 1 : 0);
        return result;
    }
}
