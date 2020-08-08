package com.galaxyzeta.util;

public class Logger {

	private String className;

	Logger(Class<?> className) {
		this.className = className.getSimpleName();
	}

	public final void DEBUG(String str) {
		System.out.printf("[DEBUG]\t%s -- %s\n", className, str);
	}

	public final void INFO(String str) {
		System.out.printf("[INFO]\t%s -- %s\n", className, str);
	}

	public final void WARN(String str) {
		System.out.printf("[WARN]\t%s -- %s\n", className, str);
	}

	public final void ERROR(String str) {
		System.out.printf("[ERROR]\t%s -- %s\n", className, str);
	}
}