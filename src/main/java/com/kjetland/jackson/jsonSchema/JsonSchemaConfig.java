package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.experimental.FieldDefaults;

@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public final class JsonSchemaConfig {
    
    public static final Map<String, String> DEFAULT_DATE_FORMAT_MAPPING
        = new HashMap<String, String>() {{ 
            // Java7 dates
            put("java.time.LocalDateTime", "datetime-local");
            put("java.time.OffsetDateTime", "datetime");
            put("java.time.LocalDate", "date");

            // Joda-dates
            put("org.joda.time.LocalDate", "date");
        }};

    public static final JsonSchemaConfig DEFAULT = JsonSchemaConfig.builder().build();

    /**
      * Use this configuration if using the JsonSchema to generate HTML5 GUI, eg. by using https://github.com/jdorn/json-editor
      *
      * The following options are enabled:
      *   <li>{@code autoGenerateTitleForProperties} - If property is named "someName", we will add {"title": "Some Name"}
      *   <li>{@code defaultArrayFormat} - this will result in a better gui than te default one.
      */
    public static final JsonSchemaConfig JSON_EDITOR 
             = JsonSchemaConfig.builder()
                .autoGenerateTitleForProperties(true)
                .defaultArrayFormat("table")
                .useOneOfForOption(true)
                .usePropertyOrdering(true)
                .hidePolymorphismTypeProperty(true)
                .useMinLengthForNotNull(true)
                .customType2FormatMapping(DEFAULT_DATE_FORMAT_MAPPING)
                .useMultipleEditorSelectViaProperty(true)
                .uniqueItemClasses(new HashSet<Class<?>>() {{ 
                    add(java.util.Set.class); 
                }})
                .build();

    /**
      * This configuration is exactly like the vanilla JSON schema generator, except that "nullables" have been turned on:
      * `useOneOfForOption` and `useOneForNullables` have both been set to `true`.  With this configuration you can either
      * use `Optional` or `Option`, or a standard nullable Java type and get back a schema that allows nulls.
      *
      *
      * If you need to mix nullable and non-nullable types, you may override the nullability of the type by either setting
      * a `NotNull` annotation on the given property, or setting the `required` attribute of the `JsonProperty` annotation.
      */
    public static final JsonSchemaConfig NULLABLE
            = JsonSchemaConfig.builder()
                    .useOneOfForOption(true)
                    .useOneOfForNullables(true)
                    .build();
    
    @Default boolean autoGenerateTitleForProperties = false;
    @Default String defaultArrayFormat = null;
    @Default boolean useOneOfForOption = false;
    @Default boolean useOneOfForNullables = false;
    @Default boolean usePropertyOrdering = false;
    @Default boolean hidePolymorphismTypeProperty = false;
    @Default boolean useMinLengthForNotNull = false;
    @Default boolean useTypeIdForDefinitionName = false;
    @Default Map<String, String> customType2FormatMapping = new HashMap<>();
    @Default boolean useMultipleEditorSelectViaProperty = false; // https://github.com/jdorn/json-editor/issues/709
    @Default Set<Class<?>> uniqueItemClasses = new HashSet<>(); // If rendering array and type is instanceOf class in this set, then we add 'uniqueItems": true' to schema - See // https://github.com/jdorn/json-editor for more info
    @Default Map<Class<?>, Class<?>> classTypeReMapping = new HashMap<>(); // Can be used to prevent rendering using polymorphism for specific classes.
    @Default Map<String, Supplier<JsonNode>> jsonSuppliers = new HashMap<>(); // Suppliers in this map can be accessed using @JsonSchemaInject(jsonSupplierViaLookup = "lookupKey")
    @Default SubclassesResolver subclassesResolver = new SubclassesResolver();
    @Default boolean failOnUnknownProperties = true;
    @Default List<Class<?>> javaxValidationGroups = new ArrayList<>(); // Used to match against different validation-groups (javax.validation.constraints)
    @Default JsonSchemaDraft jsonSchemaDraft = JsonSchemaDraft.DRAFT_04;
}
