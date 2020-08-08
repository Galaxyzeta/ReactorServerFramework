package test.interceptor;

import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.Interceptor;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

public class MyInterceptor2 implements Interceptor {

	private static Logger LOG = LoggerFactory.getLogger(MyInterceptor1.class);

	public static boolean intercept(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("请求正在通过 [拦截器2] 的过滤");
		return true;
	}
}