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

public class PolymorphicArrayWithSubtypes {

  @NotNull
  @Size(min = 2, max = 2)
  @JsonProperty(required = true)
  @JsonSchemaArrayFilter({FooType.class, BarType.class})
  private List<ABaseType> fooOrBarList;


  public PolymorphicArrayWithSubtypes() {
    this(Lists.newArrayList());
  }

  public PolymorphicArrayWithSubtypes(List<ABaseType> fooOrBarList) {
    this.fooOrBarList = fooOrBarList;
  }


  @Override
  public int hashCode() {
    return Objects.hash(fooOrBarList);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PolymorphicArrayWithSubtypes)) {
      return false;
    }
    return Objects.equals(fooOrBarList, ((PolymorphicArrayWithSubtypes) obj).fooOrBarList);
  }


  @JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(name = "foo", value = FooType.class),
      @JsonSubTypes.Type(name = "bar", value = BarType.class),
      @JsonSubTypes.Type(name = "egg", value = EggType.class),
  })
  public static abstract class ABaseType {

    @JsonProperty
    public abstract String getType();

  }

  public static class FooType extends ABaseType {

    @NotNull
    @JsonProperty
    private String fooField;

    @JsonCreator
    public FooType(@JsonProperty("fooField") String fooField) {
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
      if (!(obj instanceof FooType)) {
        return false;
      }
      return Objects.equals(fooField, ((FooType) obj).fooField);
    }

  }

  public static class BarType extends ABaseType {

    @NotNull
    @JsonProperty
    private String barField;

    @JsonCreator
    public BarType(@JsonProperty("barField") String barField) {
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
      if (!(obj instanceof BarType)) {
        return false;
      }
      return Objects.equals(barField, ((BarType) obj).barField);
    }

  }


  public static class EggType extends ABaseType {

    @NotNull
    @JsonProperty
    private String eggField;

    @JsonCreator
    public EggType(@JsonProperty("eggField") String eggField) {
      this.eggField = eggField;
    }

    @Override
    public String getType() {
      return "egg";
    }

    @Override
    public int hashCode() {
      return Objects.hash(getType(), eggField);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof EggType)) {
        return false;
      }
      return Objects.equals(eggField, ((EggType) obj).eggField);
    }

  }

}
