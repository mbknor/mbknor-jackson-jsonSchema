package com.kjetland.jackson.jsonSchema.testData;

import javax.validation.constraints.NotNull;

/**
 * Provides our tests with a simple class: a single field with a {@link javax.validation.constraints.NotNull} annotation
 * set upon a type that is, theoretically nullable.
 * <p>
 * The compiler should allow us to do this, but the schema should then fail to validate.
 */
public class PojoWithNotNull {
    @NotNull
    public Boolean notNullBooleanObject;

    public PojoWithNotNull() {

    }

    public PojoWithNotNull(Boolean notNullBooleanObject) {
        this.notNullBooleanObject = notNullBooleanObject;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PojoWithNotNull that = (PojoWithNotNull) o;

        return notNullBooleanObject != null ? notNullBooleanObject.equals(that.notNullBooleanObject) : that.notNullBooleanObject == null;
    }

    @Override
    public int hashCode() {
        return notNullBooleanObject != null ? notNullBooleanObject.hashCode() : 0;
    }
}
