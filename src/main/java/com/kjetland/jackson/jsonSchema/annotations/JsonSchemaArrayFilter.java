package com.kjetland.jackson.jsonSchema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When serializing a list of polymorphic classes to JSON schema, the {@code "items"} will supply
 * all of the classes which extend out the base class used in the list. If only a subset of
 * implementation classes are permitted within the list then this annotation can be used to filter
 * out only the acceptable classes.
 *
 * <p>Note: Only polymorphic classes are supported.
 *
 * <p>For example, a list of a base class {@code ABaseClass}, which has implementation classes
 * {@code ClassA}, {@code ClassB}, and {@code ClassC}, could permit only {@code ClassA} and {@code
 * ClassC} through:
 *
 * <p>
 * <code><pre>
 *   public class Test {
 *
 *     &#64;JsonSchemaArrayFilter({ClassA.class, ClassC.class})
 *     private List&lt;ABaseClass&gt;;
 *
 *   }
 * </pre></code>
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSchemaArrayFilter {

  Class<?>[] value();

}
