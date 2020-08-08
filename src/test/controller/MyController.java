package test.controller;

import com.galaxyzeta.annotation.RequestMapping;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.Controller;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

public class MyController implements Controller {

	private static Logger LOG = LoggerFactory.getLogger(MyController.class);

	@RequestMapping(method = "GET", url = "/debug")
	public static Object debugGet(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("GET /debug invoked OK");
		resp.setResponseBody("debug ok");
		resp.setStatusCode("200");
		resp.setStatusDescription("OK");
		return "html/index.html";
	}

	@RequestMapping(method = "POST", url = "/debug")
	public static Object debugPost(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("POST /debug invoked OK");
		return null;
	}
}