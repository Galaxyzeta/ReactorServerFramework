package com.galaxyzeta.server.reactor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.galaxyzeta.entity.Router;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.parser.RequestParser;

import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;
import com.galaxyzeta.util.ResponseFactory;
import com.galaxyzeta.util.ViewResolver;

/**
 * <p>== 这个类非常重要！==</p>
 * <p>每个请求的的处理流程</p>
 * <p>[OP_READ] --> read() --> process():线程池 --> [OP_WRITE] --> send()</p>
 */
public class Handler implements Runnable {

	// 访问 ApplicationContext 的静态方法可以得到一些与配置有关的东西。

	// Epoll 工具
	private Selector channelSelector;
	private SocketChannel clientSocket;
	private SelectionKey sk;

	// 流程附带的 req, resp, 以及视图解析对象
	private HttpRequest req = null;
	private HttpResponse resp = new HttpResponse();
	private Object viewObject = null;

	// 线程池
	private static BlockingQueue<Runnable> processorQueue = new LinkedBlockingQueue<>();
	private static ExecutorService threadPool = new ThreadPoolExecutor(25, 200, 1, TimeUnit.SECONDS, processorQueue);

	// 状态定义 READ(主线程) -> PROCESS(线程池) -> SEND(主线程)
	private static final int READ = 1;
	private static final int SEND = 2;
	private int status;

	// LOGGER
	private static Logger LOG = LoggerFactory.getLogger(Handler.class);

	// Constructor
	public Handler(Selector channelSelector, SocketChannel clientSocket) throws IOException {
		this.channelSelector = channelSelector;
		this.clientSocket = clientSocket;
		this.clientSocket.configureBlocking(false);
		this.sk = clientSocket.register(channelSelector, SelectionKey.OP_READ);
		this.sk.attach(this);
		this.channelSelector.wakeup();
		status = READ;
	}

	// 1. 解析请求报文，并调用业务处理函数
	public void read() {
		try {
			// request 的 “反序列化”
			req = new RequestParser(clientSocket).parse();
			clientSocket.shutdownInput();
			//System.out.println(req);

			// 关掉输入，这样epoll模型就不能再次检测到输入就绪事件了
			// 直到线程池完成 process，将selection的interetOPs改为OP_WRITE，以便被epoll模型检测到事件可用。
			process();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	// 2. 业务处理，此处是最重的业务层，交给线程池处理减轻 handler 负担，以便快速 Epoll
	// 包含请求拦截与请求处理
	public void process() {

		// 当场包装一个 Runnable，扔给线程池
		class InnerRunnable implements Runnable {
			@Override
			public void run() {
				Router router = WebApplicationContext.searchRouter(req.getMethod(), req.getUrl());	// 搜索路由哈希表，O(1)
				boolean intercepted = false;
				try {
					// 1. 请求拦截
					ArrayList<Method> interceptorMethods = WebApplicationContext.getInterceptors();		// 取得拦截器列表
					for(Method singleInterceptMethod : interceptorMethods) {
						Boolean execResult = (Boolean) singleInterceptMethod.invoke(null, req, resp);		// 执行拦截器逻辑
						// 请求被拒绝
						if(execResult == false) {
							LOG.WARN("请求未能通过过滤器");
							intercepted = true;
							break;
						}
					}
					
					// 2. 请求处理
					if(intercepted == false) {
						if(router == null) {
							LOG.WARN(String.format("找不到正确处理此请求的Controller %s %s，它可能是资源路径", req.getMethod(), req.getUrl()));
							viewObject = req.getUrl();		// 找不到 Controller，先认为它是资源路径
						} else {
							viewObject = router.getHandlerMethod().invoke(null, req, resp);		// 否则，视图对象是 Controller 的返回值
						}
					}
					
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
				
				status = SEND;
				sk.interestOps(SelectionKey.OP_WRITE);
				channelSelector.wakeup();
			}
		}

		threadPool.execute(new InnerRunnable());
	}

	// 根据 ViewObject 解析视图，并发送响应报文，流程在此结束
	public void write() {
		try {
			// 视图解析部分，根据对象返回类型装配 response
			
			ViewResolver.resolve(viewObject, resp);

			ByteBuffer buffer = ByteBuffer.wrap(resp.toString().getBytes());
			clientSocket.write(buffer);
			clientSocket.shutdownOutput();
			sk.cancel();
			this.channelSelector.wakeup();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@Override
	public void run() {
		switch (status) {
			case READ: {
				read();
				break;
			}
			case SEND: {
				write();
				break;
			}
		}
	}
}