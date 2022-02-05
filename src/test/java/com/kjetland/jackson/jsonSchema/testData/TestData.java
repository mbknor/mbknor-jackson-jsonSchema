package com.kjetland.jackson.jsonSchema.testData;

import com.kjetland.jackson.jsonSchema.testData.mixin.MixinChild1;
import com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child1;
import com.kjetland.jackson.jsonSchema.testData.polymorphism1.Child2;
import com.kjetland.jackson.jsonSchema.testData.polymorphism1.Parent;
import com.kjetland.jackson.jsonSchema.testData.polymorphism2.Child21;
import com.kjetland.jackson.jsonSchema.testData.polymorphism2.Child22;
import com.kjetland.jackson.jsonSchema.testData.polymorphism3.Child31;
import com.kjetland.jackson.jsonSchema.testData.polymorphism3.Child32;
import com.kjetland.jackson.jsonSchema.testData.polymorphism4.Child41;
import com.kjetland.jackson.jsonSchema.testData.polymorphism4.Child42;
import com.kjetland.jackson.jsonSchema.testData.polymorphism5.Child51;
import com.kjetland.jackson.jsonSchema.testData.polymorphism5.Child52;
import com.kjetland.jackson.jsonSchema.testData.polymorphism6.Child61;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 *
 * @author alex
 */
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
public class TestData {
    Child1 child1 = new Child1();
    {
        child1.parentString = "pv";
        child1.child1String = "cs";
        child1.child1String2 = "cs2";
        child1.child1String3 = "cs3";
    }
    Child2 child2 = new Child2();
    {
        child2.parentString = "pv";
        child2.child2int = 12;
    }
    PojoWithParent pojoWithParent = new PojoWithParent();
    {
        pojoWithParent.pojoValue = true;
        pojoWithParent.child = child1;
        pojoWithParent.stringWithDefault = "y";
        pojoWithParent.intWithDefault = 13;
        pojoWithParent.booleanWithDefault = true;
    }

    Child21 child21 = new Child21();
    {
        child21.parentString = "pv";
        child21.child1String = "cs";
        child21.child1String2 = "cs2";
        child21.child1String3 = "cs3";
    }
    Child22 child22 = new Child22();
    {
        child22.parentString = "pv";
        child22.child2int = 12;
    }

    Child31 child31 = new Child31();
    {
        child31.parentString = "pv";
        child31.child1String = "cs";
        child31.child1String2 = "cs2";
        child31.child1String3 = "cs3";
    }
    Child32 child32 = new Child32(); 
    {
        child32.parentString = "pv";
        child32.child2int = 12;
    }

    Child41 child41 = new Child41();
    Child42 child42 = new Child42();

    Child51 child51 = new Child51();
    {
        child51.parentString = "pv";
        child51.child1String = "cs";
        child51.child1String2 = "cs2";
        child51.child1String3 = "cs3";
    }
    Child52 child52 = new Child52();
    {
        child52.parentString = "pv";
        child52.child2int = 12;
    }
    Child61 child61 = new Child61();
    {
        child61.parentString = "pv";
        child61.child1String = "cs";
        child61.child1String2 = "cs2";
        child61.child1String3 = "cs3";
    }

    ClassNotExtendingAnything classNotExtendingAnything = new ClassNotExtendingAnything();
    {
        classNotExtendingAnything.someString = "Something";
        classNotExtendingAnything.myEnum = MyEnum.C;
    }

    ManyPrimitives manyPrimitives = new ManyPrimitives("s1", 1, 2, true, false, true, 0.1, 0.2, MyEnum.B);

    ManyPrimitives manyPrimitivesNulls = new ManyPrimitives(null, null, 1, null, false, false, null, 0.1, null);

    PojoUsingOptionalJava pojoUsingOptionalJava = new PojoUsingOptionalJava(Optional.of("s"), Optional.of(1), Optional.of(child1), Optional.of(Arrays.asList(classNotExtendingAnything)));

    PojoWithCustomSerializer pojoWithCustomSerializer = new PojoWithCustomSerializer();
    {
        pojoWithCustomSerializer.myString = "xxx";
    }

    ObjectWithPropertyWithCustomSerializer objectWithPropertyWithCustomSerializer = new ObjectWithPropertyWithCustomSerializer("s1", pojoWithCustomSerializer);

