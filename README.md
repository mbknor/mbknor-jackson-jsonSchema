Jackson jsonSchema Generator
===================================
[![Build Status](https://travis-ci.org/mbknor/mbknor-jackson-jsonSchema.svg)](https://travis-ci.org/mbknor/mbknor-jackson-jsonSchema)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.kjetland/mbknor-jackson-jsonschema_2.12/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cmbknor-jackson-jsonSchema)

This projects aims to do a better job than the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema)
in generating jsonSchema from your POJOs using Jackson @Annotations.


**Highlights**

* JSON Schema Draft v4
* Supports polymorphism (**@JsonTypeInfo**, **MixIn**, and **registerSubtypes()**) using JsonSchema's **oneOf**-feature.
* Supports schema customization using:
  - **@JsonSchemaDescription**/**@JsonPropertyDescription**
  - **@JsonSchemaFormat**
  - **@JsonSchemaTitle**
  - **@JsonSchemaDefault**
  - **@JsonSchemaOptions**
  - **@JsonSchemaInject**
* Supports many Javax-validation @Annotations
* Works well with Generated GUI's using [https://github.com/jdorn/json-editor](https://github.com/jdorn/json-editor)
  - (Must be configured to use this mode)
  - Special handling of Option-/Optional-properties using oneOf.
* Supports custom Class-to-format-Mapping
* Supports injecting custom json-schema-fragments using the **@JsonSchemaInject**-annotation.


**Benefits**

* Simple implementation - Just [one file](https://github.com/mbknor/mbknor-jackson-jsonSchema/blob/master/src/main/scala/com/kjetland/jackson/jsonSchema/JsonSchemaGenerator.scala)  (for now..)
* Implemented in Scala (*Built for 2.10, 2.11 and 2.12*)
* Easy to fix and add functionality

Flexible
--------------

If this generator does not generate exactly the schema you want, you can inject it by using the
**@JsonSchemaInject**-annotation.

If you need to use *patternProperties* (which is not currently 'natively' supported by *mbknor-jackson-jsonSchema*),
you can make it work by injecting the following json-schema-fragment:

```Json
{
  "patternProperties" : {
    "^[a-zA-Z0-9]+" : {
      "type" : "string"
    }
  }
}
```


.. like this in Scala:
```Scala
@JsonSerialize(using = MySpecialSerializer.class)
JsonSchemaInject(
  json =
    """
      {
        "patternProperties" : {
          "^[a-zA-Z0-9]+" : {
            "type" : "string"
          }
        }
      }
    """
)
case class MyPojo(...)
```

.. or like this in Java
```Java

@JsonSerialize(using = MySpecialSerializer.class)
@JsonSchemaInject( json = "{\n" +
        "  \"patternProperties\" : {\n" +
        "    \"^[a-zA-Z0-9]+\" : {\n" +
        "      \"type\" : \"string\"\n" +
        "    }\n" +
        "  }\n" +
        "}" )
public class MyPojo {
    ...
    ...
    ...
}
```


@JsonSchemaInject can also be used on properties.


Project status
---------------
We're currently using this codebase in an ongoing (not yet released) project at work,
and we're improving the jsonSchema-generating code when we finds issues and/or features we need that not yet is supported.

I would really appreciate it if other developers wanted to start using and contributing improvements and features.

Dependency
===================

This project publishes artifacts to central maven repo.

The project is also compiled using Java 8. This means that you also need to use Java 8.

Artifacts for both Scala 2.10, 2.11 and 2.12 is now available (Thanks to [@bbyk](https://github.com/bbyk) for adding crossBuild functionality).

Using Maven
-----------------

Add this to you pom.xml:

    <dependency>
        <groupId>com.kjetland</groupId>
        <artifactId>mbknor-jackson-jsonschema_2.12</artifactId>
        <version>[---LATEST VERSION---]</version>
    </dependency>    

Using sbt
------------

Add this to you sbt build-config:

    "com.kjetland" % "mbknor-jackson-jsonschema" %% "[---LATEST VERSION---]"


Code - Using Scala
-------------------------------

This is how to generate jsonSchema in code using Scala:

```scala
    val objectMapper = new ObjectMapper
    val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper)
    val jsonSchema:JsonNode = jsonSchemaGenerator.generateJsonSchema(classOf[YourPOJO])

    val jsonSchemaAsString:String = objectMapper.writeValueAsString(jsonSchema)
```

This is how to generate jsonSchema used for generating HTML5 GUI using [json-editor](https://github.com/jdorn/json-editor):

```scala
    val objectMapper = new ObjectMapper
    val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config = JsonSchemaConfig.html5EnabledSchema)
    val jsonSchema:JsonNode = jsonSchemaGenerator.generateJsonSchema(classOf[YourPOJO])

    val jsonSchemaAsString:String = objectMapper.writeValueAsString(jsonSchema)
```

This is how to generate jsonSchema using custom type-to-format-mapping using Scala:

```scala
    val objectMapper = new ObjectMapper
    val config:JsonSchemaConfig = JsonSchemaConfig.vanillaJsonSchemaDraft4.copy(
      customType2FormatMapping = Map( "java.time.OffsetDateTime" -> "date-time-ABC-Special" )
    )
    val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config = config)
    val jsonSchema:JsonNode = jsonSchemaGenerator.generateJsonSchema(classOf[YourPOJO])

    val jsonSchemaAsString:String = objectMapper.writeValueAsString(jsonSchema)
```

**Note about Scala and Option[Int]**:

Due to Java's Type Erasure it impossible to resolve the type T behind Option[T] when T is Int, Boolean, Double.
As a workaround, you have to use the *@JsonDeserialize*-annotation in such cases.
See https://github.com/FasterXML/jackson-module-scala/wiki/FAQ#deserializing-optionint-and-other-primitive-challenges for more info.

Example:
```scala
    case class PojoUsingOptionScala(
                                     _string:Option[String], // @JsonDeserialize not needed here
                                     @JsonDeserialize(contentAs = classOf[Int])     _integer:Option[Int],
                                     @JsonDeserialize(contentAs = classOf[Boolean]) _boolean:Option[Boolean],
                                     @JsonDeserialize(contentAs = classOf[Double])  _double:Option[Double],
                                     child1:Option[SomeOtherPojo] // @JsonDeserialize not needed here
                                   )
```

PS: Scala Option combined with Polymorphism does not work in jackson-scala-module and therefor not this project either.

Code - Using Java
-------------------------

```java
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

    // If using JsonSchema to generate HTML5 GUI:
    // JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema() );

    // If you want to confioure it manually:
    // JsonSchemaConfig config = JsonSchemaConfig.create(...);
    // JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);


    JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(YourPOJO.class);

    String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
```

**Nullable types**

Out of the box, the generator does not support nullable types. There is a preconfigured `JsonSchemaGenerator` configuration shortcut that can be used to enable them:

```java
JsonSchemaConfig config = JsonSchemaConfig.nullableJsonSchemaDraft4();
JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);
```

Under the hood `nullableJsonSchemaDraft4` toggles the `useOneOfForOption` and `useOneOfForNullables` properties on `JsonSchemaConfig`.

When support is enabled, the following types may be made nullable:
 - Use `Optional<T>` (or Scala's `Option`)
 - Use a non-optional, non-primitive type (IE: `String`, `Boolean`, `Integer` etc) 

If you've otherwise enabled support for nullable types, but need to suppress this at a per-property level, you can do this like so:

```java
// A standard validation @NotNull annotation.
@NotNull
public String foo;

// Using the Jackson @JsonProperty annotation, specifying the attribute as required.
@JsonProperty(required = true)
public String bar;
```

Backstory
--------------


At work we've been using the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema)
to generate schemas used when rendering dynamic GUI using [https://github.com/jdorn/json-editor](https://github.com/jdorn/json-editor).

Recently we needed to support POJO's using polymorphism like this:

```java
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Child1.class, name = "child1"),
            @JsonSubTypes.Type(value = Child2.class, name = "child2") })
    public abstract class Parent {

        public String parentString;

    }
```

This is not supported by the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema).
I have spent many hours trying to figure out how to modify/improve it without any luck,
and since it is implemented in such a complicated way, I decided to instead write my own
jsonSchema generator from scratch.
