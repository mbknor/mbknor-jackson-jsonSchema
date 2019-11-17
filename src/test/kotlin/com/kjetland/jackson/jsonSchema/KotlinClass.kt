package com.kjetland.jackson.jsonSchema

data class KotlinClass(
	val a:String,
	val b:Int
)

data class KotlinWithDefaultValues(
	val optional: String?,
	val required: String,
	val optionalDefault: String = "Hello",
	val optionalDefaultNull: String? = "Hello"
)
