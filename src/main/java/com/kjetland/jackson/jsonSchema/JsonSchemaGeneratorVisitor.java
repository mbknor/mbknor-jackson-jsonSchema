package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsonFormatVisitors.*;
import com.fasterxml.jackson.databind.jsontype.impl.MinimalClassNameIdResolver;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kjetland.jackson.jsonSchema.DefinitionsHandler.DefinitionInfo;
import com.kjetland.jackson.jsonSchema.annotations.*;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
class JsonSchemaGeneratorVisitor extends AbstractJsonFormatVisitorWithSerializerProvider implements JsonFormatVisitorWrapper {
    
    final JsonSchemaGenerator ctx;
    
    final int level; // = 0
    final ObjectNode node; // = JsonNodeFactory.instance.objectNode()
    final DefinitionsHandler definitionsHandler;
    final BeanProperty currentProperty; // This property may represent the BeanProperty when we're directly processing beneath the property
    
    /** Tries to retrieve an annotation and validates that it is applicable. */
    private <T extends Annotation> T tryGetAnnotation(Class<T> annotationClass) {
        return ctx.selectAnnotation(currentProperty, annotationClass);
    }

    JsonSchemaGeneratorVisitor createChildVisitor(ObjectNode childNode, BeanProperty currentProperty)  {
        return new JsonSchemaGeneratorVisitor(ctx, level + 1, childNode, definitionsHandler, currentProperty);
    }

    String extractDefaultValue() {
        // Scala way (ugly and confusing)
//        return selectAnnotation(p, JsonProperty.class)
//                .map(JsonProperty::defaultValue)
//                .or (() ->
//                    selectAnnotation(p, JsonSchemaDefault.class)
//                        .map (JsonSchemaDefault::value));
        
        // Plain java
        var jp = tryGetAnnotation(JsonProperty.class);
        if (jp != null 
                && jp.defaultValue() != null
                && !jp.defaultValue().isEmpty()) // unfortunately, defaultValue is empty by default
            return jp.defaultValue();
        
        var jsd = tryGetAnnotation(JsonSchemaDefault.class);
        if (jsd != null)
            return jsd.value();
        
        return null;

        // Hypothetical null operators
//        return selectAnnotation(p, JsonProperty.class)?.defaultValue()
//            ?? selectAnnotation(p, JsonSchemaDefault.class)?.value();
    }
    
//    @RequiredArgsConstructor
    class MyJsonValueFormatVisitor
            extends AbstractJsonFormatVisitorWithSerializerProvider
            implements 
                JsonStringFormatVisitor,
                JsonNumberFormatVisitor, 
                JsonIntegerFormatVisitor, 
                JsonBooleanFormatVisitor {
        
        @Override 
        public void format(JsonValueFormat format) {
            node.put("format", format.toString());
        }

        @Override
        public void enumTypes(Set<String> enums) {
            log.trace("JsonStringFormatVisitor-enum.enumTypes: {}", enums);

            var enumValuesNode = JsonNodeFactory.instance.arrayNode();
            for (var e : enums)
                enumValuesNode.add(e);
            node.set("enum", enumValuesNode);
        }
        
        @Override public void numberType(JsonParser.NumberType type) {
            log.trace("JsonNumberFormatVisitor.numberType: {}", type);
        }
    }

