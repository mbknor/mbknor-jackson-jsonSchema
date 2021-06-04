package com.kjetland.jackson.jsonSchema;

import java.io.IOException;
import java.util.Collections;
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
		public String somePayload;
	}
	public static class Abs extends Pt {
	}
	public static class Line extends Loc {
		public List<Proxy> attr;
	}
	public static class Proxy {
		public List<Pt> attr;
	}

	private final ObjectMapper MAPPER = new ObjectMapper();

	public void testGenerateSchema() throws IOException {
		com.kjetland.jackson.jsonSchema.JsonSchemaGenerator generator = new com.kjetland.jackson.jsonSchema.JsonSchemaGenerator(MAPPER);
		ObjectWriter objectWriter = MAPPER.writerWithDefaultPrettyPrinter();
		JsonNode jsonNode = generator.generateJsonSchema(Loc.class);
		System.out.println(objectWriter.writeValueAsString(jsonNode));
		String value = objectWriter.writeValueAsString(new Pt());
		System.out.println(value);
		Pt pt = MAPPER.readValue(value, Pt.class);
		Line line = new Line();
		Proxy proxy = new Proxy();
		proxy.attr = Collections.singletonList(new Abs());
		line.attr = Collections.singletonList(proxy);
		value = objectWriter.writeValueAsString(line);
		System.out.println(value);
		line = MAPPER.readValue(value, Line.class);
	}

	public static void main(String[] args) throws IOException {
		new SubtypeOrderTest().testGenerateSchema();
	}
}
