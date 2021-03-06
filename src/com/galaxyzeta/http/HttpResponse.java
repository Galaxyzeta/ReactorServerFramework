package com.galaxyzeta.http;

import java.util.HashMap;
import java.util.Set;

public class HttpResponse {
	private String statusCode = "200";
	private String statusDescription = "OK";
	private String version = "HTTP/1.1";
	private String responseBody;
	private HashMap<String, String> responseHeaders = new HashMap<>();
	// Constructor
	public HttpResponse() {}

	public HttpResponse(String statusCode, String statusDescription, String responseBody) {
		this.statusCode = statusCode;
		this.statusDescription = statusDescription;
		this.responseBody = responseBody;
	}

	public HttpResponse(String statusCode, String statusDescription, String responseBody, HashMap<String, String> responseHeaders) {
		this(statusCode, statusDescription, responseBody);
		this.responseHeaders = responseHeaders;
	}

	// Getter
	public String getResponseBody() {
		return responseBody;
	}
	public HashMap<String, String> getResponseHeaders() {
		return responseHeaders;
	}
	public String getStatusCode() {
		return statusCode;
	}
	public String getStatusDescription() {
		return statusDescription;
	}
	public String getVersion() {
		return version;
	}

	// Setter
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}
	public void setResponseHeaders(HashMap<String, String> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	// toString()
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("%s %s %s\n", version, statusCode, statusDescription));
		if (responseHeaders != null) {
			Set<String> keySet = responseHeaders.keySet();
			for(String key: keySet) {
				sb.append(key).append(": ").append(responseHeaders.get(key)).append('\n');
			}
		}
		sb.append("\n").append(responseBody);
		return sb.toString();
	}

	// Common
	public void addResponseHeader(String key, String value) {
		responseHeaders.put(key, value);
	}

	public void copy(HttpResponse resp) {
		this.responseBody = resp.responseBody;
		this.responseHeaders = new HashMap<String, String>(resp.responseHeaders);
		this.statusCode = resp.statusCode;
		this.statusDescription = resp.statusDescription;
		this.version = resp.version;
	}
}