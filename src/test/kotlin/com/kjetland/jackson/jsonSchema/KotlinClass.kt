package com.kjetland.jackson.jsonSchema

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
sealed class SampleCreator {

	@JsonTypeName("author")
	data class Author(
		val name: String,
	) : SampleCreator()

	@JsonTypeName("actor")
	data class Actor(
		val name: String,
	) : SampleCreator()

	sealed class Artist : SampleCreator() {
		@JsonTypeName("band")
		data class Band(val name: String) : Artist()

		@JsonTypeName("performer")
		data class Performer(
			val name: String,
		) : Artist()
	}
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes(
	JsonSubTypes.Type(SampleCreator.Author::class, name="author"),
	JsonSubTypes.Type(SampleCreator.Actor::class, name="actor"),
	JsonSubTypes.Type(SampleCreator.Artist::class, name="artist"),
)
sealed class SampleCreatorWithSubtypes {

	@JsonTypeName("author")
	data class Author(
		val name: String,
	) : SampleCreatorWithSubtypes()

	@JsonTypeName("actor")
	data class Actor(
		val name: String,
	) : SampleCreatorWithSubtypes()

	sealed class Artist : SampleCreatorWithSubtypes() {
		@JsonTypeName("band")
		data class Band(val name: String) : Artist()

		@JsonTypeName("performer")
		data class Performer(
			val name: String,
		) : Artist()
	}
}
