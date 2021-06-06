package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;

import java.util.Objects;

@JsonSchemaDescription("This is our pojo")
@JsonSchemaTitle("Pojo using format")
@JsonTypeName("OtherTypeName")
public class PojoUsingJsonTypeName {

    public String stringWithDefault;

    public PojoUsingJsonTypeName() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PojoUsingJsonTypeName that = (PojoUsingJsonTypeName) o;
        return Objects.equals(stringWithDefault, that.stringWithDefault);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringWithDefault);
    }
}
