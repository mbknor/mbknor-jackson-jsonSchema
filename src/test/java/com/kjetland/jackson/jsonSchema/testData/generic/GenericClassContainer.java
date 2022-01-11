package com.kjetland.jackson.jsonSchema.testData.generic;

import java.util.Objects;

public class GenericClassContainer {
    public GenericClass<String> child1 = new GenericClass<>("asdf");
    public GenericClass<BoringClass> child2 = new GenericClass<>(new BoringClass(1337));
    public GenericClassTwo<String, GenericClass<BoringClass>> child3 =
            new GenericClassTwo<>("qqq", new GenericClass<>(new BoringClass(1337)));

    public GenericClassWithJsonTypeName<String> child4 = new GenericClassWithJsonTypeName<>("testString");

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericClassContainer that = (GenericClassContainer) o;
        return Objects.equals(child1, that.child1) &&
                Objects.equals(child2, that.child2) &&
                Objects.equals(child3, that.child3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(child1, child2, child3);
    }
}
