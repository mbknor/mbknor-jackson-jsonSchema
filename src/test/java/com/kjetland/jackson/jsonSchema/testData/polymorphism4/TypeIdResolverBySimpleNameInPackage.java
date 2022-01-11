package com.kjetland.jackson.jsonSchema.testData.polymorphism4;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.io.IOException;

/**
 * This type resolver has the contract that should be followed.
 * Each message should have the interface, and it should be in the same package with base class
 */
public class TypeIdResolverBySimpleNameInPackage extends TypeIdResolverBase {

    private JavaType baseType;

    @Override
    public void init(JavaType baseType) {
        this.baseType = baseType;
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return suggestedType.getSimpleName();
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        try {
            Class<?> rawClass = baseType.getRawClass();
            String parentPackage = rawClass.getPackage().getName();
            Class<?> subclass = Class.forName(parentPackage + "." + id);
            return context.constructSpecializedType(baseType, subclass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
