package com.kjetland.jackson.jsonSchema.testData;

import java.util.ArrayList;
import java.util.List;

public class GenericClass<T> {

	private T content;

	private List<T> list = new ArrayList<>();

	public T getContent() {
		return content;
	}

	public GenericClass<T> setContent(T content) {
		this.content = content;
		return this;
	}

	public List<T> getList() {
		return list;
	}

	public GenericClass<T> setList(List<T> list) {
		this.list = list;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		GenericClass<?> that = (GenericClass<?>) o;

		return content != null ? content.equals(that.content) : that.content == null;
	}

	@Override
	public int hashCode() {
		return content != null ? content.hashCode() : 0;
	}

	public static class GenericClassVoid extends GenericClass<Void> {
	}
}