    PojoWithArrays pojoWithArrays = new PojoWithArrays(
        new int[] { 1,2,3 },
        new String[] { "a1", "a2", "a3" },
        List.of("l1", "l2", "l3"),
        List.of(child1, child2),
        new Parent[] { child1, child2 },
        List.of(classNotExtendingAnything, classNotExtendingAnything),
        Arrays.asList(Arrays.asList("1","2"), Arrays.asList("3")),
        Set.of(MyEnum.B)
    );

    PojoWithArraysNullable pojoWithArraysNullable = new PojoWithArraysNullable(
        new int[] { 1, 2, 3 },
        new String[] { "a1","a2","a3" },
        List.of("l1", "l2", "l3"),
        List.of(child1, child2),
        new Parent[] { child1, child2 },
        List.of(classNotExtendingAnything, classNotExtendingAnything),
        Arrays.asList(Arrays.asList("1","2"), Arrays.asList("3")),
        Set.of(MyEnum.B)
    );

    RecursivePojo recursivePojo = new RecursivePojo("t1", List.of(new RecursivePojo("c1", null)));

    PojoUsingMaps pojoUsingMaps = new PojoUsingMaps(
        Map.of("a", 1, "b", 2),
        Map.of("x", "y", "z", "w"),
        Map.of("1", child1, "2", child2)
    );

    PojoUsingFormat pojoUsingFormat = new PojoUsingFormat("test@example.com", true, OffsetDateTime.now(), OffsetDateTime.now());
    ManyDates manyDates = new ManyDates(LocalDateTime.now(), OffsetDateTime.now(), LocalDate.now(), org.joda.time.LocalDate.now());

    DefaultAndExamples defaultAndExamples = new DefaultAndExamples("email@example.com", 18, "s", 2, false);

    ClassUsingValidation classUsingValidation = new ClassUsingValidation(
        "_stringUsingNotNull", 
        "_stringUsingNotBlank", 
        "_stringUsingNotBlankAndNotNull", 
        "_stringUsingNotEmpty", 
        List.of("l1", "l2", "l3"), 
        Map.of("mk1", "mv1", "mk2", "mv2"),
        "_stringUsingSize", 
        "_stringUsingSizeOnlyMin", 
        "_stringUsingSizeOnlyMax", 
        "_stringUsingPatternA", 
        "_stringUsingPatternList",
        1, 2, 1.0, 2.0, 1.6, 2.0, 
        "mbk@kjetland.com"
    );

    ClassUsingValidationWithGroups classUsingValidationWithGroups = new ClassUsingValidationWithGroups(
        "_noGroup", "_defaultGroup", "_group1", "_group2", "_group12"
    );

    PojoUsingValidation pojoUsingValidation = new PojoUsingValidation(
        "_stringUsingNotNull", 
        "_stringUsingNotBlank", 
        "_stringUsingNotBlankAndNotNull", 
        "_stringUsingNotEmpty", 
        new String[] { "a1", "a2", "a3" }, 
        List.of("l1", "l2", "l3"),
        Map.of("mk1", "mv1", "mk2", "mv2"), 
        "_stringUsingSize", 
        "_stringUsingSizeOnlyMin", 
        "_stringUsingSizeOnlyMax", 
        "_stringUsingPatternA",
        "_stringUsingPatternList", 
        1, 2, 1.0, 2.0, 1.6, 2.0
    );

    MixinChild1 mixinChild1 = new MixinChild1();
    {
        mixinChild1.parentString = "pv";
        mixinChild1.child1String = "cs";
        mixinChild1.child1String2 = "cs2";
        mixinChild1.child1String3 = "cs3";
    }

    // Test the collision of @NotNull validations and null fields.
    PojoWithNotNull notNullableButNullBoolean = new PojoWithNotNull(null);

    NestedPolymorphism1_1 nestedPolymorphism = new NestedPolymorphism1_1("a1", new NestedPolymorphism2_2("a2", Optional.of(new NestedPolymorphism3("b3"))));

    GenericClass.GenericClassVoid genericClassVoid = new GenericClass.GenericClassVoid();

    MapLike.GenericMapLike genericMapLike = new MapLike.GenericMapLike(Collections.singletonMap("foo", "bar"));

//  KotlinWithDefaultValues kotlinWithDefaultValues = new KotlinWithDefaultValues("1", "2", "3", "4");
}