    @Override 
    public JsonStringFormatVisitor expectStringFormat(JavaType type) {
        log.trace("expectStringFormat {}", type);

        node.put("type", "string");

        var notBlankAnnotation = tryGetAnnotation(NotBlank.class);
        if (notBlankAnnotation != null)
            node.put("pattern", "^.*\\S+.*$");

        var patternAnnotation = tryGetAnnotation(Pattern.class);
        if (patternAnnotation != null)
            node.put("pattern", patternAnnotation.regexp());

        var patternListAnnotation = tryGetAnnotation(Pattern.List.class);
        if (patternListAnnotation != null) {
            String pattern = "^";
            for (var p : patternListAnnotation.value())
                pattern += "(?=" + p.regexp() + ")";
            pattern += ".*$";
            node.put("pattern", pattern);
        }

        var defaultValue = extractDefaultValue();
        if (defaultValue != null)
            node.put("default", defaultValue);

        // Look for @JsonSchemaExamples
        var examplesAnnotation = tryGetAnnotation(JsonSchemaExamples.class);
        if (examplesAnnotation != null) {
            var examples = JsonNodeFactory.instance.arrayNode();
            for (var example : examplesAnnotation.value())
                examples.add(example);
            node.set("examples", examples);
        }

        // Look for @Email
        var emailAnnotation = tryGetAnnotation(Email.class);
        if (emailAnnotation != null)
            node.put("format", "email");

        // Look for a @Size annotation, which should have a set of min/max properties.
        var minAndMaxLengthAnnotation = tryGetAnnotation(Size.class);
        var notNullAnnotation = tryGetAnnotation(NotNull.class);
        var notEmptyAnnotation = tryGetAnnotation(NotEmpty.class);
        if (minAndMaxLengthAnnotation != null) {
            if (minAndMaxLengthAnnotation.min() != 0)
                node.put("minLength", minAndMaxLengthAnnotation.min());
            if (minAndMaxLengthAnnotation.max() != Integer.MAX_VALUE)
                node.put("maxLength", minAndMaxLengthAnnotation.max());
        }
        else if (ctx.config.useMinLengthForNotNull && notNullAnnotation != null)
            node.put("minLength", 1);
        else if (notEmptyAnnotation != null || notBlankAnnotation != null)
            node.put("minLength", 1);

        return new MyJsonValueFormatVisitor();
    }

    @Override 
    public JsonArrayFormatVisitor expectArrayFormat(JavaType type) {
        log.trace("expectArrayFormat {}", type);

        node.put("type", "array");

        if (ctx.config.uniqueItemClasses.stream().anyMatch(c -> type.getRawClass().isAssignableFrom(c))) {
            // Adding '"uniqueItems": true' to be used with https://github.com/jdorn/json-editor
            node.put("uniqueItems", true);
            node.put("format", "checkbox");
        } else {
            // Try to set default format
            if (ctx.config.defaultArrayFormat != null)
                node.put("format", ctx.config.defaultArrayFormat);
        }

        var sizeAnnotation = tryGetAnnotation(Size.class);
        if (sizeAnnotation != null) {
            node.put("minItems", sizeAnnotation.min());
            node.put("maxItems", sizeAnnotation.max());
        }

        var notEmptyAnnotation = tryGetAnnotation(NotEmpty.class);
        if (notEmptyAnnotation != null)
            node.put("minItems", 1);

        var defaultValue = extractDefaultValue();
        if (defaultValue != null)
            node.put("default", defaultValue);


        var itemsNode = JsonNodeFactory.instance.objectNode();
        node.set("items", itemsNode);

        // We get improved result while processing scala-collections by getting elementType this way
        // instead of using the one which we receive in JsonArrayFormatVisitor.itemsFormat
        // This approach also works for Java
        JavaType preferredElementType = type.getContentType();

        class MyVisitor extends AbstractJsonFormatVisitorWithSerializerProvider implements JsonArrayFormatVisitor {
            @Override 
            public void itemsFormat(JsonFormatVisitable handler, JavaType elementType) throws JsonMappingException  {
                log.trace("expectArrayFormat - handler: $handler - elementType: {} - preferredElementType: {}", elementType, preferredElementType);
                var type = ctx.tryToReMapType(preferredElementType);
                var visitor = createChildVisitor(itemsNode, null);
                ctx.objectMapper.acceptJsonFormatVisitor(type, visitor);
            }

            @Override
            public void itemsFormat(JsonFormatTypes format)  {
                log.trace("itemsFormat - format: {}", format);
                itemsNode.put("type", format.value());
            }
        }
        
        return new MyVisitor();
    }

