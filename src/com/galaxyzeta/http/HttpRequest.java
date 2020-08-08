package com.galaxyzeta.http;

import java.util.HashMap;
import java.util.Set;

public class HttpRequest {
	private HashMap<String, String> requestHeaders;
	private String method;
	private String requestBody;
	private String url;
	private String version = "HTTP/1.1";

	// Constructors
	public HttpRequest() {
	}

	public HttpRequest(String method, String url, HashMap<String, String> headers, String requestBody) {
		this.method = method;
		this.url = url;
		this.requestHeaders = headers;
		this.requestBody = requestBody;
	}

	// Getters
	public String getMethod() {
		return method;
	}
	public String getRequestBody() {
		return requestBody;
	}
	public HashMap<String, String> getRequestHeaders() {
		return requestHeaders;
	}
	public String getUrl() {
		return url;
	}
	public String getVersion() {
		return version;
	}

	// Setters
	public void setHeaders(HashMap<String, String> headers) {
		this.requestHeaders = headers;
	}
	public void setBody(String body) {
		this.requestBody = body;
	}
	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}
	public void setRequestHeaders(HashMap<String, String> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	// toString
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("%s %s %s\n", version, method, url));
		
		if (requestHeaders != null) {
			Set<String> keySet = requestHeaders.keySet();
			for(String key: keySet) {
				sb.append(key).append(": ").append(requestHeaders.get(key)).append('\n');
			}
		}

		sb.append("\n").append(requestBody);
		return sb.toString();
	}
}