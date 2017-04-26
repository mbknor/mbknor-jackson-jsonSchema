package com.kjetland.jackson.jsonSchema.testDataScala

import javax.validation.constraints._

case class ClassUsingValidation
(
  @NotNull
  stringUsingNotNull:String,

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
  doubleMax:Double


)
