Jackson jsonSchema Generator
===================================
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.kjetland/mbknor-jackson-jsonschema_2.11/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cmbknor-jackson-jsonSchema)

This projects aims to do a better job than the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema)
in generating jsonSchema from your POJOs using Jackson @Annotations.

Current version: *1.0.1*

**Highlights**

* JSON Schema Draft v4
* Supports polymorphism using **@JsonTypeInfo** and **oneOf**
* Supports schema customization using @JsonSchemaDescription, @JsonSchemaFormat and @JsonSchemaTitle
* Works well with Generated GUI's using [https://github.com/jdorn/json-editor](https://github.com/jdorn/json-editor) - NB: Must be configured to use this mode

**Benefits**

* Simple implementation - Just [one file](https://github.com/mbknor/mbknor-jackson-jsonSchema/blob/master/src/main/scala/com/kjetland/jackson/jsonSchema/JsonSchemaGenerator.scala)  (for now..) 
* Implemented in Scala 
* Easy to fix and add functionality

**The Future**

* Should support all different variations of Jackson-usage
* Should support all (a lot?) of *javax.validation-API*-annotations


Project status
---------------
We're currently using this codebase in an ongoing (not yet released) project at work,
and we're improving the jsonSchema-generating code when we finds issues and/or features we need that not yet is supported.

**Currently missing**

* Lot of javax.validation-API support

I would really appreciate it if other developers wanted to start using and contributing improvements and features. 

Dependency
===================

This project publishes artifacts to central maven repo.

The project is also compiled using Java 8. This means that you also need to use Java 8.


Using Maven
-----------------
 
Add this to you pom.xml:

    <dependency>
        <groupId>com.kjetland</groupId>
        <artifactId>mbknor-jackson-jsonschema_2.11</artifactId>
        <version>1.0.1</version>
    </dependency>    

Using sbt
------------
 
Add this to you sbt build-config:

    "com.kjetland" % "mbknor-jackson-jsonschema" %% "1.0.1"


Code
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


**Note about Scala and Option[Int]**:

Due to Java's Type Erasure it impossible to resolve the type T behind Option[T] when T is Int, Boolean, Double.
Ass a workaround, you have to use the *@JsonDeserialize*-annotation in such cases.
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

And using Java:

```java
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    
    // If using JsonSchema to generate HTML5 GUI:
    // JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema() );
    
    JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(YourPOJO.class);
    
    String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
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
