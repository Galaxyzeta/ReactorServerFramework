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

public class MainSubReactorServer extends ReactorServer implements Runnable {

	private int port = 8080;
	private int subReactorCount = 1;
	private int roundRobin = 0;			// 轮询，负载均衡
	
	private static final int TIMEOUT = 30000;	// select 轮询阻塞 30s 不然太耗资源
	private static final Logger LOG = LoggerFactory.getLogger(MainSubReactorServer.class);

	private Selector acceptorSelector = null;

	private ServerSocketChannel serverSocket = null;
	private List<SubReactor> subs = new ArrayList<>();
 	private WebApplicationContext context;

	public MainSubReactorServer(int port) {
		this.port = port;
	}

	public MainSubReactorServer(int port, int subReactorCount) {
		this(port);
		this.subReactorCount = subReactorCount;
	}

	/** 从 Reactor */
	private class SubReactor implements Runnable {
		
		private int no;
		private Selector subSelector = null;

		public SubReactor(int i) throws IOException {
			no = i;
			subSelector = Selector.open();
		}

		/** 将 clientSocket 注册到当前 subReactor 的 selector 上 */
		public void register(SocketChannel clientSocket) {
			LOG.INFO("将clientSocket注册到 SubReactor #" + no);
			try {
				new Handler(subSelector, clientSocket);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		/** 将被选择的 key 扔给 Handler */
		protected final void dispatch(SelectionKey key) throws IOException {
			Handler task = (Handler) key.attachment();
			task.execute();
		}

		@Override
		/** SubReactor 主循环 */
		public void run() {
			try {
				while (true) {
					if (subSelector.select(TIMEOUT) > 0) {
						// 有被选择的 Key
						Iterator<SelectionKey> iter = subSelector.selectedKeys().iterator();
						while (iter.hasNext()) {
							SelectionKey key = iter.next();
							iter.remove();

							if(key.isValid()) {
								LOG.INFO("Selector 轮询：SubReactor #" + no + ", SelectionKey = " + key.hashCode() + " INTEREST = " + key.interestOps());
								dispatch(key);
							}
						}
					}
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	/** 初始化，创建所有 Main Reactor 和 Sub Reactor */
	private void init() throws IOException {
		// 初始化 Acceptor 的 Selector
		acceptorSelector = Selector.open();
		// 打开服务器接收通道
		serverSocket = ServerSocketChannel.open();
		serverSocket.bind(new InetSocketAddress(port));
		serverSocket.configureBlocking(false);	// 设置 IO 非阻塞
		serverSocket.register(acceptorSelector, SelectionKey.OP_ACCEPT);	// 将 Channel 注册到 Selector 上，监听 Accept 事件
		// 添加 Subreactor，并让它们运行起来
		for(int i=0; i<subReactorCount; i++) {
			SubReactor subReactor = new SubReactor(i);
			new Thread(subReactor).start();
			subs.add(subReactor);
		}
		LOG.INFO("添加了" + subReactorCount + "个 subReactor.");
	}

	/** 接收一个请求 */	
	public final void accept(SubReactor sub) {
		try {
			SocketChannel clientSocket = serverSocket.accept();
			LOG.INFO(String.format("请求已被接受，socket信息：%s <=> %s", 
				clientSocket.getLocalAddress(), clientSocket.getRemoteAddress()));
			if(clientSocket != null) {
				sub.register(clientSocket);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@Override
	/** Acceptor 主线程 */
	public void run() {
		try {
			LOG.INFO("正在启动... ...");
			// 核心启动
			init();
			LOG.INFO(String.format("已在端口 %s 上启动服务.", port));
			// 核心循环
			while(true) {
				if(acceptorSelector.select(TIMEOUT) >= 1) {
					// 检测到可以 accept 一个请求
					Iterator<SelectionKey> iter = acceptorSelector.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						iter.remove();
						if(key.isValid() && key.isAcceptable()) {
							LOG.INFO("主 Reactor 检测到请求可以被接收");
							// 启动接收器
							accept(subs.get(roundRobin));
							roundRobin = ( roundRobin + 1 ) % subReactorCount;
							roundRobin = roundRobin > 0? roundRobin : 0;
						}
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
