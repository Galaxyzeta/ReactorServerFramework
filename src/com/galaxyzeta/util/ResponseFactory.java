package com.galaxyzeta.util;

import java.util.HashMap;

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

	public static void setNotFound(HttpResponse resp) {
		resp.setStatusCode("404");
		resp.setResponseBody("NOT FOUND!");
		resp.setStatusDescription("NOT_FOUND");
	}
}