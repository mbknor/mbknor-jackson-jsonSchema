package com.kjetland.jackson.jsonSchema.testData_issue_24;


public class Business extends AbstractObject {
	private Address employment;
	private Name name;

	public Address getEmployment() {
		return employment;
	}

	public void setEmployment(Address employment) {
		this.employment = employment;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

}
