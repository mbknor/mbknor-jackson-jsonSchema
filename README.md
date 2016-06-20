Jackson jsonSchema Generator
===================================
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.kjetland/mbknor-jackson-jsonschema_2.11/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cmbknor-jackson-jsonSchema)

This projects aims to do a better job than the original [jackson-module-jsonSchema](https://github.com/FasterXML/jackson-module-jsonSchema)
in generating jsonSchema from your POJOs using Jackson @Annotations.

Current version: *1.0.0*

**Highlights**

* JSON Schema Draft v4
* Supports polymorphism using **@JsonTypeInfo** and **oneOf**

**Benefits**

* Simple implementation - Just [one file](https://github.com/mbknor/mbknor-jackson-jsonSchema/blob/master/src/main/scala/com/kjetland/jackson/jsonSchema/JsonSchemaGenerator.scala)  (for now..) 
* Implemented in Scala 
* Easy to fix and add functionality

**The Future**

* Should support all different variations of Jackson-usage
* Should support all (a lot?) of *javax.validation-API* 


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


Using Maven
-----------------
 
Add this to you pom.xml:

    <dependency>
        <groupId>com.kjetland</groupId>
        <artifactId>mbknor-jackson-jsonschema_2.11</artifactId>
        <version>1.0.0</version>
    </dependency>    

Using sbt
------------
 
Add this to you sbt build-config:

    "com.kjetland" % "mbknor-jackson-jsonschema" %% "1.0.0"


Code
-------------------------------

This is how to generate jsonSchema in code using Scala:

```scala
    val objectMapper = new ObjectMapper
    val jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper)
    val jsonSchema:JsonNode = jsonSchemaGenerator.generateJsonSchema(classOf[YourPOJO])
    
    val jsonSchemaAsString:String = objectMapper.writeValueAsString(jsonSchema)
```

And using Java:

```java
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
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
