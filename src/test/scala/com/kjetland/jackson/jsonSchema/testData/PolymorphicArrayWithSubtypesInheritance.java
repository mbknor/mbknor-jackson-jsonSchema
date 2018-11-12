package com.kjetland.jackson.jsonSchema.testData;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.Lists;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaArrayFilter;

import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class PolymorphicArrayWithSubtypesInheritance {

  @NotNull
  @Size(min = 2, max = 2)
  @JsonProperty(required = true)
  @JsonSchemaArrayFilter({Foo2Type.class, ASubType.class})
  private List<ABase2Type> fooOrSubList;


  public PolymorphicArrayWithSubtypesInheritance() {
    this(Lists.newArrayList());
  }

  public PolymorphicArrayWithSubtypesInheritance(List<ABase2Type> fooOrSubList) {
    this.fooOrSubList = fooOrSubList;
  }


  @Override
  public int hashCode() {
    return Objects.hash(fooOrSubList);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PolymorphicArrayWithSubtypesInheritance)) {
      return false;
    }
    return Objects
        .equals(fooOrSubList, ((PolymorphicArrayWithSubtypesInheritance) obj).fooOrSubList);
  }


  @JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(name = "foo", value = Foo2Type.class),
      @JsonSubTypes.Type(name = "bar", value = Bar2Type.class),
      @JsonSubTypes.Type(name = "spam", value = SpamType.class),
      @JsonSubTypes.Type(name = "ham", value = HamType.class),
  })
  public static abstract class ABase2Type {

    @JsonProperty
    public abstract String getType();

  }

  public static class Foo2Type extends ABase2Type {

    @NotNull
    @JsonProperty
    private String fooField;

    @JsonCreator
    public Foo2Type(@JsonProperty("fooField") String fooField) {
      this.fooField = fooField;
    }

    @Override
    public String getType() {
      return "foo";
    }

    @Override
    public int hashCode() {
      return Objects.hash(getType(), fooField);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Foo2Type)) {
        return false;
      }
      return Objects.equals(fooField, ((Foo2Type) obj).fooField);
    }

  }

  public static class Bar2Type extends ABase2Type {

    @NotNull
    @JsonProperty
    private String barField;

    @JsonCreator
    public Bar2Type(@JsonProperty("barField") String barField) {
      this.barField = barField;
    }

    @Override
    public String getType() {
      return "bar";
    }

    @Override
    public int hashCode() {
      return Objects.hash(getType(), barField);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Bar2Type)) {
        return false;
      }
      return Objects.equals(barField, ((Bar2Type) obj).barField);
    }

  }


  public static abstract class ASubType extends ABase2Type {

  }

  public static class SpamType extends ASubType {

    @NotNull
    @JsonProperty
    private String spamField;

    @JsonCreator
    public SpamType(@JsonProperty("spamField") String spamField) {
      this.spamField = spamField;
    }

    @Override
    public String getType() {
      return "spam";
    }

    @Override
    public int hashCode() {
      return Objects.hash(getType(), spamField);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof SpamType)) {
        return false;
      }
      return Objects.equals(spamField, ((SpamType) obj).spamField);
    }

  }

  public static class HamType extends ASubType {

    @NotNull
    @JsonProperty
    private String hamField;

    @JsonCreator
    public HamType(@JsonProperty("hamField") String hamField) {
      this.hamField = hamField;
    }

    @Override
    public String getType() {
      return "ham";
    }

    @Override
    public int hashCode() {
      return Objects.hash(getType(), hamField);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof HamType)) {
        return false;
      }
      return Objects.equals(hamField, ((HamType) obj).hamField);
    }

  }

}
