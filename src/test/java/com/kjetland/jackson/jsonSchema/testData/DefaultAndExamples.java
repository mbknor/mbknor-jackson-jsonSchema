package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.extern.jackson.Jacksonized;

@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true) @Jacksonized @Builder @EqualsAndHashCode // Can be Java 17 record
public class DefaultAndExamples
{
  @JsonSchemaExamples({"user@example.com"})
  String emailValue;
        
  @JsonSchemaDefault("12")
  @JsonSchemaExamples({"10", "14", "18"})
  int fontSize;

  @JsonProperty(defaultValue = "ds")
  String defaultStringViaJsonValue;

  @JsonProperty(defaultValue = "1")
  int defaultIntViaJsonValue;

  @JsonProperty(defaultValue = "true")
  boolean defaultBoolViaJsonValue;
}