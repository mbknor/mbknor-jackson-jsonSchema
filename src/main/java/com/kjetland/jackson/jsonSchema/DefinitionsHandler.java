package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

// Class that manages creating new definitions or getting $refs to existing definitions
@RequiredArgsConstructor
class DefinitionsHandler {

    // can be records
    @Data @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) @Accessors(fluent = true)
    static class DefinitionInfo { String ref; JsonObjectFormatVisitor jsonObjectFormatVisitor; }
    @Data @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) @Accessors(fluent = true)
    static class WorkInProgress { JavaType typeInProgress; ObjectNode nodeInProgress; }
    
    final JsonSchemaConfig config;

    final private Map<JavaType, String> class2Ref = new HashMap<>();
    final private ObjectNode definitionsNode = JsonNodeFactory.instance.objectNode();

    // Used to combine multiple invocations of `getOrCreateDefinition` when processing polymorphism.
    private Deque<Optional<WorkInProgress>> workInProgressStack = new LinkedList<>();
    private Optional<WorkInProgress> workInProgress = Optional.empty();

    
    @FunctionalInterface
    interface VisitorSupplier {
        
        JsonObjectFormatVisitor get(JavaType type, ObjectNode t) throws JsonMappingException;
    }

    public void pushWorkInProgress() {
        workInProgressStack.push(workInProgress);
        workInProgress = Optional.empty();
    }

    public void popworkInProgress() {
        workInProgress = workInProgressStack.pop();
    }

    // Either creates new definitions or return $ref to existing one
    public DefinitionInfo getOrCreateDefinition(JavaType type, VisitorSupplier visitorSupplier) throws JsonMappingException {

        var ref = class2Ref.get(type);
        if (ref != null)
            // Return existing definition
            if (workInProgress.isEmpty())
                return new DefinitionInfo(ref, null);
            else {
                // this is a recursive polymorphism call
                if (type != workInProgress.get().typeInProgress)
                    throw new IllegalStateException("Wrong type - working on " + workInProgress.get().typeInProgress + " - got " + type);

                var visitor = visitorSupplier.get(type, workInProgress.get().nodeInProgress);
                return new DefinitionInfo(null, visitor);
            }
            
        // Build new definition
        var retryCount = 0;
        var definitionName = getDefinitionName(type);
        var shortRef = definitionName;
        var longRef = "#/definitions/" + definitionName;
        while (class2Ref.containsValue(longRef)) {
            retryCount = retryCount + 1;
            shortRef = definitionName + "_" + retryCount;
            longRef = "#/definitions/" + definitionName + "_" + retryCount;
        }
        class2Ref.put(type, longRef);

        var node = JsonNodeFactory.instance.objectNode();

        // When processing polymorphism, we might get multiple recursive calls to getOrCreateDefinition - this is a way to combine them
        workInProgress = Optional.of(new WorkInProgress(type, node));
        definitionsNode.set(shortRef, node);

        var visitor = visitorSupplier.get(type, node);

        workInProgress = Optional.empty();

        return new DefinitionInfo(longRef, visitor);
    }

    public ObjectNode getFinalDefinitionsNode() {
        if (class2Ref.isEmpty())
            return null;
        else
            return definitionsNode;
    }

    private String getDefinitionName(JavaType type) {
        var baseName = config.useTypeIdForDefinitionName
                ? type.getRawClass().getTypeName()
                : Utils.extractTypeName(type);

        if (type.hasGenericTypes()) {
            var containedTypeNames
                    = IntStream.range(0, type.containedTypeCount())
                            .mapToObj(type::containedType)
                            .map(this::getDefinitionName)
                            .collect(Collectors.joining(","));
            return baseName + "(" + containedTypeNames + ")";
        } else {
            return baseName;
        }
    }
}
