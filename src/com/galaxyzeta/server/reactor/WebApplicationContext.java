package com.galaxyzeta.server.reactor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.net.URISyntaxException;
import java.net.URL;

import com.galaxyzeta.parser.ConfigParser;
import com.galaxyzeta.server.ioc.IocContainer;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

import com.galaxyzeta.annotation.RequestMapping;
import com.galaxyzeta.entity.Router;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;

/**
 *  内置 Ioc 容器的 Web 运行上下文。
 */
public class WebApplicationContext {

	private final Logger LOG = LoggerFactory.getLogger(WebApplicationContext.class);
	private final HashMap<String, String> CONFIGS = new HashMap<>();
	private final HashMap<String, HashMap<String, Router>> ROUTERS = new HashMap<>();
	private final ArrayList<Method> INTERCEPTORS = new ArrayList<>();
	private final ArrayList<Class<?>> CONTROLLERS = new ArrayList<>();
	private IocContainer iocContainter;

	private static final String INTERCEPTOR_INTERFACE_NAME = "Interceptor";
	private static final String INTERCEPTOR_METHOD_NAME = "intercept";

	// 对外提供 Controller 路由查询
	public Router searchRouter(String method, String url) {
		return ROUTERS.get(method).get(url);
	}

	// 对外提供拦截器方法
	public ArrayList<Method> getInterceptors() {
		return INTERCEPTORS;
	}

	// 对外提供获取static路径方法
	public String getStaticPath() {
		return CONFIGS.get("static");
	}

	// 对外提供获取 Ioc 容器的方法
	public IocContainer getIocContainer() {
		return this.iocContainter;
	}

	// 初始化 方法 -- URL Mapping 哈希表
	{
		String[] list = new String[] {"GET", "POST", "PUT", "DELETE", "PATCH", "OPTION"};
		for(String httpMethod: list) {
			ROUTERS.put(httpMethod, new HashMap<>());
		}
	}
	
	// 包扫描
	public void packageScanner(String packageName, List<Class<?>> classList) {
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Enumeration<URL> urls = loader.getResources(packageName);
			while (urls.hasMoreElements()) {
				URL currentFolder = urls.nextElement();
				File fp = new File(currentFolder.toURI());
				File[] fileList = fp.listFiles();
				for (File singleFile : fileList) {
					if(singleFile.isDirectory()) {
						packageScanner(singleFile.getName(), classList);
					}
					String className = (packageName + "/" + singleFile.getName().split("\\.")[0]).replace("/", ".");
					classList.add(Class.forName(className));
				}
			}
		} catch (IOException | URISyntaxException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// 实现了 Controller 接口的子类是控制器，含有 @ReqeustMapping 的方法为控制器路由方法，这些方法将被全部记录在 hashmap 中
	// 使用 hashmap 维护
	private void controllerAnnotationRegister() {
		for (Class<?> controller : CONTROLLERS) {
			try {
				Method[] handlers = controller.getDeclaredMethods();
				for(Method singleHandler: handlers) {
					RequestMapping requestMapping = singleHandler.getAnnotation(RequestMapping.class);

					if (requestMapping == null) {
						continue;
					}

					Router router = new Router(requestMapping.method(), requestMapping.url(), singleHandler);
					ROUTERS.get(requestMapping.method()).put(requestMapping.url(), router);
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	// 注册拦截器
	private void interceptorRegistration() {
		String interceptorBase = CONFIGS.get("interceptor-base-pacakge").trim();
		if(interceptorBase == null) {	
			LOG.WARN("拦截器根目录没有定义，放弃配置拦截器");
			return;
		}
		String[] _interceptors = CONFIGS.getOrDefault("interceptors", "").split(",");
		String interceptorClassName = null;

		for (String interceptor: _interceptors) {
			try {
				interceptorClassName = (interceptorBase + "/" + interceptor.trim()).replace("/", ".");
				// 验证是否是合法的 Interface
				boolean isValidImplmentation = false;
				Class<?> interceptorClass = Class.forName(interceptorClassName);
				Class<?>[] interfaces = interceptorClass.getInterfaces();
				for(Class<?> interfaceClass: interfaces) {
					if (interfaceClass.getSimpleName().equals(INTERCEPTOR_INTERFACE_NAME)) {
						isValidImplmentation = true;
					}
				}
				if(! isValidImplmentation) {
					LOG.ERROR(String.format("启动失败，拦截器必须实现 %s 接口", INTERCEPTOR_INTERFACE_NAME));
					System.exit(1);
				} else {
					INTERCEPTORS.add(interceptorClass.getDeclaredMethod("intercept", HttpRequest.class, HttpResponse.class));
					LOG.INFO(String.format("拦截器 %s 注册成功", interceptorClassName));
				}
			} catch (ClassNotFoundException e ) {
				LOG.ERROR(String.format("启动失败，定义在配置文件中的拦截器 %s 没有找到", interceptorClassName));
				System.exit(1);
			} catch (NoSuchMethodException e ) {
				LOG.ERROR(String.format("方法不存在，拦截器必须有 public static boolean %s (HttpRequest, HttpResponse) 方法", INTERCEPTOR_METHOD_NAME));
				System.exit(1);
			}
		}
	}
	
	// 运行一个Reactor架构的服务器
	public void runApplication(String configFile) {
		
		// 读取配置
		ConfigParser configParser = new ConfigParser(configFile);
		CONFIGS.putAll(configParser.parse());
		LOG.INFO("配置文件解析完毕");

		// 赋予配置
		String port = CONFIGS.getOrDefault("port", "8080");

		// Ioc 容器注册
		iocContainter = new IocContainer(CONFIGS.getOrDefault("ioc-xml-path", "src/iocxml/bean.xml"));
		try {
			LOG.INFO("=======初始化Ioc容器=======");
			iocContainter.init();
		} catch (Exception e) {
			LOG.ERROR("IOC容器初始化失败！程序将会退出！");
			e.printStackTrace();
			System.exit(1);
		}
		LOG.INFO("Ioc容器初始化成功");

		// Controller 注册，基于注解
		packageScanner(CONFIGS.get("controller-base-package"), CONTROLLERS);
		controllerAnnotationRegister();
		LOG.INFO("业务逻辑处理器 解析成功");

		// Interceptor 注册，根据 XML 配置注册
		interceptorRegistration();

		// 服务器初始化
		// --- 选择使用何种类型的服务器
		boolean isMainSubReactor = Boolean.parseBoolean(CONFIGS.get("use-main-sub-reactor"));
		ReactorServer server;
		if(isMainSubReactor) {
			// 主从 Reactor 模型
			server = new MainSubReactorServer(Integer.parseInt(port), this, Integer.valueOf(CONFIGS.get("sub-reactor-count")));
		} else {
			// 单 Reactor 模型
			server = new SingleReactorServer(Integer.parseInt(port), this);
		}
		server.run();
	}
}