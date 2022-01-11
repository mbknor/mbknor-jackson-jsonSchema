package com.kjetland.jackson.jsonSchema.testData;

import javax.validation.constraints.NotNull;

public class ManyPrimitives {
    public String _string;
    public Integer _integer;
    public int _int;
    public Boolean _booleanObject;
    public boolean _booleanPrimitive;

    @NotNull
    public Boolean _booleanObjectWithNotNull;
    public Double _doubleObject;
    public double _doublePrimitive;
    public MyEnum myEnum;

    public ManyPrimitives() {
    }

    public ManyPrimitives(String _string, Integer _integer, int _int, Boolean _booleanObject, boolean _booleanPrimitive, Boolean _booleanObjectWithNotNull, Double _doubleObject, double _doublePrimitive, MyEnum myEnum) {
        this._string = _string;
        this._integer = _integer;
        this._int = _int;
        this._booleanObject = _booleanObject;
        this._booleanPrimitive = _booleanPrimitive;
        this._booleanObjectWithNotNull = _booleanObjectWithNotNull;
        this._doubleObject = _doubleObject;
        this._doublePrimitive = _doublePrimitive;
        this.myEnum = myEnum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManyPrimitives)) return false;

        ManyPrimitives that = (ManyPrimitives) o;

        if (_int != that._int) return false;
        if (_booleanPrimitive != that._booleanPrimitive) return false;
        if (Double.compare(that._doublePrimitive, _doublePrimitive) != 0) return false;
        if (_string != null ? !_string.equals(that._string) : that._string != null) return false;
        if (_integer != null ? !_integer.equals(that._integer) : that._integer != null) return false;
        if (_booleanObject != null ? !_booleanObject.equals(that._booleanObject) : that._booleanObject != null) return false;
        if (_booleanObjectWithNotNull != null ? !_booleanObjectWithNotNull.equals(that._booleanObjectWithNotNull) : that._booleanObjectWithNotNull != null) return false;
        if (_doubleObject != null ? !_doubleObject.equals(that._doubleObject) : that._doubleObject != null) return false;
        return myEnum == that.myEnum;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = _string != null ? _string.hashCode() : 0;
        result = 31 * result + (_integer != null ? _integer.hashCode() : 0);
        result = 31 * result + _int;
        result = 31 * result + (_booleanObject != null ? _booleanObject.hashCode() : 0);
        result = 31 * result + (_booleanPrimitive ? 1 : 0);
        result = 31 * result + (_booleanObjectWithNotNull != null ? _booleanObjectWithNotNull.hashCode() : 0);
        result = 31 * result + (_doubleObject != null ? _doubleObject.hashCode() : 0);
        temp = Double.doubleToLongBits(_doublePrimitive);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (myEnum != null ? myEnum.hashCode() : 0);
        return result;
    }
}
