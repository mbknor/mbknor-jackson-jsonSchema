package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.*;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class JsonSchemaGenerator {
    
    final ObjectMapper objectMapper;
    final JsonSchemaConfig config;

    /**
     * JSON Schema Generator.
     * @param rootObjectMapper pre-configured ObjectMapper
     */
    public JsonSchemaGenerator(ObjectMapper rootObjectMapper) {
        this(rootObjectMapper, JsonSchemaConfig.DEFAULT);
    }

    /**
     * JSON Schema Generator.
     * @param rootObjectMapper pre-configured ObjectMapper
     * @param config by default, {@link JsonSchemaConfig#DEFAULT}. 
     *     Use {@link JsonSchemaConfig#JSON_EDITOR} for {@link https://github.com/jdorn/json-editor JSON GUI}.
     */
    public JsonSchemaGenerator(ObjectMapper rootObjectMapper, JsonSchemaConfig config) {
        this.objectMapper = rootObjectMapper;
        this.config = config;
    }

    public JsonNode generateJsonSchema(Class<?> clazz) throws JsonMappingException { 
        return generateJsonSchema(clazz, null, null);
    }
    
    public JsonNode generateJsonSchema(JavaType javaType) throws JsonMappingException { 
        return generateJsonSchema(javaType, null, null); 
    }
    
    public JsonNode generateJsonSchema(Class<?> clazz, String title, String description) throws JsonMappingException {

        var clazzToUse = tryToReMapType(clazz);

        var javaType = objectMapper.constructType(clazzToUse);

        return generateJsonSchema(javaType, title, description);
    }

    public JsonNode generateJsonSchema(JavaType javaType, String title, String description) throws JsonMappingException {

        var rootNode = JsonNodeFactory.instance.objectNode();

        rootNode.put("$schema", config.jsonSchemaDraft.url);

        if (title == null)
            title = Utils.camelCaseToSentenceCase(javaType.getRawClass().getSimpleName());
        if (!title.isEmpty())
            // If root class is annotated with @JsonSchemaTitle, it will later override this title
            rootNode.put("title", title);

        if (description != null)
            // If root class is annotated with @JsonSchemaDescription, it will later override this description
            rootNode.put("description", description);


        var definitionsHandler = new DefinitionsHandler(config);
        var rootVisitor = new JsonSchemaGeneratorVisitor(this, 0, rootNode, definitionsHandler, null);


        objectMapper.acceptJsonFormatVisitor(javaType, rootVisitor);

        var definitionsNode = definitionsHandler.getFinalDefinitionsNode();
        if (definitionsNode != null)
            rootNode.set("definitions", definitionsNode);

        return rootNode;
    }
    

    JavaType tryToReMapType(JavaType originalType) {
        Class<?> mappedToClass = config.classTypeReMapping.get(originalType.getRawClass());
        if (mappedToClass != null) {
            log.trace("Class {} is remapped to {}", originalType, mappedToClass);
            return objectMapper.getTypeFactory().constructType(mappedToClass);
        }
        else
            return originalType;
    }
    
    Class<?> tryToReMapType(Class<?> originalClass) {
        Class<?> mappedToClass = config.classTypeReMapping.get(originalClass);
        if (mappedToClass != null) {
            log.trace("Class {} is remapped to {}", originalClass, mappedToClass);
            return mappedToClass;
        }
        else
            return originalClass;
    }

    String  resolvePropertyFormat(JavaType type) {
        var omConfig = objectMapper.getDeserializationConfig();
        var annotatedClass = AnnotatedClassResolver.resolve(omConfig, type, omConfig);
        var annotation = annotatedClass.getAnnotation(JsonSchemaFormat.class);
        if (annotation != null)
            return annotation.value();
        
        var rawClassName = type.getRawClass().getName();
        return config.customType2FormatMapping.get(rawClassName);
    }

    String resolvePropertyFormat(BeanProperty prop) {
        var annotation = prop.getAnnotation(JsonSchemaFormat.class);
        if (annotation != null)
            return annotation.value();
        
        var rawClassName = prop.getType().getRawClass().getName();
        return config.customType2FormatMapping.get(rawClassName);
    }
    
    /** Tries to retrieve an annotation and validates that it is applicable. */
    <T extends Annotation> T selectAnnotation(BeanProperty prop, Class<T> annotationClass) {
        if (prop == null)
            return null;
        var ann = prop.getAnnotation(annotationClass);
        if (ann == null || !annotationIsApplicable(ann))
            return null;
        return ann;
    }

    <T extends Annotation> T selectAnnotation(AnnotatedClass annotatedClass, Class<T> annotationClass) {
        var ann = annotatedClass.getAnnotation(annotationClass);
        if (ann == null || !annotationIsApplicable(ann))
            return null;
        return ann;
    }
    
    // Checks to see if a javax.validation field that makes our field required is present.
    boolean validationAnnotationRequired(BeanProperty prop) {
        return selectAnnotation(prop, NotNull.class) != null
                || selectAnnotation(prop, NotBlank.class) != null
                || selectAnnotation(prop, NotEmpty.class) != null;
    }

    /** Verifies that the annotation is applicable based on the config.javaxValidationGroups. */
    boolean annotationIsApplicable(Annotation annotation) {
        var desiredGroups = config.javaxValidationGroups;
        if (desiredGroups == null || desiredGroups.isEmpty())
            desiredGroups = List.of (Default.class);

        var annotationGroups = Utils.extractGroupsFromAnnotation(annotation);
        if (annotationGroups.isEmpty())
            annotationGroups = List.of (Default.class);

        for (var group : annotationGroups)
            if (desiredGroups.contains (group))
                return true;
        return false;
    }
    
    TypeSerializer getTypeSerializer(JavaType baseType) throws JsonMappingException {

        return objectMapper
                .getSerializerFactory()
                .createTypeSerializer(objectMapper.getSerializationConfig(), baseType);
    }
    
    
    /**
     * @returns the value of merge
     */
    boolean injectFromAnnotation(ObjectNode node, JsonSchemaInject injectAnnotation) throws JsonMappingException {
        // Must parse json
        JsonNode injectedNode;
        try {
            injectedNode = objectMapper.readTree(injectAnnotation.json());
        }
        catch(JsonProcessingException e) {
            throw new JsonMappingException("Could not parse JsonSchemaInject.json", e);
        }
        
        // Apply the JSON supplier (may be a no-op)
        try {
            var jsonSupplier = injectAnnotation.jsonSupplier().newInstance();
            var jsonNode = jsonSupplier.get();
            if (jsonNode != null)
                Utils.merge (injectedNode, jsonNode);
        }
        catch (InstantiationException|IllegalAccessException e) {
            throw new JsonMappingException("Could not call JsonSchemaInject.jsonSupplier constructor", e);
        }
        
        // Apply the JSON-supplier-via-lookup
        if (!injectAnnotation.jsonSupplierViaLookup().isEmpty()) {
            var jsonSupplier = config.jsonSuppliers.get(injectAnnotation.jsonSupplierViaLookup());
            if (jsonSupplier == null)
                throw new JsonMappingException("@JsonSchemaInject(jsonSupplierLookup='"+injectAnnotation.jsonSupplierViaLookup()+"') does not exist in ctx.config.jsonSupplierLookup-map");
            var jsonNode = jsonSupplier.get();
            if (jsonNode != null)
                Utils.merge(injectedNode, jsonNode);
        }
        
        // 
        for (var v : injectAnnotation.strings())
            Utils.visit(injectedNode, v.path(), (o, n) -> o.put(n, v.value()));
        for (var v : injectAnnotation.ints())
            Utils.visit(injectedNode, v.path(), (o, n) -> o.put(n, v.value()));
        for (var v : injectAnnotation.bools())
            Utils.visit(injectedNode, v.path(), (o, n) -> o.put(n, v.value()));

        var injectOverridesAll = injectAnnotation.overrideAll();
        if (injectOverridesAll) {
          // Since we're not merging, we must remove all content of thisObjectNode before injecting.
          // We cannot just "replace" it with injectJsonNode, since thisObjectNode already have been added to its parent
          node.removeAll();
        }

        Utils.merge(node, injectedNode);

        return injectOverridesAll;
    }
}
