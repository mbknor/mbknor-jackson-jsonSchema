package com.kjetland.jackson.jsonSchema.testData;

public class ClassNotExtendingAnything {

    public String someString;
    public MyEnum myEnum;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassNotExtendingAnything)) return false;

        ClassNotExtendingAnything that = (ClassNotExtendingAnything) o;

        if (someString != null ? !someString.equals(that.someString) : that.someString != null) return false;
        return myEnum == that.myEnum;

    }

    @Override
    public int hashCode() {
        int result = someString != null ? someString.hashCode() : 0;
        result = 31 * result + (myEnum != null ? myEnum.hashCode() : 0);
        return result;
    }
}
