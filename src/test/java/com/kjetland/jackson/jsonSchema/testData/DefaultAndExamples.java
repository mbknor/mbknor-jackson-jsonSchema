package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaExamples;


public record DefaultAndExamples
(
  @JsonSchemaExamples({"user@example.com"})
  String emailValue,
        
  @JsonSchemaDefault("12")
  @JsonSchemaExamples({"10", "14", "18"})
  int fontSize,

  @JsonProperty(defaultValue = "ds")
  String defaultStringViaJsonValue,

  @JsonProperty(defaultValue = "1")
  int defaultIntViaJsonValue,

  @JsonProperty(defaultValue = "true")
  boolean defaultBoolViaJsonValue
)
{
}