package com.galaxyzeta.entity;

import java.lang.reflect.Method;


/**
 * 路由信息，包含了请求的类型，请求的url，应调用的方法
 */
public class Router {
	private Method handlerMethod;
	private String method;
	private String url;
	private String controllerBeanName;
	
	// Constructor
	public Router(String method, String url, Method handlerMethod, String controllerBeanName) {
		this.method = method;
		this.handlerMethod = handlerMethod;
		this.url = url;
		this.controllerBeanName = controllerBeanName;
	}

	// Getter
	public Method getHandlerMethod() {
		return handlerMethod;
	}
	public String getMethod() {
		return method;
	}
	public String getUrl() {
		return url;
	}
	public String getControllerBeanName() {
		return controllerBeanName;
	}

	// Setter
	public void setHandlerMethod(Method handlerMethod) {
		this.handlerMethod = handlerMethod;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setControllerBeanName(String controllerBeanName) {
		this.controllerBeanName = controllerBeanName;
	}
}