    @Override 
    public JsonNumberFormatVisitor expectNumberFormat(JavaType type) {
        log.trace("expectNumberFormat");

        node.put("type", "number");

        // Look for @Min, @Max, @DecimalMin, @DecimalMax => minimum, maximum
        var minAnnotation = tryGetAnnotation(Min.class);
        if (minAnnotation != null)
            node.put("minimum", minAnnotation.value());

        var maxAnnotation = tryGetAnnotation(Max.class);
        if (maxAnnotation != null)
            node.put("maximum", maxAnnotation.value());

        var decimalMinAnnotation = tryGetAnnotation(DecimalMin.class);
        if (decimalMinAnnotation != null)
            node.put("minimum", Double.valueOf(decimalMinAnnotation.value()));

        var decimalMaxAnnotation = tryGetAnnotation(DecimalMax.class);
        if (decimalMaxAnnotation != null)
            node.put("maximum", Double.valueOf(decimalMaxAnnotation.value()));

        var defaultValue = extractDefaultValue();
        if (defaultValue != null)
            node.put("default", Double.valueOf(defaultValue));

        if (currentProperty != null) {
            var examplesAnnotation = currentProperty.getAnnotation(JsonSchemaExamples.class);
            if (examplesAnnotation != null) {
                ArrayNode examples = JsonNodeFactory.instance.arrayNode();
                for (var example : examplesAnnotation.value()) {
                    examples.add(example);
                }
                node.set("examples", examples);
            }
        }

        return new MyJsonValueFormatVisitor();
    }

    @Override 
    public JsonAnyFormatVisitor expectAnyFormat(JavaType type) {
        log.warn("Unable to process {} - "
                + "it is probably using custom serializer which does not override acceptJsonFormatVisitor", type);

        return new JsonAnyFormatVisitor() {};
    }

    @Override 
    public JsonIntegerFormatVisitor expectIntegerFormat(JavaType type) {
        log.trace("expectIntegerFormat");

        node.put("type", "integer");

        var minAnnotation = tryGetAnnotation(Min.class);
        if (minAnnotation != null)
            node.put("minimum", minAnnotation.value());
        
        var maxAnnotation = tryGetAnnotation(Max.class);
        if (maxAnnotation != null)
            node.put("maximum", maxAnnotation.value());

        var defaultValue = extractDefaultValue();
        if (defaultValue != null)
            node.put("default", Integer.valueOf(defaultValue));

        if (currentProperty != null) {
            var examplesAnnotation = currentProperty.getAnnotation(JsonSchemaExamples.class);
            if (examplesAnnotation != null) {
                ArrayNode examples = JsonNodeFactory.instance.arrayNode();
                for (var example : examplesAnnotation.value()) {
                    examples.add(example);
                }
                node.set("examples", examples);
            }
        }

        return new MyJsonValueFormatVisitor();
    }

    @Override public JsonNullFormatVisitor expectNullFormat(JavaType type) {
        log.trace("expectNullFormat {}", type);
        node.put("type", "null");
        return new JsonNullFormatVisitor.Base();
    }

    @Override public JsonBooleanFormatVisitor expectBooleanFormat(JavaType type) {
        log.trace("expectBooleanFormat");

        node.put("type", "boolean");

        var defaultValue = extractDefaultValue();
        if (defaultValue != null)
            node.put("default", Boolean.valueOf(defaultValue));

        return new MyJsonValueFormatVisitor();
    }

