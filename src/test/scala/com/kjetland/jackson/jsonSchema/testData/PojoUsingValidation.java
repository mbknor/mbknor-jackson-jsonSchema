package com.kjetland.jackson.jsonSchema.testData;

import com.kjetland.jackson.jsonSchema.testDataScala.ClassUsingValidation;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provides a test POJO with javax.validation annotations on it. This is the Java equivalent of
 * {@link ClassUsingValidation}, and it exists because certain types are used differently between Scala and Java.
 *
 * @see ClassUsingValidation
 */
public class PojoUsingValidation {
    @NotNull
    public String stringUsingNotNull;

    @NotBlank
    public String stringUsingNotBlank;

    @NotNull
    @NotBlank
    public String stringUsingNotBlankAndNotNull;

    @NotEmpty
    public String stringUsingNotEmpty;

    @NotEmpty
    public String[] notEmptyStringArray;

    @NotEmpty
    public List<String> notEmptyStringList;

    @NotEmpty
    public Map<String, String> notEmptyStringMap;

    @Size(min = 1, max = 20)
    public String stringUsingSize;

    @Size(min = 1)
    public String stringUsingSizeOnlyMin;

    @Size(max = 30)
    public String stringUsingSizeOnlyMax;

    @Pattern(regexp = "_stringUsingPatternA|_stringUsingPatternB")
    public String stringUsingPattern;

    @Pattern.List({
            @Pattern(regexp = "^_stringUsing.*"),
            @Pattern(regexp = ".*PatternList$")
    })
    public String stringUsingPatternList;

    @Min(1)
    public int intMin;

    @Max(10)
    public int intMax;

    @Min(1)
    public double doubleMin;

    @Max(10)
    public double doubleMax;

    public PojoUsingValidation() {

    }

    public PojoUsingValidation(final String stringUsingNotNull,final String stringUsingNotBlank, final String stringUsingNotBlankAndNotNull, final String stringUsingNotEmpty,
                               final String[] notEmptyStringArray, final List<String> notEmptyStringList, final Map<String, String> notEmptyStringMap,
                               final String stringUsingSize,final String stringUsingSizeOnlyMin, final String stringUsingSizeOnlyMax, final String stringUsingPattern,
                               final String stringUsingPatternList, final int intMin, final int intMax, final double doubleMin, final double doubleMax) {
        this.stringUsingNotNull = stringUsingNotNull;
        this.stringUsingNotBlank = stringUsingNotBlank;
        this.stringUsingNotBlankAndNotNull = stringUsingNotBlankAndNotNull;
        this.stringUsingNotEmpty = stringUsingNotEmpty;
        this.notEmptyStringArray = notEmptyStringArray;
        this.notEmptyStringList = notEmptyStringList;
        this.notEmptyStringMap = notEmptyStringMap;
        this.stringUsingSize = stringUsingSize;
        this.stringUsingSizeOnlyMin = stringUsingSizeOnlyMin;
        this.stringUsingSizeOnlyMax = stringUsingSizeOnlyMax;
        this.stringUsingPattern = stringUsingPattern;
        this.stringUsingPatternList = stringUsingPatternList;
        this.intMin = intMin;
        this.intMax = intMax;
        this.doubleMin = doubleMin;
        this.doubleMax = doubleMax;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PojoUsingValidation that = (PojoUsingValidation) o;
        return intMin == that.intMin &&
                intMax == that.intMax &&
                Double.compare(that.doubleMin, doubleMin) == 0 &&
                Double.compare(that.doubleMax, doubleMax) == 0 &&
                Objects.equals(stringUsingNotNull, that.stringUsingNotNull) &&
                Objects.equals(stringUsingNotBlank, that.stringUsingNotBlank) &&
                Objects.equals(stringUsingNotBlankAndNotNull, that.stringUsingNotBlankAndNotNull) &&
                Objects.equals(stringUsingNotEmpty, that.stringUsingNotEmpty) &&
                Arrays.equals(notEmptyStringArray, that.notEmptyStringArray) &&
                Objects.equals(notEmptyStringList, that.notEmptyStringList) &&
                Objects.equals(notEmptyStringMap, that.notEmptyStringMap) &&
                Objects.equals(stringUsingSize, that.stringUsingSize) &&
                Objects.equals(stringUsingSizeOnlyMin, that.stringUsingSizeOnlyMin) &&
                Objects.equals(stringUsingSizeOnlyMax, that.stringUsingSizeOnlyMax) &&
                Objects.equals(stringUsingPattern, that.stringUsingPattern) &&
                Objects.equals(stringUsingPatternList, that.stringUsingPatternList);
    }

    @Override
    public int hashCode() {

        int result = Objects
                .hash(stringUsingNotNull, stringUsingNotBlank, stringUsingNotBlankAndNotNull, stringUsingNotEmpty, notEmptyStringList, notEmptyStringMap, stringUsingSize, stringUsingSizeOnlyMin, stringUsingSizeOnlyMax, stringUsingPattern, stringUsingPatternList, intMin, intMax, doubleMin, doubleMax);
        result = 31 * result + Arrays.hashCode(notEmptyStringArray);
        return result;
    }
}
