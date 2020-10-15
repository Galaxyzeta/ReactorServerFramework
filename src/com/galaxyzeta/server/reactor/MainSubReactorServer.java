package com.galaxyzeta.server.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

// 主从 Reactor 架构的服务器。
// 原理：
// 1. 主 Reactor 关注能否接收，若接收到请求，则 isAcceptible 事件可用，selector 选择发生事件的 channel，执行 dispatch 方法。
// 2. dispatch 方法用 round-robin 选择一个 SubReactor 与新创建的的 Acceptor 线程关联，线程内部调用 SubReactor 的 register 方法。
// 3. register 方法创建 handler，在 handler 内部包含线程池，对 read/write 事件进行处理。

public class MainSubReactorServer extends ReactorServer {

	private int port = 8080;
	private int subReactorCount = 1;
	private int roundRobin = 0;			// 轮询，负载均衡
	
	private static final int TIMEOUT = 3000;
	private static final Logger LOG = LoggerFactory.getLogger(MainSubReactorServer.class);

	private Selector acceptSelector = null;

	private ServerSocketChannel server = null;
	private List<SubReactor> subs = new ArrayList<>();
	private WebApplicationContext context;

	public MainSubReactorServer(int port, WebApplicationContext context) {
		this.port = port;
		this.context = context;
	}

	public MainSubReactorServer(int port, WebApplicationContext context, int subReactorCount) {
		this(port, context);
		this.subReactorCount = subs.size();
	}

	// === Sub Reactor ===

	private class SubReactor implements Runnable {
		
		private int no;
		private Selector subSelector = null;

		public SubReactor(int i) throws IOException {
			no = i;
			subSelector = Selector.open();
		}

		public void register(SocketChannel clientSocket) {
			LOG.INFO("进入 SubReactor #" + no);
			try {
				new Handler(subSelector, clientSocket, context);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		protected void dispatch(SelectionKey key) throws IOException {
			Runnable task = (Runnable) key.attachment();
			new Thread(task).start();
		}

		@Override
		public void run() {
			try {
				// 核心循环
				while (true) {
					if (subSelector.select(TIMEOUT) >= 1) {
						Iterator<SelectionKey> iter = subSelector.selectedKeys().iterator();
						while (iter.hasNext()) {
							SelectionKey key = iter.next();
							if(key.isValid()) {
								LOG.INFO("SubReactor, SelectionKey = " + key);
								dispatch(key);
							}
							iter.remove();
						}
					}
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	// === Acceptor ===

	private class Acceptor {
		
		public SubReactor sub;
		
		Acceptor(SubReactor sub) {
			this.sub = sub;
		}
		
		public void run() {
			try {
				SocketChannel clientSocket = server.accept();
				LOG.INFO("请求已被接受");
				if(clientSocket != null) {
					sub.register(clientSocket);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	/**
	 * 反应核初始化
	 */
	private void init() throws IOException {
		// 初始化 Selector
		acceptSelector = Selector.open();
		// 打开服务器接收通道
		server = ServerSocketChannel.open();
		server.bind(new InetSocketAddress(port));
		server.configureBlocking(false);	// 设置 IO 非阻塞
		server.register(acceptSelector, SelectionKey.OP_ACCEPT);	// 将 Channel 注册到 Selector 上，监听 Accept 事件
		// 添加 Subreactor，并让它们就绪
		for(int i=0; i<subReactorCount; i++) {
			SubReactor subReactor = new SubReactor(i);
			new Thread(subReactor).start();
			subs.add(subReactor);
		}
		LOG.INFO("添加了" + subReactorCount + "个 subReactor.");
	}

	@Override
	protected void dispatch(SelectionKey key) {
		// 启动接收器
		new Acceptor(subs.get(roundRobin)).run();
		roundRobin = ( roundRobin + 1 ) % subReactorCount;
	}

	@Override
	protected void run() {
		try {
			LOG.INFO("正在启动... ...");
			// 核心启动
			init();
			LOG.INFO(String.format("已在端口 %s 上启动服务.", port));
			// 核心循环
			while(true) {
				if(acceptSelector.select(TIMEOUT) >= 1) {
					Iterator<SelectionKey> iter = acceptSelector.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						if(key.isValid() && key.isAcceptable()) {
							LOG.INFO("Main Reactor, isAcceptible");
							dispatch(key);
						}
						iter.remove();
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
