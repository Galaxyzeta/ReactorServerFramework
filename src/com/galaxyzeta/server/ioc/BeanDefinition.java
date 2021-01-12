package com.galaxyzeta.server.ioc;

import java.util.ArrayList;

public class BeanDefinition {

	private String name;
	private String classname;
	private String initMethod;
	private ArrayList<PropertyValue> prop = new ArrayList<>();
	private Object bean;
	private Status status = Status.NULL;

	// constructor
	public BeanDefinition(String name, String classname, String initMethod) {
		this.name = name;
		this.classname = classname;
		this.initMethod = initMethod;
	}

	// getter
	public String getClassname() {
		return classname;
	}
	public String getName() {
		return name;
	}
	public ArrayList<PropertyValue> getProp() {
		return prop;
	}
	public Object getBean() {
		return bean;
	}
	public String getInitMethod() {
		return initMethod;
	}
	public Status getStatus() {
		return status;
	}
	
	// setter
	public void setClassname(String classname) {
		this.classname = classname;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setProp(ArrayList<PropertyValue> prop) {
		this.prop = prop;
	}
	public void setBean(Object bean) {
		this.bean = bean;
	}
	public void setInitMethod(String initMethod) {
		this.initMethod = initMethod;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
}
