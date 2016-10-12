package com.kjetland.jackson.jsonSchema.testData.mixin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.module.SimpleModule;


public class MixinModule extends SimpleModule {


    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MixinChild1.class, name = "MixinChild1"),
            @JsonSubTypes.Type(value = MixinChild2.class, name = "MixinChild2") })
    abstract class Mixin {

    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(MixinParent.class, Mixin.class);
    }

}
