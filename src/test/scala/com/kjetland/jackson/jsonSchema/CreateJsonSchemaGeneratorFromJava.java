package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CreateJsonSchemaGeneratorFromJava {
    public CreateJsonSchemaGeneratorFromJava(ObjectMapper objectMapper) {
        JsonSchemaGenerator vanilla = new JsonSchemaGenerator(objectMapper);
        JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema());
        JsonSchemaGenerator nullable = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.nullableJsonSchemaDraft4());
    }
}
