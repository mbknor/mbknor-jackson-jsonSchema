package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import scala.None;
import scala.Option;

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
    }

}