    @Override public JsonMapFormatVisitor expectMapFormat(JavaType type) throws JsonMappingException {
        log.trace ("expectMapFormat {}", type);

        // There is no way to specify map in jsonSchema,
        // So we're going to treat it as type=object with additionalProperties = true,
        // so that it can hold whatever the map can hold

        node.put("type", "object");

        // If we're annotated with @NotEmpty, make sure we add a minItems of 1 to our schema here.
        var notEmptyAnnotation = tryGetAnnotation(NotEmpty.class);
        if (notEmptyAnnotation != null)
            node.put("minProperties", 1);

        var defaultValue = extractDefaultValue();
        if (defaultValue != null)
            node.put("default", defaultValue);

        var additionalPropsObject = JsonNodeFactory.instance.objectNode();
        definitionsHandler.pushWorkInProgress();
        var childVisitor = createChildVisitor(additionalPropsObject, null);
        ctx.objectMapper.acceptJsonFormatVisitor(ctx.tryToReMapType(type.getContentType()), childVisitor);
        definitionsHandler.popworkInProgress();
        node.set("additionalProperties", additionalPropsObject);

        
        class MapVisitor extends AbstractJsonFormatVisitorWithSerializerProvider implements JsonMapFormatVisitor {
        
            @Override public void keyFormat(JsonFormatVisitable handler, JavaType keyType)  {
                log.trace("JsonMapFormatVisitor.keyFormat handler: $handler - keyType: $keyType");
            }

            @Override public void valueFormat(JsonFormatVisitable handler, JavaType valueType)  {
                log.trace("JsonMapFormatVisitor.valueFormat handler: $handler - valueType: $valueType");
            }
        }
        
        return new MapVisitor();
    }

