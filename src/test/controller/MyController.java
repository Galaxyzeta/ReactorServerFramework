package test.controller;

import java.util.ArrayList;

import com.galaxyzeta.annotation.RequestMapping;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.Controller;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;
import com.galaxyzeta.util.ResponseFactory;

import test.pojo.MyHttpResult;
import test.pojo.QueryList;

public class MyController implements Controller {

	private static Logger LOG = LoggerFactory.getLogger(MyController.class);

	@RequestMapping(method = "GET", url = "/debug")
	public static Object debugGet(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("GET /debug invoked OK");
		return "html/index.html";
	}

	@RequestMapping(method = "GET", url = "/string")
	public static Object debugString(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("GET /string invoked OK");
		HttpResponse myrResponse = ResponseFactory.getSuccess();
		resp.setResponseBody("Hello this is a json view object!");
		return null;
	}

	@RequestMapping(method = "GET", url = "/json")
	public static Object debugJason(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("GET /json invoked OK");
		QueryList ql = new QueryList();
		ArrayList<String> li = new ArrayList<>();
		li.add("asd");
		li.add("qwert");
		ql.setList(li);
		return new MyHttpResult(200, "Json test OK!", ql);
	}
}