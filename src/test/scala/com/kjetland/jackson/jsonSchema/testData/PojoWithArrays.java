package com.kjetland.jackson.jsonSchema.testData;

import java.util.Arrays;
import java.util.List;

public class PojoWithArrays {

    // It was difficult to construct this from scala :)
    public static List<List<String>> _listOfListOfStringsValues = Arrays.asList(Arrays.asList("1","2"), Arrays.asList("3"));

    public int[] intArray1;
    public String[] stringArray;

    public List<String> stringList;

    public List<Parent> polymorphismList;
    public Parent[] polymorphismArray;
    public List<ClassNotExtendingAnything> regularObjectList;

    public List<List<String>> listOfListOfStrings;

    public PojoWithArrays() {
    }

    public PojoWithArrays(int[] intArray1, String[] stringArray, List<String> stringList, List<Parent> polymorphismList, Parent[] polymorphismArray, List<ClassNotExtendingAnything> regularObjectList, List<List<String>> listOfListOfStrings) {
        this.intArray1 = intArray1;
        this.stringArray = stringArray;
        this.stringList = stringList;
        this.polymorphismList = polymorphismList;
        this.polymorphismArray = polymorphismArray;
        this.regularObjectList = regularObjectList;
        this.listOfListOfStrings = listOfListOfStrings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PojoWithArrays)) return false;

        PojoWithArrays that = (PojoWithArrays) o;

        if (!Arrays.equals(intArray1, that.intArray1)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(stringArray, that.stringArray)) return false;
        if (stringList != null ? !stringList.equals(that.stringList) : that.stringList != null) return false;
        if (polymorphismList != null ? !polymorphismList.equals(that.polymorphismList) : that.polymorphismList != null)
        if (regularObjectList != null ? !regularObjectList.equals(that.regularObjectList) : that.regularObjectList != null)
            return false;
        if (listOfListOfStrings != null ? !listOfListOfStrings.equals(that.listOfListOfStrings) : that.listOfListOfStrings != null)
            return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(polymorphismArray, that.polymorphismArray);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(intArray1);
        result = 31 * result + Arrays.hashCode(stringArray);
        result = 31 * result + (stringList != null ? stringList.hashCode() : 0);
        result = 31 * result + (polymorphismList != null ? polymorphismList.hashCode() : 0);
        result = 31 * result + (regularObjectList != null ? regularObjectList.hashCode() : 0);
        result = 31 * result + (listOfListOfStrings != null ? listOfListOfStrings.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(polymorphismArray);
        return result;
    }
}
