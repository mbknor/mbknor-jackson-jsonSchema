package com.kjetland.jackson.jsonSchema.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Use this annotation to inject json into the generated jsonSchema.
 */
@Target({METHOD, FIELD, PARAMETER, TYPE})
@Retention(RUNTIME)
public @interface JsonSchemaInject {
    /**
     * @return a raw json that will be merged on top of the generated jsonSchema
     */
    String json() default "{}";

    /**
     * @return a collection of key/value pairs to merge on top of the generated jsonSchema and applied after {@link #json()}
     */
    JsonSchemaString[] strings() default {};

    /**
     * @return a collection of key/value pairs to merge on top of the generated jsonSchema and applied after {@link #json()}
     */
    JsonSchemaInt[] ints() default {};

    /**
     * @return a collection of key/value pairs to merge on top of the generated jsonSchema and applied after {@link #json()}
     */
    JsonSchemaBool[] bools() default {};
}

