package com.kjetland.jackson.jsonSchema.testData_issue_24;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = Person.class, name = "Person"),
		@JsonSubTypes.Type(value = Business.class, name = "Business") })
public abstract class AbstractObject {

}
