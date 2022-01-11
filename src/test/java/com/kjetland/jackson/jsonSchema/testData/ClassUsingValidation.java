package com.kjetland.jackson.jsonSchema.testData;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.*;
import javax.validation.groups.Default;

public record ClassUsingValidation
(
  @NotNull
  String stringUsingNotNull,

  @NotBlank
  String stringUsingNotBlank,

  @NotNull
  @NotBlank
  String stringUsingNotBlankAndNotNull,

  @NotEmpty
  String stringUsingNotEmpty,

  @NotEmpty
  List<String> notEmptyStringArray, // Per PojoArraysWithScala, we use always use Lists in Scala, and never raw arrays.

  @NotEmpty
  Map<String, String> notEmptyMap,

  @Size(min=1, max=20)
  String stringUsingSize,

  @Size(min=1)
  String stringUsingSizeOnlyMin,

  @Size(max=30)
  String stringUsingSizeOnlyMax,

  @Pattern(regexp = "_stringUsingPatternA|_stringUsingPatternB")
  String stringUsingPattern,

  @Pattern.List({
    @Pattern(regexp = "^_stringUsing.*"),
    @Pattern(regexp = ".*PatternList$")
  })
  String stringUsingPatternList,

  @Min(1)
  int intMin,
  @Max(10)
  int intMax,
  @Min(1)
  double doubleMin,
  @Max(10)
  double doubleMax,
  @DecimalMin("1.5")
  double decimalMin,
  @DecimalMax("2.5")
  double decimalMax,

  @Email
  String email
)
{
    
}