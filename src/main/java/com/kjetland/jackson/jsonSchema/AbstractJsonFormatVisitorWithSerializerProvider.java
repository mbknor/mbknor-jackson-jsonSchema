package com.kjetland.jackson.jsonSchema;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWithSerializerProvider;

abstract class AbstractJsonFormatVisitorWithSerializerProvider implements JsonFormatVisitorWithSerializerProvider {

    SerializerProvider provider;

    @Override
    public SerializerProvider getProvider() {
        return provider;
    }

    @Override
    public void setProvider(SerializerProvider provider) {
        this.provider = provider;
    }
}
