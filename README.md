Jackson jsonSchema Generator
===================================
[![Build Status](https://travis-ci.org/mbknor/mbknor-jackson-jsonSchema.svg)](https://travis-ci.org/mbknor/mbknor-jackson-jsonSchema)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.kjetland/mbknor-jackson-jsonschema_2.12/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cmbknor-jackson-jsonSchema)

This projects aims to do a better job than the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema)
in generating jsonSchema from your POJOs using Jackson @Annotations.


**Highlights**

* JSON Schema DRAFT-04, DRAFT-06, DRAFT-07 and DRAFT-2019-09
* Supports polymorphism (**@JsonTypeInfo**, **MixIn**, and **registerSubtypes()**) using JsonSchema's **oneOf**-feature.
* Supports schema customization using:
  - **@JsonSchemaDescription**/**@JsonPropertyDescription**
  - **@JsonSchemaFormat**
  - **@JsonSchemaTitle**
  - **@JsonSchemaDefault**
  - **@JsonSchemaOptions**
  - **@JsonSchemaInject**
  - **@JsonSchemaExamples**
* Supports many Javax-validation @Annotations including support for validation-groups
* Works well with Generated GUI's using [https://github.com/json-editor/json-editor](https://github.com/json-editor/json-editor)
  - (Must be configured to use this mode)
  - Special handling of Option-/Optional-properties using oneOf.
* Supports custom Class-to-format-Mapping
* Supports injecting custom json-schema-fragments using the **@JsonSchemaInject**-annotation.


**Benefits**

* Simple implementation - Just [one file](https://github.com/mbknor/mbknor-jackson-jsonSchema/blob/master/src/main/scala/com/kjetland/jackson/jsonSchema/JsonSchemaGenerator.scala)  (for now..)
* Implemented in Scala (*Built for 2.10, 2.11, 2.12 and 2.13*)
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
Alternatively, the following one liner will achieve the same result:
```Java
@JsonSerialize(using = MySpecialSerializer.class)
@JsonSchemaInject(strings = {@JsonSchemaString(path = "patternProperties/^[a-zA-Z0-9]+/type", value = "string")})
public class MyPojo {
    ...
    ...
    ...
}
```
The annotation will nest the `value` at the level defined by the `path`. You can use the raw json along with the individual path/value pairs in the same `@JsonSchemaInject` annotation. Although keep in mind that the pairs are applied last. For boolean and number values use the `JsonSchemaInject#bools` and `JsonSchemaInject#ints` collections correspondingly.
```Java
public class MyPojo {
    @JsonSchemaInject(
      bools = {@JsonSchemaBool(path = "exclusiveMinimum", value = true)},
      ints = {@JsonSchemaInt(path = "multipleOf", value = 7)}
    )
    @Min(5)
    public int myIntValue;
    ...
    ...
    ...
}
```
If a part of the schema is not known at compile time, you can use a json supplier:
```Scala
case class MyPojo {
  @JsonSchemaInject(jsonSupplier = classOf[UserNamesLoader])
  uns:Set[String]
  ...
  ...
  ...
}

class UserNamesLoader extends Supplier[JsonNode] {
  val _objectMapper = new ObjectMapper()

  override def get(): JsonNode = {
    val schema = _objectMapper.createObjectNode()
    val values = schema.putObject("items").putArray("enum")
    loadUsers().foreach(u => values.add(u.name))

    schema
  }
  ...
  ...
  ...
}
```
This will associate an enum of possible values for the set that you generate at runtime.

If you need even more control over the schema-generating runtime, you can use **@JsonSchemaInject.jsonSupplierViaLookup**
like this:

```Scala
case class MyPojo {
  @JsonSchemaInject(jsonSupplierViaLookup = "theKeyToUseWhenLookingUpASupplier")
  uns:Set[String]
  ...
  ...
  ...
}
```

Then you have to add the mapping between the key 'theKeyToUseWhenLookingUpASupplier' and the Supplier-instance in the config-object
used when creating the JsonSchemaGenerator.


The default behaviour of @JsonSchemaInject is to **merge** the injected json into the generated JsonSchema.
If you want to have full control over it, you can specify @JsonSchemaInject.merge = false to **replace** the generated
jsonSchema with the injected json.


@JsonSchemaInject can also be used on properties.


Another way of altering the generated schema is to use the config-param **classTypeReMapping**.

This can be used to remap the Class found by Jackson into another Class before generating the schema for it.

It might be the case that you have a complex Class-structure using Polymorphism, but for some reason
you know upfront that the user needs to enter/specify a specific subType.
To enforce this into the generated schema, you can map the SuperType into the specific-type. 


Project status
---------------
Applications using this project has been in production for many years.

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

    "com.kjetland" %% "mbknor-jackson-jsonschema" % "[---LATEST VERSION---]"


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

PS: Scala Option combined with Polymorphism does not work in jackson-scala-module and therefore not this project either.

Code - Using Java
-------------------------

```java
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

    // If using JsonSchema to generate HTML5 GUI:
    // JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema() );

    // If you want to configure it manually:
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
**Using JSON Views**

Using JSON Views is most helpful for an API for various clients that will receive different output fields out of the same class, by calling different service endpoints.
While support for these is not built in jsonSchema, it is handy to know how to use them with it since it is not an obvious process unless you are very familiar with the Jackson API. 

Hence, let's suppose that you want to filter YourPojo using properties marked with the view Views.MyView.

Here is how to do it in Scala: 

```scala
    val objectMapper = new ObjectMapper

    objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
    objectMapper.setConfig(objectMapper.getSerializationConfig().withView(Views.MyView.class))

    val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper)
    val jsonSchema:JsonNode = jsonSchemaGenerator.generateJsonSchema(classOf[YourPOJO])

    val jsonSchemaAsString:String = objectMapper.writeValueAsString(jsonSchema)
```

And here is the equivalent for Java: 

```java
    ObjectMapper objectMapper = new ObjectMapper();
		
    // Disabling default View so only the properties that matter are output
    objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
	    
    // And the trick: grab the serializationConfig and define the desired view
    objectMapper.setConfig(objectMapper.getSerializationConfig().withView(Views.MyView.class));
	    
    // Then, proceed as usual. Only fields and classes annotated with MyView will appear in the schema
    JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper);
    JsonNode jsonSchema = generator.generateJsonSchema(SearchResult.class);
    String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
```

Subclass-resolving using reflection
-------

In some cases it is needed to find extra info about classes not found in jackson data.
[https://github.com/classgraph/classgraph](https://github.com/classgraph/classgraph) is used to solve this problem.

By default we scan the entire classpath. This can be slow, so it is better to customize what to scan.

This is how you can configure what *mbknor-jackson-jsonSchema* should scan 

in Scala:
```Scala
    // Scan only some packages (this is faster)
    
    val resolver = SubclassesResolverImpl()
                    .withPackagesToScan(List("this.is.myPackage"))
                    .withClassesToScan(List("this.is.myPackage.MyClass")) // and/or this one
                    //.withClassGraph() - or use this one to get full control..       
    
    config = config.withSubclassesResolver( resolver )

```

.. or in Java:
```Java
    // Scan only some packages (this is faster)
    
    final SubclassesResolver resolver = new SubclassesResolverImpl()
                                            .withPackagesToScan(Arrays.asList(
                                               "this.is.myPackage"
                                            ))
                                            .withClassesToScan(Arrays.asList( // and/or this one
                                               "this.is.myPackage.MyClass"
                                            ))
                                            //.withClassGraph() - or use this one to get full control..       
    
    config = config.withSubclassesResolver( resolver )

```



Choosing which DRAFT to generate
--------------------------------

This jsonSchema-generator was originally written to generate schema according to DRAFT-04.
Later more drafts/versions of jsonSchema has arrived.

I've been asked by other developers to not only support DRAFT-04, so I've invested some time reading
all the migration guides from DRAFT-04 to DRAFT-06 to DRAFT-07 to DRAFT-2019-09 ([Migrating from older drafts](https://json-schema.org/specification.html)).

And from what I can see, the only part of the schema generated by this project that is different, is the schema-url.

Therefor I've concluded that this project can generate valid schema for all the versions.

But since the schema-url is different, you must specify which version to use.

In the future, if someone finds bugs and/or add new features, we'll add special-casing for the different versions
when generating the schema.

This is how you specify which version/draft to use:

Specify draft-version in Scala:
```scala
    val config:JsonSchemaConfig = JsonSchemaConfig.vanillaJsonSchemaDraft4.withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07
    val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config = config)
```

Specify draft-version in Java:
```java
    JsonSchemaConfig config = JsonSchemaConfig.vanillaJsonSchemaDraft4().withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07;
    JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);
```




Backstory
--------------


At work we've been using the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema)
to generate schemas used when rendering dynamic GUI using [https://github.com/json-editor/json-editor](https://github.com/json-editor/json-editor).

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
