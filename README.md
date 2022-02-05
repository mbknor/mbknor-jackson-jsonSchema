**NOTE:** This is the Java rewrite of the original project, without Scala dependencies. It is a version-compatible drop-in replacement, except that configuration is now via a builder.

Jackson jsonSchema Generator
===================================

This projects aims to do a better job than the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema)
in generating jsonSchema from your POJOs using Jackson @Annotations.


**Highlights**

* JSON Schema DRAFT-04, DRAFT-06, DRAFT-07 and DRAFT-2019-09
* Supports polymorphism (**@JsonTypeInfo**, **MixIn**, and **registerSubtypes()**) using JsonSchema's **oneOf**-feature.
* Supports schema customization using:
  - **@JsonSchemaDescription**/**@JsonPropertyDescription**
  - **@JsonSchemaFormat**
  - **@JsonSchemaTitle**
  - **@JsonProperty(.., defaultValue)** / **@JsonSchemaDefault**
  - **@JsonSchemaOptions**
  - **@JsonSchemaInject**
  - **@JsonSchemaExamples**
* Supports many Javax-validation @Annotations including support for validation-groups
* Works well with Generated GUI's using [https://github.com/json-editor/json-editor](https://github.com/json-editor/json-editor)
  - (Must be configured to use this mode)
  - Special handling of Option-/Optional-properties using oneOf.
* Supports custom Class-to-format-Mapping
* Supports injecting custom json-schema-fragments using the **@JsonSchemaInject**-annotation.


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


.. like this
```Java

@JsonSerialize(using = MySpecialSerializer.class)
@JsonSchemaInject( json = """
      {
        "patternProperties" : {
          "^[a-zA-Z0-9]+" : {
            "type" : "string"
          }
        }
      }""")
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
```Java
class MyPojo {
  @JsonSchemaInject(jsonSupplier = UserNamesLoader.class)
  Set<String> uns;
}

class UserNamesLoader implements Supplier<JsonNode> {
  ObjectMapper objectMapper = new ObjectMapper()

  @Override public JsonNode get() {
    var schema = objectMapper.createObjectNode();
    var valuesNode = schema.putObject("items").putArray("enum");
    for (var user : loadUsers())
      valuesNode.add(user.name);
    return schema;
  }
}
```
This will associate an enum of possible values for the set that you generate at runtime.

If you need even more control over the schema-generating runtime, you can use **@JsonSchemaInject.jsonSupplierViaLookup**
like this:

```Scala
class MyPojo {
  @JsonSchemaInject(jsonSupplierViaLookup = "theKeyToUseWhenLookingUpASupplier")
  Set<String> uns;
  ...
  ...
  ...
}
```

Then you have to add the mapping between the key 'theKeyToUseWhenLookingUpASupplier' and the Supplier-instance in the config-object
used when creating the JsonSchemaGenerator.


The default behaviour of `@JsonSchemaInject` is to **merge** the injected json into the generated JsonSchema.
If you want to have full control over it, you can specify `@JsonSchemaInject(overrideAll = true)` to **replace** the generated
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

This project publishes artifacts to central maven repo. The project requires Java 11.

Using Maven
-----------------

Add this to you pom.xml:

    <dependency>
        <groupId>net.almson</groupId>
        <artifactId>mbknor-jackson-jsonschema-java</artifactId>
        <version>1.0.39.2</version>
    </dependency>


Examples
-------------------------

```java
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

    // If using JsonSchema to generate a JSON Editor interface:
    // JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.JSON_EDITOR);

    // If you want to configure it manually:
    // JsonSchemaConfig config = JsonSchemaConfig.create(...);
    // JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);


    JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(YourPOJO.class);

    String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
```

**Nullable types**

Out of the box, the generator does not support nullable types. There is a preconfigured `JsonSchemaGenerator` configuration shortcut that can be used to enable them:

```java
JsonSchemaConfig config = JsonSchemaConfig.NULLABLE;
JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);
```

Under the hood `NULLABLE` toggles the `useOneOfForOption` and `useOneOfForNullables` properties on `JsonSchemaConfig`.

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

Here is how to do it:

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

```Java
    // Scan only some packages (this is faster)
    
    final SubclassesResolver resolver = new SubclassesResolver(List.of(
                                               "this.is.myPackage" // packages to include
                                            ),
                                            List.of(
                                               "this.is.myPackage.MyClass" // classes to include
                                            ));
    config = JsonSchemaConfig.builder().subclassesResolver(resolver).build();

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

Specify draft-version:
```java
    JsonSchemaConfig config = JsonSchemaConfig.builder().jsonSchemaDraft(JsonSchemaDraft.DRAFT_07).build();
    JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);
```
