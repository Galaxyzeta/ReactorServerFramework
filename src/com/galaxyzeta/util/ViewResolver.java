package com.galaxyzeta.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.WebApplicationContext;

public class ViewResolver {

	private static Logger LOG = LoggerFactory.getLogger(ViewResolver.class);
	private static final String STATIC_PATH = WebApplicationContext.getStaticPath();
	private static final Pattern RESOURCES_REGEX = Pattern.compile("^.*\\.(html|js|css)$");

	public static void resolve(Object viewObject, HttpResponse resp) {
		if(viewObject instanceof HttpResponse) {
			LOG.INFO("按照 [HttpResponse] 的方式处理视图");
		} else if (viewObject instanceof String) {
			resourceResolver(viewObject, resp);
		} else {
			LOG.INFO("视图无法被正确解析，按 [404 NOT FOUND] 处理");
			resp = ResponseFactory.getNotFound();
		}
	}

	private static void resourceResolver(Object viewObject, HttpResponse resp) {
		String resource = (String)viewObject;
		String filePath = STATIC_PATH+"/"+resource;
		Matcher matcher = RESOURCES_REGEX.matcher(resource);
		if (matcher.matches()) {
			LOG.INFO("按照 [资源] 方式处理视图");
			try {
				File fp = new File(filePath);
				FileReader fr = new FileReader(fp);
				int k;
				StringBuffer sb = new StringBuffer();
				while((k = fr.read()) != -1) {
					sb.append((char)k);
				}
				resp.setResponseBody(sb.toString());
				fr.close();
			} catch (IOException ioe) {
				LOG.ERROR(String.format("文件%s未找到，或其他IO异常", filePath));
			}
		} else {
			resp = ResponseFactory.getNotFound();
			LOG.ERROR(String.format("文件%s不是合法的资源", filePath));
		}
	}
}