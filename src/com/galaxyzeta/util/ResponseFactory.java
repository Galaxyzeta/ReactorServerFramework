package com.galaxyzeta.util;

import com.galaxyzeta.http.HttpResponse;

public class ResponseFactory {
	
	public static HttpResponse getSuccess() {
		return new HttpResponse("200", "OK", null);
	}

	public static HttpResponse getRedirect() {
		return new HttpResponse("302", "REDIRECT", null);
	}

	public static HttpResponse getNotFound() {
		return new HttpResponse("404", "NOT FOUND", null);
	}

	public static HttpResponse getInternalServerError() {
		return new HttpResponse("500", "ERROR", null);
	}
}