    @Data @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) @Accessors(fluent = true)
    static class PolymorphismInfo { String typePropertyName; String subTypeName; }

    private PolymorphismInfo extractPolymorphismInfo(JavaType type) throws JsonMappingException {
        
        var baseType = Utils.getSuperClass(type);
        if (baseType == null)
            return null;
        
        var serializer = ctx.getTypeSerializer(baseType);
        if (serializer == null)
            return null;
        
        var inclusionMethod = serializer.getTypeInclusion();
        if (inclusionMethod == JsonTypeInfo.As.PROPERTY
                || inclusionMethod == JsonTypeInfo.As.EXISTING_PROPERTY) {
            var idResolver = serializer.getTypeIdResolver();
            assert idResolver != null;
            String id;
            if (idResolver instanceof MinimalClassNameIdResolver)
                // use custom implementation, because default implementation needs instance and we don't have one
                id = Utils.extractMinimalClassnameId(baseType, type);
            else
                id = idResolver.idFromValueAndType(null, type.getRawClass());
            return new PolymorphismInfo(serializer.getPropertyName(), id);
        }
        else
            throw new IllegalStateException("We do not support polymorphism using jsonTypeInfo.include() = " + inclusionMethod);
    }

    private List<Class<?>> extractSubTypes(JavaType type) {
        return extractSubTypes(type.getRawClass());
    }

    private List<Class<?>> extractSubTypes(Class<?> type) {
        var ac = AnnotatedClassResolver.resolveWithoutSuperTypes(
                ctx.objectMapper.getDeserializationConfig(), 
                type, 
                ctx.objectMapper.getDeserializationConfig());

        var jsonTypeInfo = ac.getAnnotation(JsonTypeInfo.class);
        if (jsonTypeInfo == null) {
            return List.of();
        }

        if (jsonTypeInfo.use() == JsonTypeInfo.Id.NAME) {

            var subTypeAnn = type.getDeclaredAnnotation(JsonSubTypes.class);

            if (subTypeAnn == null) {
                // We did not find it via @JsonSubTypes-annotation (Probably since it is using mixin's) => Must fallback to using collectAndResolveSubtypesByClass
                var resolvedSubTypes 
                    = ctx.objectMapper.getSubtypeResolver()
                        .collectAndResolveSubtypesByClass(ctx.objectMapper.getDeserializationConfig(), ac);
                
                return resolvedSubTypes.stream()
                        .map(e -> e.getType())
                        .filter(c -> type.isAssignableFrom(c) && type != c)
//                        .toList(); // javac bug, lol (#9072339)
                        .collect(Collectors.toList());
            }

            
            var subTypes = subTypeAnn.value();
            return Stream.of(subTypes)
                    .map(subType -> subType.value())
                    .flatMap(subType -> {
                        var subSubTypes = extractSubTypes(subType);
                        if (!subSubTypes.isEmpty())
                            return subSubTypes.stream();
                        else
                            return Stream.of(subType);
                    })
                    .collect(Collectors.toList());
        }
        else
            return ctx.config.subclassesResolver.getSubclasses(type);
    }

    @Override 
    public JsonObjectFormatVisitor expectObjectFormat(JavaType type) throws JsonMappingException {

        var defaultValue = extractDefaultValue();
        if (defaultValue != null)
            node.put("default", defaultValue);

        List<Class<?>> subTypes = extractSubTypes(type);

        // Check if we have subtypes
        if (!subTypes.isEmpty()) {
            // We have subtypes
            //log.trace("polymorphism - subTypes: $subTypes")

            var anyOfArrayNode = JsonNodeFactory.instance.arrayNode();
            node.set("oneOf", anyOfArrayNode);

            for (var subType : subTypes) {
                log.trace("polymorphism - subType: $subType");
                var definitionInfo = definitionsHandler.getOrCreateDefinition
                    (ctx.objectMapper.constructType(subType), 
                    (t, objectNode) -> {
                        var childVisitor = createChildVisitor(objectNode, null);
                        ctx.objectMapper.acceptJsonFormatVisitor(ctx.tryToReMapType(subType), childVisitor);
                        return null;
                    });

                var thisOneOfNode = JsonNodeFactory.instance.objectNode();
                thisOneOfNode.put("$ref", definitionInfo.ref());
             
                // If class is annotated with JsonSchemaTitle, we should add it
                var titleAnnotation = subType.getDeclaredAnnotation(JsonSchemaTitle.class);
                if (titleAnnotation != null)
                    thisOneOfNode.put("title", titleAnnotation.value());

                anyOfArrayNode.add(thisOneOfNode);
            }

            return null; // Returning null to stop jackson from visiting this object since we have done it manually
        }
        else {
            // We do not have subtypes

            if (level == 0) {
                // This is the first level - we must not use definitions
                return objectBuilder(type, node);
            } 
            else {
                DefinitionInfo definitionInfo = definitionsHandler.getOrCreateDefinition(type, this::objectBuilder);

                if (definitionInfo.ref() != null)
                    node.put("$ref", definitionInfo.ref());

                return definitionInfo.jsonObjectFormatVisitor();
            }
        }
    }
    
    
    private JsonObjectFormatVisitor objectBuilder(JavaType type, ObjectNode thisObjectNode) throws JsonMappingException {

        thisObjectNode.put("type", "object");
        thisObjectNode.put("additionalProperties", !ctx.config.failOnUnknownProperties);

        var ac = AnnotatedClassResolver.resolve(ctx.objectMapper.getDeserializationConfig(), type, ctx.objectMapper.getDeserializationConfig());
        
        // If class is annotated with JsonSchemaFormat, we should add it
        var format = ctx.resolvePropertyFormat(type);
        if (format != null)
            thisObjectNode.put("format", format);
        
        // If class is annotated with JsonSchemaDescription, we should add it
        var descriptionAnnotation = ac.getAnnotations().get(JsonSchemaDescription.class);
        if (descriptionAnnotation != null)
            thisObjectNode.put("description", descriptionAnnotation.value());
        else {
            var descriptionAnnotation2 = ac.getAnnotations().get(JsonPropertyDescription.class);
            if (descriptionAnnotation2 != null)
                thisObjectNode.put("description", descriptionAnnotation2.value());
        }

//        // Scala (just as long. wtf?)
//        Option(ac.getAnnotations.get(classOf[JsonSchemaDescription])).map(_.value())
//          .orElse(Option(ac.getAnnotations.get(classOf[JsonPropertyDescription])).map(_.value))
//          .foreach {
//            description: String =>
//              thisObjectNode.put("description", description)
//          }

//        // Hypothetical syntax
//        var description = (ac.getAnnotations().get(JsonSchemaDescription.class) ?| _.value())
//                       ?? (ac.getAnnotations().get(JsonPropertyDescription.class) ?| _.value());
//        description ?| thisObjectNode.put("description", _);
        
//        // Alt syntax
//        var description = ac.getAnnotations().get(JsonSchemaDescription.class)?.value()
//        description ??= ac.getAnnotations().get(JsonPropertyDescription.class)?.value();
//        if (description) thisObjectNode.put("description", description);

        // If class is annotated with JsonSchemaTitle, we should add it
        var titleAnnotation = ac.getAnnotations().get(JsonSchemaTitle.class);
        if (titleAnnotation != null)
            thisObjectNode.put("title", titleAnnotation.value());
        
//        // alt syntax
//        ac.getAnnotations().get(JsonSchemaTitle.class) ?| thisObjectNode.put("title", _.value());

        // If class is annotated with JsonSchemaOptions, we should add it
        var optionsAnnotation = ac.getAnnotations().get(JsonSchemaOptions.class);
        if (optionsAnnotation != null) {
            var optionsNode = Utils.getOptionsNode(thisObjectNode);
            for (var item : optionsAnnotation.items())
                optionsNode.put(item.name(), item.value());
        }

        
        // Add JsonSchemaInject to top-level, if exists.
        // Possibly, it overrides further processing.
        boolean injectOverridesAll;
        var injectAnnotation = ctx.selectAnnotation(ac, JsonSchemaInject.class);
        if (injectAnnotation != null) {
            // Continue to render props if we merged injection
            injectOverridesAll = ctx.injectFromAnnotation(thisObjectNode, injectAnnotation);
        }
        else
            injectOverridesAll = false;
        if (injectOverridesAll)
            return null;
        

        var propertiesNode = Utils.getOrCreateObjectChild(thisObjectNode, "properties");

        var polyInfo = extractPolymorphismInfo(type);
        if (polyInfo != null) {
            thisObjectNode.put("title", polyInfo.subTypeName);
            
            // must inject the 'type'-param and value as enum with only one possible value
            // This is done to make sure the json generated from the schema using this oneOf
            // contains the correct "type info"
            var enumValuesNode = JsonNodeFactory.instance.arrayNode();
            enumValuesNode.add(polyInfo.subTypeName);

            var enumObjectNode = Utils.getOrCreateObjectChild(propertiesNode, polyInfo.typePropertyName);
            enumObjectNode.put("type", "string");
            enumObjectNode.set("enum", enumValuesNode);
            enumObjectNode.put("default", polyInfo.subTypeName);

            if (ctx.config.hidePolymorphismTypeProperty) {
                // Make sure the editor hides this polymorphism-specific property
                var optionsNode = JsonNodeFactory.instance.objectNode();
                enumObjectNode.set("options", optionsNode);
                optionsNode.put("hidden", true);
            }

            Utils.getRequiredArrayNode(thisObjectNode).add(polyInfo.typePropertyName);

            if (ctx.config.useMultipleEditorSelectViaProperty) {
                // https://github.com/jdorn/json-editor/issues/709
                // Generate info to help generated editor to select correct oneOf-type
                // when populating the gui/schema with existing data
                var objectOptionsNode = Utils.getOrCreateObjectChild( thisObjectNode, "options");
                var multipleEditorSelectViaPropertyNode = Utils.getOrCreateObjectChild( objectOptionsNode, "multiple_editor_select_via_property");
                multipleEditorSelectViaPropertyNode.put("property", polyInfo.typePropertyName);
                multipleEditorSelectViaPropertyNode.put("value", polyInfo.subTypeName);
            }

        }

        class MyObjectVisitor extends AbstractJsonFormatVisitorWithSerializerProvider implements JsonObjectFormatVisitor {

            @Override public void optionalProperty(BeanProperty prop) throws JsonMappingException  {
                log.trace("JsonObjectFormatVisitor.optionalProperty: prop: {}", prop);
                handleProperty(prop.getName(), prop.getType(), prop, false);
            }

            @Override public void optionalProperty(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) throws JsonMappingException  {
                log.trace("JsonObjectFormatVisitor.optionalProperty: name:{} handler:{} propertyTypeHint:{}", name, handler, propertyTypeHint);
                handleProperty(name, propertyTypeHint, null, false);
            }

            @Override public void property(BeanProperty prop) throws JsonMappingException  {
                log.trace("JsonObjectFormatVisitor.property: prop:{}", prop);
                handleProperty(prop.getName(), prop.getType(), prop, true);
            }

            @Override public void property(String name, JsonFormatVisitable handler, JavaType propertyTypeHint) throws JsonMappingException {
                log.trace("JsonObjectFormatVisitor.property: name:{} handler:{} propertyTypeHint:{}", name, handler, propertyTypeHint);
                handleProperty(name, propertyTypeHint, null, true);
            }

            // Used when rendering schema using propertyOrdering as specified here:
            // https://github.com/jdorn/json-editor#property-ordering
            int nextPropertyOrderIndex = 1;

            void handleProperty(String propertyName, JavaType propertyType, BeanProperty prop, Boolean jsonPropertyRequired) throws JsonMappingException {
                log.trace("JsonObjectFormatVisitor - {}: {}", propertyName, propertyType);

                if (propertiesNode.get(propertyName) != null) {
                    log.debug("Ignoring property '{}' in $propertyType since it has already been added, probably as type-property using polymorphism", propertyName);
                    return;
                }

                // Need to check for Optional/Optional-special-case before we know what node to use here.
//                record PropertyNode(ObjectNode main, ObjectNode meta) {}
                @Data @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) @Accessors(fluent = true)
                class PropertyNode { final ObjectNode main; final ObjectNode meta; }

                // Check if we should set this property as required. Primitive types MUST have a value, as does anything
                // with a @JsonProperty that has "required" set to true. Lastly, various javax.validation annotations also
                // make this required.
                boolean requiredProperty = propertyType.getRawClass().isPrimitive() || jsonPropertyRequired || ctx.validationAnnotationRequired(prop);

                boolean optionalType = Optional.class.isAssignableFrom(propertyType.getRawClass())
                        || propertyType.getRawClass().getName().equals("scala.Option");

                PropertyNode thisPropertyNode;
                {
                    var node = JsonNodeFactory.instance.objectNode();
                    propertiesNode.set(propertyName, node);

                    if (ctx.config.usePropertyOrdering) {
                        node.put("propertyOrder", nextPropertyOrderIndex);
                        nextPropertyOrderIndex += 1;
                    }

                    if (!requiredProperty && ((ctx.config.useOneOfForOption && optionalType) ||
                      (ctx.config.useOneOfForNullables && !optionalType))) {
                        // We support this type being null, insert a oneOf consisting of a sentinel "null" and the real type.
                        var oneOfArray = JsonNodeFactory.instance.arrayNode();
                        node.set("oneOf", oneOfArray);

                        // Create our sentinel "null" value for the case no value is provided.
                        var oneOfNull = JsonNodeFactory.instance.objectNode();
                        oneOfNull.put("type", "null");
                        oneOfNull.put("title", "Not included");
                        oneOfArray.add(oneOfNull);

                        // If our nullable/optional type has a value, it'll be this.
                        var oneOfReal = JsonNodeFactory.instance.objectNode();
                        oneOfArray.add(oneOfReal);

                        // Return oneOfReal which, from now on, will be used as the node representing this property
                        thisPropertyNode = new PropertyNode(oneOfReal, node);
                    } else {
                        // Our type must not be null: primitives, @NotNull annotations, @JsonProperty annotations marked required etc.
                        thisPropertyNode = new PropertyNode(node, node);
                    }
                }

                // Continue processing this property
                var childVisitor = createChildVisitor(thisPropertyNode.main, prop);

                // Push current work in progress since we're about to start working on a new class
                definitionsHandler.pushWorkInProgress();

                if ((Optional.class.isAssignableFrom(propertyType.getRawClass()) 
                            || propertyType.getRawClass().getName().equals("scala.Option"))
                        && propertyType.containedTypeCount() >= 1) {

                    // Property is scala Optional or Java Optional.
                    //
                    // Due to Java's Type Erasure, the type behind Optional is lost.
                    // To workaround this, we use the same workaround as jackson-scala-module described here:
                    // https://github.com/FasterXML/jackson-module-scala/wiki/FAQ#deserializing-optionint-and-other-primitive-challenges

                    JavaType optionType = Utils.resolveElementType(propertyType, prop, ctx.objectMapper);

                    ctx.objectMapper.acceptJsonFormatVisitor(ctx.tryToReMapType(optionType), childVisitor);
                } else {
                    ctx.objectMapper.acceptJsonFormatVisitor(ctx.tryToReMapType(propertyType), childVisitor);
                }

                // Pop back the work we were working on..
                definitionsHandler.popworkInProgress();

                // If this property is required, add it to our array of required properties.
                if (requiredProperty)
                    Utils.getRequiredArrayNode(thisObjectNode).add(propertyName);
                
                if (prop == null)
                    return;

                var format = ctx.resolvePropertyFormat(prop);
                if (format != null)
                    thisPropertyNode.main.put("format", format);

                // Optionally add description
                var descriptionAnn = prop.getAnnotation(JsonSchemaDescription.class);
                var descriptionAnn2 = prop.getAnnotation(JsonPropertyDescription.class);
                if (descriptionAnn != null)
                    thisPropertyNode.meta.put("description", descriptionAnn.value());
                else if (descriptionAnn2 != null)
                    thisPropertyNode.meta.put("description", descriptionAnn2.value());

                // Optionally add title
                var titleAnn = prop.getAnnotation(JsonSchemaTitle.class);
                if (titleAnn != null)
                    thisPropertyNode.meta.put("title", titleAnn.value());
                else if (ctx.config.autoGenerateTitleForProperties) {
                    // We should generate 'pretty-name' based on propertyName
                    var title = Utils.camelCaseToSentenceCase(propertyName);
                    thisPropertyNode.meta.put("title", title);
                }
                
//                var title = prop.getAnnotation(JsonSchemaTitle.class) ?| _.value();
//                if (ctx.config.autoGenerateTitleForProperties)
//                    title ??= Utils.generateTitleFromPropertyName(propertyName);
//                title ?| thisPropertyNode.meta.put("title", _);

                // Optionally add options
                var optionsAnn = prop.getAnnotation(JsonSchemaOptions.class);
                if (optionsAnn != null) {
                    var optionsNode = Utils.getOrCreateObjectChild(thisPropertyNode.meta, "options");
                    for (var option : optionsAnn.items())
                        optionsNode.put(option.name(), option.value());
                }
                
                // unpack operator and foreach-pipe
//                prop.getAnnotation(JsonSchemaOptions.class) ?| *(_.items()) | option -> {
//                    var optionsNode = Utils.getOrCreateObjectChild(thisPropertyNode.meta, "options");
//                    optionsNode.put(option.name(), option.value());
//                };

                // just null-safe (or null short-circuiting) pipe
//                for (var option : (prop.getAnnotation(JsonSchemaOptions.class) ?| _.items()) ?? List.of()) {
//                    var optionsNode = Utils.getOrCreateObjectChild(thisPropertyNode.meta, "options");
//                    optionsNode.put(_.name(), _.value());
//                }

                // null-safe foreach (possibly using :? operator)
//                for (var option : prop.getAnnotation(JsonSchemaOptions.class) ?| _.items()) {
//                    var optionsNode = Utils.getOrCreateObjectChild(thisPropertyNode.meta, "options");
//                    optionsNode.put(_.name(), _.value());
//                }

                // Optionally add JsonSchemaInject
                var injectAnn = ctx.selectAnnotation(prop, JsonSchemaInject.class);
                if (injectAnn == null) {
                    // Try to look at the class itself -- Looks like this is the only way to find it if the type is Enum
                    var injectAnn2 = prop.getType().getRawClass().getAnnotation(JsonSchemaInject.class);
                    if (injectAnn2 != null && ctx.annotationIsApplicable(injectAnn2))
                        injectAnn = injectAnn2;
                }
                if (injectAnn != null)
                    ctx.injectFromAnnotation(thisPropertyNode.meta, injectAnn);
            }
        }
            
        return new MyObjectVisitor();
    }
}
