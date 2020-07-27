package services;

import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;

public final class RangeKeyMatches {
    public static final RangeKeyCondition rangeKeyMatches(String attrName, Object val) {
        return new RangeKeyCondition(attrName).eq(val);
    }
}
