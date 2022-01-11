package com.kjetland.jackson.jsonSchema.annotations;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;

import com.fasterxml.jackson.databind.JsonNode;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * Use this annotation to inject JSON into the generated jsonSchema.
 * When applied to a class, will be injected into the object type node.
 * When applied to a property, will be injected into the property node.
 */
@Target({METHOD, FIELD, PARAMETER, TYPE})
@Retention(RUNTIME)
public @interface JsonSchemaInject {
    /**
     * JSON that will injected into the object or property node.
     */
    String json() default "{}";

    /**
     * Supplier of a JsonNode that will be injected. Applied after {@link #json()}.
     */
    Class<? extends Supplier<JsonNode>> jsonSupplier() default None.class;

    /**
     * Key to entry in {@link JsonSchemaConfig#jsonSuppliers} which will supply
     * a JsonNode that will be injected. Applied after {@link #jsonSupplier()}.
     */
    String jsonSupplierViaLookup() default "";

    /**
     * Collection of String key/value pairs that will be injected. Applied after {@link #jsonSupplierViaLookup()}.
     */
    JsonSchemaString[] strings() default {};

    /**
     * Collection of Integer key/value pairs that will be injected. Applied after {@link #strings()}.
     */
    JsonSchemaInt[] ints() default {};

    /**
     * Collection of Boolean key/value pairs that will be injected. Applied after {@link #ints()}.
     */
    JsonSchemaBool[] bools() default {};

    /**
     * If overrideAll is false (the default), the injected json will be merged with the generated schema. 
     * If overrideAll is true, then we skip schema generation and use only the injected json.
     */
    boolean overrideAll() default false;

    // This can be used in the same way as 'groups' in javax.validation.constraints
    Class<?>[] javaxValidationGroups() default { };

    class None implements Supplier<JsonNode> {
        @Override
        public JsonNode get() {
            return null;
        }
    }
}

