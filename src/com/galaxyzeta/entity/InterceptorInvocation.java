package com.galaxyzeta.entity;

import java.lang.reflect.Method;

public class InterceptorInvocation {
	
	private Method interceptorMethod;
	
	private String interceptorBeanName;

	public InterceptorInvocation(String interceptorBeanName, Method interceptorMethod) {
		this.interceptorBeanName = interceptorBeanName;
		this.interceptorMethod = interceptorMethod;
	}

	public String getInterceptorBeanName() {
		return interceptorBeanName;
	}
	public Method getInterceptorMethod() {
		return interceptorMethod;
	}
	
	public void setInterceptorBeanName(String interceptorBeanName) {
		this.interceptorBeanName = interceptorBeanName;
	}
	public void setInterceptorMethod(Method interceptorMethod) {
		this.interceptorMethod = interceptorMethod;
	}
	
}
