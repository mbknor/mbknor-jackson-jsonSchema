package com.kjetland.jackson.jsonSchema.testData.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MixinChild1 extends MixinParent {

    public String child1String;

    @JsonProperty("_child1String2")
    public String child1String2;

    @JsonProperty(value = "_child1String3", required = true)
    public String child1String3;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MixinChild1)) return false;
        if (!super.equals(o)) return false;

        MixinChild1 child1 = (MixinChild1) o;

        if (child1String != null ? !child1String.equals(child1.child1String) : child1.child1String != null)
            return false;
        if (child1String2 != null ? !child1String2.equals(child1.child1String2) : child1.child1String2 != null)
            return false;
        return child1String3 != null ? child1String3.equals(child1.child1String3) : child1.child1String3 == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (child1String != null ? child1String.hashCode() : 0);
        result = 31 * result + (child1String2 != null ? child1String2.hashCode() : 0);
        result = 31 * result + (child1String3 != null ? child1String3.hashCode() : 0);
        return result;
    }
}
