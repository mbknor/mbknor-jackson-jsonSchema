package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UtilityClass // Do not static import!
public final class TestUtils {
    
    @SneakyThrows
    String asPrettyJson(JsonNode node, ObjectMapper om) {
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    // Asserts that we're able to go from object => json => equal object
    @SneakyThrows
    JsonNode assertToFromJson(JsonSchemaGenerator g, Object o) {
        return assertToFromJson(g, o, o.getClass());
    }

    // Asserts that we're able to go from object => json => equal object
    // desiredType might be a class which o extends (polymorphism)
    @SneakyThrows
    JsonNode assertToFromJson(JsonSchemaGenerator g, Object o, Class<?> desiredType) {
        var json = g.objectMapper.writeValueAsString(o);
        System.out.println("json: " + json);
        var jsonNode = g.objectMapper.readTree(json);
        var r = g.objectMapper.treeToValue(jsonNode, desiredType);
        assertEquals (o , r);
        return jsonNode;
    }

    @SneakyThrows
    void useSchema(JsonNode schema, @Nullable JsonNode json) {
        var schemaValidator = JsonSchemaFactory.byDefault().getJsonSchema(schema);
        if (json != null) {
            var r = schemaValidator.validate(json);
            if (!r.isSuccess())
                throw new Exception("json does not validate against schema: " + r);
        }
    }
    
    
    /** 
     * Generates schema, validates the schema using external schema validator and
     * Optionally tries to validate json against the schema.
     */
    @SneakyThrows
    JsonNode generateAndValidateSchema(
            JsonSchemaGenerator g,
            Class<?> clazz, 
            @Nullable JsonNode jsonToTestAgainstSchema) {
        return generateAndValidateSchema(g, clazz, jsonToTestAgainstSchema, JsonSchemaDraft.DRAFT_04);
    }
    
    /** 
     * Generates schema, validates the schema using external schema validator and
     * Optionally tries to validate json against the schema.
     */
    @SneakyThrows
    JsonNode generateAndValidateSchema(
            JsonSchemaGenerator g,
            Class<?> clazz, 
            @Nullable JsonNode jsonToTestAgainstSchema,
            JsonSchemaDraft jsonSchemaDraft) {
      var schema = g.generateJsonSchema(clazz);

      out.println("--------------------------------------------");
      out.println(asPrettyJson(schema, g.objectMapper));

      assertEquals (jsonSchemaDraft.url, schema.at("/$schema").asText());

      useSchema(schema, jsonToTestAgainstSchema);

      return schema;
    }
    
    /** 
     * Generates schema, validates the schema using external schema validator and
     * Optionally tries to validate json against the schema.
     */
    @SneakyThrows
    JsonNode generateAndValidateSchema(
            JsonSchemaGenerator g,
            JavaType javaType,
            @Nullable JsonNode jsonToTestAgainstSchema) {
        return generateAndValidateSchema(g, javaType, jsonToTestAgainstSchema, JsonSchemaDraft.DRAFT_04);
    }

    /** 
     * Generates schema, validates the schema using external schema validator and
     * Optionally tries to validate json against the schema.
     */
    @SneakyThrows
    JsonNode generateAndValidateSchema(
            JsonSchemaGenerator g,
            JavaType javaType,
            @Nullable JsonNode jsonToTestAgainstSchema,
            JsonSchemaDraft jsonSchemaDraft) {
        
        var schema = g.generateJsonSchema(javaType);

        out.println("--------------------------------------------");
        out.println(asPrettyJson(schema, g.objectMapper));

        assertEquals (jsonSchemaDraft.url, schema.at("/$schema").asText());

        useSchema(schema, jsonToTestAgainstSchema);

        return schema;
    }
    
    void assertJsonSubTypesInfo(JsonNode node, String typeParamName, String typeName) {
        assertJsonSubTypesInfo(node, typeParamName, typeName, false);
    }
    
    void assertJsonSubTypesInfo(JsonNode node, String typeParamName, String typeName, boolean html5Checks) {
        /*
        "properties" : {
          "type" : {
            "type" : "string",
            "enum" : [ "child1" ],
            "default" : "child1"
          },
        },
        "title" : "child1",
        "required" : [ "type" ]
         */
        assertEquals (node.at("/properties/" + typeParamName + "/type").asText(), "string");
        assertEquals (node.at("/properties/" + typeParamName + "/enum/0").asText(), typeName);
        assertEquals (node.at("/properties/" + typeParamName + "/default").asText(), typeName);
        assertEquals (node.at("/title").asText(), typeName);
        assertPropertyRequired(node, typeParamName, true);

        if (html5Checks) {
            assertTrue(node.at("/properties/" + typeParamName + "/options/hidden").asBoolean());
            assertEquals (node.at("/options/multiple_editor_select_via_property/property").asText(), typeParamName);
            assertEquals (node.at("/options/multiple_editor_select_via_property/value").asText(), typeName);
        } else {
            assertTrue(node.at("/options/multiple_editor_select_via_property/property") instanceof MissingNode);
        }
    }

    List<String> getArrayNodeAsListOfStrings(JsonNode node) {
        if (node instanceof MissingNode)
            return List.of();
        else
            return StreamSupport.stream(node.spliterator(), false).map(JsonNode::asText).toList();
    }

    List<String> getRequiredList(JsonNode node) {
        return getArrayNodeAsListOfStrings(node.at("/required"));
    }
    
    void assertPropertyRequired(JsonNode schema, String propertyName, boolean required) {
        if (required) {
            assertTrue (getRequiredList(schema).contains(propertyName));
        } else {
            assertFalse (getRequiredList(schema).contains(propertyName));
        }
    }
    
    boolean isPropertyRequired(JsonNode schema, String propertyName) {
        return getRequiredList(schema).contains(propertyName);
    }
    
    JsonNode getNodeViaRefs(JsonNode root, String pathToArrayOfRefs, String definitionName) {
      List<JsonNode> arrayItemNodes = new ArrayList<>();
      var child = root.at(pathToArrayOfRefs);
      if (child instanceof ArrayNode)
          for (var e : child)
              arrayItemNodes.add(e);
      else
          arrayItemNodes.add((ObjectNode)child);
      
      var ref = arrayItemNodes.stream()
              .map(a -> a.get("$ref").asText().substring(1))
              .filter(a -> a.endsWith("/"+definitionName+""))
              .findFirst().get();
      
      return root.at(ref);
    }

    ObjectNode getNodeViaRefs(JsonNode root, JsonNode nodeWithRef, String definitionName) {
      var ref = nodeWithRef.at("/$ref").asText();
      assertTrue (ref.endsWith("/"+definitionName+""));
      // use ref to look the node up
      var fixedRef = ref.substring(1); // Removing starting #
      return (ObjectNode) root.at(fixedRef);
    }
    
    <T> List<T> toList(Iterator<T> iterator) {
        var list = new ArrayList<T>();
        while (iterator.hasNext())
            list.add (iterator.next());
        return list;
    }
    
    void assertNullableType(JsonNode node, String path, String expectedType) {
        var nullType = node.at(path).at("/oneOf/0");
        assertEquals (nullType.at("/type").asText(), "null");
        assertEquals (nullType.at("/title").asText(), "Not included");

        var valueType = node.at(path).at("/oneOf/1");
        assertEquals (valueType.at("/type").asText(), expectedType);

        var pathParts = path.split("/");
        var propName = pathParts[pathParts.length - 1];
        assertFalse(getRequiredList(node).contains(propName));
    }
}
