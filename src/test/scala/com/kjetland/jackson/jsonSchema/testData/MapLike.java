package com.kjetland.jackson.jsonSchema.testData;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public interface MapLike<T> extends Map<String, String> {
	void accept(T content);

	@JsonDeserialize(using = MyDeserializer.class)
	class GenericMapLike implements MapLike<MyEnum> {

		private final Map<String, String> innerMap;

		public GenericMapLike(Map<String, String> innerMap) {
			this.innerMap = new LinkedHashMap<>(innerMap);
		}

		@Override
		public void accept(MyEnum content) {
			innerMap.put(content.name(), content.name());
		}

		@Override
		public int size() {
			return innerMap.size();
		}

		@Override
		public boolean isEmpty() {
			return innerMap.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return innerMap.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return innerMap.containsValue(value);
		}

		@Override
		public String get(Object key) {
			return innerMap.get(key);
		}

		@Override
		public String put(String key, String value) {
			return innerMap.put(key, value);
		}

		@Override
		public String remove(Object key) {
			return innerMap.remove(key);
		}

		@Override
		public void putAll(Map<? extends String, ? extends String> m) {
			innerMap.putAll(m);
		}

		@Override
		public void clear() {
			innerMap.clear();
		}

		@Override
		public Set<String> keySet() {
			return innerMap.keySet();
		}

		@Override
		public Collection<String> values() {
			return innerMap.values();
		}

		@Override
		public Set<Entry<String, String>> entrySet() {
			return innerMap.entrySet();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			GenericMapLike that = (GenericMapLike) o;

			return innerMap.equals(that.innerMap);
		}

		@Override
		public int hashCode() {
			return innerMap.hashCode();
		}
	}

	class MyDeserializer extends JsonDeserializer<GenericMapLike> {
		@Override
		public GenericMapLike deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException
		{
			Map<String, String> map = p.readValueAs(Map.class);
			return new GenericMapLike(map);
		}
	}
}
