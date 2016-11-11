package com.kjetland.jackson.jsonSchema.testData.mixin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.Module.SetupContext; // This import is needed for it to compile using Scala 2.12.0 - If not we get an obscure compiler error

public class MixinModule extends SimpleModule {


    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MixinChild1.class, name = "child1"),
            @JsonSubTypes.Type(value = MixinChild2.class, name = "child2") })
    abstract class Mixin {

    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(MixinParent.class, Mixin.class);
    }

}
