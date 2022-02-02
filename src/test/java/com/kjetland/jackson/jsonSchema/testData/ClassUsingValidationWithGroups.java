package com.kjetland.jackson.jsonSchema.testData;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import com.kjetland.jackson.jsonSchema.testData.ClassUsingValidationWithGroups.ValidationGroup1;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.extern.jackson.Jacksonized;

@JsonSchemaInject(json = "{\"injected\":true}", javaxValidationGroups = { ValidationGroup1.class })
@EqualsAndHashCode @FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true) @Jacksonized @Builder // Can be Java 17 record
public class ClassUsingValidationWithGroups
{
    @NotNull
    @JsonSchemaInject(json = "{\"injected\":true}")
    String noGroup;

    @NotNull(groups = { Default.class })
    @JsonSchemaInject(json = "{\"injected\":true}", javaxValidationGroups = { Default.class })
    String defaultGroup;

    @NotNull(groups = { ValidationGroup1.class })
    @JsonSchemaInject(json = "{\"injected\":true}", javaxValidationGroups = { ValidationGroup1.class })
    String group1;

    @NotNull(groups = { ValidationGroup2.class })
    @JsonSchemaInject(json = "{\"injected\":true}", javaxValidationGroups = { ValidationGroup2.class })
    String group2;

    @NotNull(groups = { ValidationGroup1.class, ValidationGroup2.class })
    @JsonSchemaInject(json = "{\"injected\":true}", javaxValidationGroups = { ValidationGroup1.class, ValidationGroup2.class })
    String group12;
    
    public interface ValidationGroup1 {}
    public interface ValidationGroup2 {}
    public interface ValidationGroup3_notInUse {}
}