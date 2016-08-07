package com.kjetland.jackson.jsonSchema.testData;

import java.util.List;
import java.util.Optional;

public class PojoUsingOptionalJava {

    public Optional<String> _string;
    public Optional<Integer> _integer;
    public Optional<Child1> child1;
    public Optional<List<ClassNotExtendingAnything>> optionalList;

    public PojoUsingOptionalJava() {
    }

    public PojoUsingOptionalJava(Optional<String> _string, Optional<Integer> _integer, Optional<Child1> child1, Optional<List<ClassNotExtendingAnything>> optionalList) {
        this._string = _string;
        this._integer = _integer;
        this.child1 = child1;
        this.optionalList = optionalList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PojoUsingOptionalJava that = (PojoUsingOptionalJava) o;

        if (_string != null ? !_string.equals(that._string) : that._string != null) return false;
        if (_integer != null ? !_integer.equals(that._integer) : that._integer != null) return false;
        if (child1 != null ? !child1.equals(that.child1) : that.child1 != null) return false;
        return optionalList != null ? optionalList.equals(that.optionalList) : that.optionalList == null;

    }

    @Override
    public int hashCode() {
        int result = _string != null ? _string.hashCode() : 0;
        result = 31 * result + (_integer != null ? _integer.hashCode() : 0);
        result = 31 * result + (child1 != null ? child1.hashCode() : 0);
        result = 31 * result + (optionalList != null ? optionalList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PojoUsingOptionalJava{" +
                "_string=" + _string +
                ", _integer=" + _integer +
                ", child1=" + child1 +
                ", optionalList=" + optionalList +
                '}';
    }
}
