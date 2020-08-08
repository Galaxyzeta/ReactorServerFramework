package com.galaxyzeta.entity;

import java.lang.reflect.Method;


/**
 * 路由信息，包含了请求的类型，请求的url，应调用的方法
 */
public class Router {
	private Method handlerMethod;
	private String method;
	private String url;
	
	// Constructor
	public Router(String method, String url, Method handlerMethod) {
		this.method = method;
		this.handlerMethod = handlerMethod;
		this.url = url;
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
}