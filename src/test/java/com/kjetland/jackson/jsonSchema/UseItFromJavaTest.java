package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.*;

public class UseItFromJavaTest {

    static class MyJavaPojo {
        public String name;
    }

    public UseItFromJavaTest() {
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
        JsonSchemaConfig config = JsonSchemaConfig.create(
                true,
                Optional.of("A"),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                customMapping,
                false,
                new HashSet<>(),
                new HashMap<>(),
                new HashMap<>(),
                null,
                true,
                null);
        JsonSchemaGenerator g2 = new JsonSchemaGenerator(objectMapper, config);


        // Config SubclassesResolving

        final SubclassesResolver subclassesResolver = new SubclassesResolverImpl()
                .withClassesToScan(Arrays.asList(
                        "this.is.myPackage"
                ));
    }

}
