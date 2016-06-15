package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PojoWithCustomSerializerSerializer extends JsonSerializer<PojoWithCustomSerializer> {

    @Override
    public void serialize(PojoWithCustomSerializer value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
        gen.writeString(value.myString+"**");
    }
}
