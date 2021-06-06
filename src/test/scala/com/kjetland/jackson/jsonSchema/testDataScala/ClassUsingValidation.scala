package com.kjetland.jackson.jsonSchema.testDataScala

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject
import javax.validation.constraints._
import javax.validation.groups.Default

case class ClassUsingValidation
(
  @NotNull
  stringUsingNotNull:String,

  @NotBlank
  stringUsingNotBlank:String,

  @NotNull
  @NotBlank
  stringUsingNotBlankAndNotNull:String,

  @NotEmpty
  stringUsingNotEmpty:String,

  @NotEmpty
  notEmptyStringArray:List[String], // Per PojoArraysWithScala, we use always use Lists in Scala, and never raw arrays.

  @NotEmpty
  notEmptyMap:Map[String, String],

  @Size(min=1, max=20)
  stringUsingSize:String,

  @Size(min=1)
  stringUsingSizeOnlyMin:String,

  @Size(max=30)
  stringUsingSizeOnlyMax:String,

  @Pattern(regexp = "_stringUsingPatternA|_stringUsingPatternB")
  stringUsingPattern:String,

  @Pattern.List(Array(
    new Pattern(regexp = "^_stringUsing.*"),
    new Pattern(regexp = ".*PatternList$")
  ))
  stringUsingPatternList:String,

  @Min(1)
  intMin:Int,
  @Max(10)
  intMax:Int,
  @Min(1)
  doubleMin:Double,
  @Max(10)
  doubleMax:Double,
  @DecimalMin("1.5")
  decimalMin:Double,
  @DecimalMax("2.5")
  decimalMax:Double,

  @Email
  email:String
)

trait ValidationGroup1
trait ValidationGroup2
trait ValidationGroup3_notInUse

@JsonSchemaInject(json = """{"injected":true}""", javaxValidationGroups = Array(classOf[ValidationGroup1]))
case class ClassUsingValidationWithGroups
(
  @NotNull
  @JsonSchemaInject(json = """{"injected":true}""")
  noGroup:String,

  @NotNull(groups = Array(classOf[Default]))
  @JsonSchemaInject(json = """{"injected":true}""", javaxValidationGroups = Array(classOf[Default]))
  defaultGroup:String,

  @NotNull(groups = Array(classOf[ValidationGroup1]))
  @JsonSchemaInject(json = """{"injected":true}""", javaxValidationGroups = Array(classOf[ValidationGroup1]))
  group1:String,

  @NotNull(groups = Array(classOf[ValidationGroup2]))
  @JsonSchemaInject(json = """{"injected":true}""", javaxValidationGroups = Array(classOf[ValidationGroup2]))
  group2:String,

  @NotNull(groups = Array(classOf[ValidationGroup1], classOf[ValidationGroup2]))
  @JsonSchemaInject(json = """{"injected":true}""", javaxValidationGroups = Array(classOf[ValidationGroup1], classOf[ValidationGroup2]))
  group12:String

)
