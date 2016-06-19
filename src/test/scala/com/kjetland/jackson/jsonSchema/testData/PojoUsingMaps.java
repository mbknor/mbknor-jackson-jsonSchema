package com.kjetland.jackson.jsonSchema.testData;

import java.util.Map;

public class PojoUsingMaps {

    public Map<String, Integer> string2Integer;
    public Map<String, String> string2String;
    public Map<String, Parent> string2PojoUsingJsonTypeInfo;

    public PojoUsingMaps() {
    }

    public PojoUsingMaps(Map<String, Integer> string2Integer, Map<String, String> string2String, Map<String, Parent> string2PojoUsingJsonTypeInfo) {
        this.string2Integer = string2Integer;
        this.string2String = string2String;
        this.string2PojoUsingJsonTypeInfo = string2PojoUsingJsonTypeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PojoUsingMaps that = (PojoUsingMaps) o;

        if (string2Integer != null ? !string2Integer.equals(that.string2Integer) : that.string2Integer != null)
            return false;
        if (string2String != null ? !string2String.equals(that.string2String) : that.string2String != null)
            return false;
        return string2PojoUsingJsonTypeInfo != null ? string2PojoUsingJsonTypeInfo.equals(that.string2PojoUsingJsonTypeInfo) : that.string2PojoUsingJsonTypeInfo == null;

    }

    @Override
    public int hashCode() {
        int result = string2Integer != null ? string2Integer.hashCode() : 0;
        result = 31 * result + (string2String != null ? string2String.hashCode() : 0);
        result = 31 * result + (string2PojoUsingJsonTypeInfo != null ? string2PojoUsingJsonTypeInfo.hashCode() : 0);
        return result;
    }
}
