package com.galaxyzeta.server.deprecated;

import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.parser.RequestParser;

import java.io.PrintWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;


// 阻塞 IO 服务器
// 单线程 Acceptor
// 业务逻辑线程池处理

@Deprecated
public class BlockingIOServer {

	private static final int PORT = 8080;	// 端口号
	private static final int TIMEOUT = 3000;
	private static final int MAX_REJECTION_BEFORE_SHUTDOWN = 10;	// 压测服务器宕机模拟

	private static AtomicLong rejectedCount = new AtomicLong(0);	// 已经被拒绝的task数

	private static BlockingQueue<Runnable> bQueue = new LinkedBlockingQueue<Runnable>(100);			// 核心线程池将要溢出时，任务开始排队，阻塞队列再溢出，线程池新增线程来处理。

	private static RejectedExecutionHandler rejectionHandler = new RejectedExecutionHandler(){		// 处理服务拒绝
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			DEBUG.accept("Rejected! Current Rejection count = " + rejectedCount.getAndIncrement());
			if(rejectedCount.get() > MAX_REJECTION_BEFORE_SHUTDOWN) {
				DEBUG.accept("Load test failed. Shutting down...");
				executor.shutdownNow();
				System.exit(1);
			}
		}
	};

	private static ExecutorService threadPool = new ThreadPoolExecutor(25, 200, 1, TimeUnit.SECONDS, bQueue, rejectionHandler) {	// 线程池 logger 重载
		@Override
		public void beforeExecute(Thread t, Runnable r) {
			//DEBUG.accept("Task Count=" + this.getActiveCount() + " Waiting Numbers=" + this.getQueue().size());
		}
	};

	private static Consumer<String> DEBUG = new Consumer<>() {		//包装一个 debug logger
		@Override
		public void accept(String in) {
			System.out.println("[DEBUG] " + in);
		}
	};

	private static void blockingIOServer() {
		/*
			阻塞 IO 模型
			Acceptor 主线程接收 socket
			接收完毕封装一个 runnable 交给线程池执行
		*/
		ServerSocket server;
		try {
			server = new ServerSocket(PORT);
			//DEBUG.accept("Server running at " + PORT);
			while (true) {
				// 阻塞等待客户连接，连上后得到它的 IO 组件
				Socket clientSocket = server.accept();

				// DEBUG.accept("Accepted");

				Thread task = new Thread() {
					@Override
					public void run() {
						try {
							InputStream istream = clientSocket.getInputStream();
							PrintWriter writer = new PrintWriter(
									new BufferedOutputStream(clientSocket.getOutputStream()));

							// 读取请求报文
							HttpRequest request = new RequestParser(istream).parse();
							// DEBUG.accept("Received");
							// DEBUG.accept(request.toString());

							// 发送 http 响应报文
							HttpResponse resp = new HttpResponse();
							resp.setStatusCode("200");
							resp.setStatusDescription("OK");
							resp.setResponseBody("<h1>Connection OK</h1>");
							// DEBUG.accept(resp.toString());

							writer.println(resp.toString());
							writer.flush();
							writer.close();
						} catch (SocketException se) {
							//DEBUG.accept("Connection Reset ...");
						} catch (IOException ioe) {
							ioe.printStackTrace();
						} 
					}
				};

				threadPool.execute(task);
			}
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
	}

	public static void main(String[] args) {
		blockingIOServer();
	}
}