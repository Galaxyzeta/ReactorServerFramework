package com.galaxyzeta.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.WebApplicationContext;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;
import com.galaxyzeta.util.ResponseFactory;


public class ViewResolver {

	private static Logger LOG = LoggerFactory.getLogger(ViewResolver.class);
	private static final Pattern RESOURCES_REGEX = Pattern.compile("^.*\\.(html|js|css)$");
	private static String STATIC_PATH;
	private WebApplicationContext context;

	public ViewResolver(){}

	public ViewResolver(WebApplicationContext context) {
		this.context = context;
		STATIC_PATH = context.getStaticPath();
	}

	public void setContext(WebApplicationContext context) {
		this.context = context;
		STATIC_PATH = context.getStaticPath();
	}

	public void resolve(Object viewObject, HttpResponse resp) {
		if(viewObject instanceof HttpResponse) {
			LOG.INFO("按照 [HttpResponse] 的方式处理视图");
		} else if (viewObject instanceof String) {
			resourceResolver(viewObject, resp);
		} else if (viewObject != null){
			LOG.INFO("按照 Json 解析进行处理");
			resp.setResponseBody(JasonConverter.convert(viewObject));
		}
	}

	private void resourceResolver(Object viewObject, HttpResponse resp) {
		String resource = (String)viewObject;
		String filePath = STATIC_PATH+resource;
		Matcher matcher = RESOURCES_REGEX.matcher(resource);
		if (matcher.matches()) {
			LOG.INFO("按照 [资源] 方式处理视图");
			try {
				File fp = new File(filePath);
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fp)));

				StringBuilder sb = new StringBuilder();
				int c;
				while((c = br.read()) != -1) {
					sb.append((char)c);
				}
				resp.setResponseBody(sb.toString());
				br.close();

				LOG.INFO("资源 "+ filePath + "解析完毕");
			} catch (IOException ioe) {
				ResponseFactory.setNotFound(resp);
				LOG.ERROR(String.format("文件%s未找到，或其他IO异常", filePath));
			}
		} else {
			ResponseFactory.setNotFound(resp);
			LOG.ERROR(String.format("文件%s不是合法的资源", filePath));
		}
	}
}