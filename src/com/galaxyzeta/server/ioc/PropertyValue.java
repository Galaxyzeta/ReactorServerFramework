package com.galaxyzeta.server.ioc;

public class PropertyValue {
	private Object value;
	private Type type;
	private String name;

	public static enum Type {
		VALUE, REF;
	}

	// constructor
	PropertyValue(String name, Object value, Type type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}
	
	// getter
	public Object getValue() {
		return value;
	}
	public Type getType() {
		return type;
	}
	public String getName() {
		return name;
	}

	// setter
	public void setType(Type type) {
		this.type = type;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public void setName(String name) {
		this.name = name;
	}
}