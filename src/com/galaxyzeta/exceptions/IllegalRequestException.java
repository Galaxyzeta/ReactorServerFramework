package com.galaxyzeta.exceptions;

public class IllegalRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public IllegalRequestException(String message) {
		super(message);
	}
}