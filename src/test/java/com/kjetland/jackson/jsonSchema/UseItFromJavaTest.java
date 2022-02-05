package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;

public class UseItFromJavaTest {

    static class MyJavaPojo {
        public String name;
    }

    @Test
    public void test() throws JsonMappingException {
        // Just make sure it compiles
        ObjectMapper objectMapper = new ObjectMapper();
        JsonSchemaGenerator g1 = new JsonSchemaGenerator(objectMapper);
        // TODO - This is not very beautiful from Java - Need to improve Java API
        g1.generateJsonSchema(MyJavaPojo.class);
        g1.generateJsonSchema(MyJavaPojo.class, "My title", "My description");

        g1.generateJsonSchema(objectMapper.constructType(MyJavaPojo.class));
        g1.generateJsonSchema(objectMapper.constructType(MyJavaPojo.class), "My title", "My description");

        // Create custom JsonSchemaConfig from java
        Map<String,String> customMapping = new HashMap<>();
        customMapping.put(OffsetDateTime.class.getName(), "date-time");
        JsonSchemaConfig config = JsonSchemaConfig.builder()
                .autoGenerateTitleForProperties(true)
                .defaultArrayFormat("A")
                .useOneOfForOption(false)
                .useOneOfForNullables(true)
                .usePropertyOrdering(true)
                .hidePolymorphismTypeProperty(true)
                .useMinLengthForNotNull(true)
                .useTypeIdForDefinitionName(true)
                .customType2FormatMapping(customMapping)
                .useMultipleEditorSelectViaProperty(false)
                .subclassesResolver(null)
                .failOnUnknownProperties(true)
                .javaxValidationGroups(null)
                .build();
        JsonSchemaGenerator g2 = new JsonSchemaGenerator(objectMapper, config);

        // Config SubclassesResolving
        final SubclassesResolver subclassesResolver 
                = new SubclassesResolver
                    (null
                    , Arrays.asList(
                            "this.is.myPackage"
                    ));
    }

}
