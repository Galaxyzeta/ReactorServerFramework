package com.galaxyzeta.server.reactor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import com.galaxyzeta.entity.Router;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.parser.RequestParser;
import com.galaxyzeta.util.Constant;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;
import com.galaxyzeta.parser.ViewResolver;

/**
 * 负责业务逻辑的处理。
 * 每当 Selector 表明事件发生，同步调用 Handler 的 Execute 方法进行处理。
 * 处理时扔给线程池，减轻 SubReactor 的负担。
 */
public class Handler {

	// 访问 ApplicationContext 的静态方法可以得到一些与配置有关的东西。
	private WebApplicationContext context;

	// 单例 viewResolver, @autowire
	private ViewResolver viewResolver;

	// 多路复用相关对象
	private Selector channelSelector;
	private SocketChannel clientSocket;
	private SelectionKey sk;

	// 流程附带的 req, resp, 以及视图解析对象
	private HttpRequest req = null;
	private HttpResponse resp = new HttpResponse();
	private Object viewObject = null;

	// 状态定义 READ(主线程) -> PROCESS(线程池) -> SEND(主线程)
	private static final int READ = 1;
	private static final int SEND = 2;
	private static final int BUSY = 4;
	private int status;

	// LOGGER
	private static Logger LOG = LoggerFactory.getLogger(Handler.class);

	/** 设置 Ioc 上下文，设置视图解析器，绑定 Selector 与 SocketChannel，将自身作为附加物加到 SelectionKey 上 */
	public Handler(Selector channelSelector, SocketChannel clientSocket, WebApplicationContext context) throws IOException {
		this.context = context;
		this.viewResolver = (ViewResolver)context.getIocContainer().getBean("viewResolver");
		this.viewResolver.setContext(context);

		this.channelSelector = channelSelector;
		this.clientSocket = clientSocket;
		this.clientSocket.configureBlocking(false);
		
		this.sk = clientSocket.register(channelSelector, SelectionKey.OP_READ);
		this.sk.attach(this);
		status = READ;
		this.channelSelector.wakeup();
	}

	/** 处理读取，交给线程池处理 */
	private void processRead() {
		// threadPool.execute(()->{
			try {
				// === 请求解析 ===
				req = new RequestParser(clientSocket).parse();
				if(req == null) {
					// -1 作为结束
					LOG.WARN("连接断开");
					shutdownConnection();
					return;
				}

				// === 设置 keepAlive ===
				if(req.getHeader("Connection") != null) {
					clientSocket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
				}

				// === 查找路由 ===
				Router router = context.searchRouter(req.getMethod(), req.getUrl());
				boolean intercepted = false;
			
				// === 调用拦截 ===
				ArrayList<Method> interceptorMethods = context.getInterceptors();		// 取得拦截器列表
				for(Method singleInterceptMethod : interceptorMethods) {
					Boolean execResult = (Boolean) singleInterceptMethod.invoke(null, req, resp);		// 执行拦截器逻辑
					// 请求被拒绝
					if(execResult == false) {
						LOG.WARN("请求未能通过过滤器");
						intercepted = true;
						break;
					}
				}
				
				// === 视图对象赋值 ===
				if(intercepted == false) {
					if(router == null) {
						LOG.WARN(String.format("找不到正确处理此请求的Controller %s %s，它可能是资源路径", req.getMethod(), req.getUrl()));
						viewObject = req.getUrl();		// 找不到 Controller，先认为它是资源路径
					} else {
						viewObject = router.getHandlerMethod().invoke(null, req, resp);		// 否则，视图对象是 Controller 的返回值
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 设置准备发送
			setSend();

		// });
	}

	/** 处理写回 */
	private void processWrite() {
		// threadPool.execute(()->{
			try {
				// 视图解析部分，根据对象返回类型装配 response
				viewResolver.resolve(viewObject, resp);

				boolean keepAlive = context.getConfig(Constant.CONNECTION_KEEP_ALIVE).equals("true");
				resp.addResponseHeader("Content-Length", Integer.toString(resp.getResponseBody().length()));
				if(keepAlive) {
					resp.addResponseHeader("Connection", "Keep-Alive");
					resp.addResponseHeader("Keep-Alive", String.format("timeout=%d, max=%d", 
						Integer.parseInt(context.getConfig(Constant.KEEP_ALIVE_TIMEOUT)),
						Integer.parseInt(context.getConfig(Constant.KEEP_ALIVE_MAX))
					));
				}

				ByteBuffer buffer = ByteBuffer.wrap(resp.toString().getBytes());
				clientSocket.write(buffer);
				
				// 此处若直接断开C/S连接，则变为短连接
				if(!keepAlive ) {
					clientSocket.shutdownOutput();
					sk.cancel();
				} else {
					sk.interestOps(SelectionKey.OP_READ);
					status = READ;
					channelSelector.wakeup();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				sk.cancel();
			}
		// });
	}

	/** 设置自身状态为 SEND，并修改 SelectionKey 的监听事件 */
	private void setSend() {
		status = SEND;
		sk.interestOps(SelectionKey.OP_WRITE);
		channelSelector.wakeup();
	}

	/** 根据 ViewObject 解析视图，并发送响应。异步处理 */
	private void write() {
		status = BUSY;
		processWrite();
	}

	/** 处理读取的业务逻辑。异步处理 */
	private void read() {
		status = BUSY;
		processRead();
	}

	/** 关闭连接 */
	private void shutdownConnection() throws IOException {
		sk.cancel();
		clientSocket.close();
	}

	/** 每当外部有 Selector 报告事件发生，调用 execute 执行流程 */
	public void execute() {
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