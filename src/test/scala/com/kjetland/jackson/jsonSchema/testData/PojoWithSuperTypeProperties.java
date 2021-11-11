package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import com.kjetland.jackson.jsonSchema.testDataScala.PolymorphismAndTitleBase;

import java.util.Objects;

public class PojoWithSuperTypeProperties {

    public PolymorphismAndTitleBase prop1;

    @JsonSchemaTitle("CustomPropTitle2")
    @JsonPropertyDescription("Custom prop title 2 description")
    public PolymorphismAndTitleBase prop2;

    private PojoWithSuperTypeProperties() {
    }

    public PojoWithSuperTypeProperties(PolymorphismAndTitleBase prop1) {
        this.prop1 = prop1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PojoWithSuperTypeProperties that = (PojoWithSuperTypeProperties) o;
        return prop1.equals(that.prop1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prop1);
    }

    @Override
    public String toString() {
        return "PojoWithSuperTypeProperties{" +
            "prop1=" + prop1 +
            '}';
    }
}
