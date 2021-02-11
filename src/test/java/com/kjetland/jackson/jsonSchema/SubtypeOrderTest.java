package com.kjetland.jackson.jsonSchema;

import java.io.IOException;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class SubtypeOrderTest {

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
	@JsonSubTypes({
			@JsonSubTypes.Type(Line.class),
			@JsonSubTypes.Type(Pt.class) })
	public static class Loc  {
	}
	@JsonSubTypes({
			@JsonSubTypes.Type(Abs.class)
	})
	public static class Pt extends Loc {
	}
	public static class Abs extends Pt {
	}
	public static class Line extends Loc {
		public List<Pt> attr;
	}

	private final ObjectMapper MAPPER = new ObjectMapper();

	public void testGenerateSchema() throws IOException {
		com.kjetland.jackson.jsonSchema.JsonSchemaGenerator generator = new com.kjetland.jackson.jsonSchema.JsonSchemaGenerator(MAPPER);
		ObjectWriter objectWriter = MAPPER.writerWithDefaultPrettyPrinter();
		JsonNode jsonNode = generator.generateJsonSchema(Loc.class);
		System.out.println(objectWriter.writeValueAsString(jsonNode));

	}
}
