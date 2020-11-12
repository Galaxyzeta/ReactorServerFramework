package com.galaxyzeta.server.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

public final class SingleReactorServer extends ReactorServer {

	private int port = 8080;
	private static final int TIMEOUT = 3000;
	private static final Logger LOG = LoggerFactory.getLogger(SingleReactorServer.class);

	private Selector channelSelector = null;
	private ServerSocketChannel server = null;

	private WebApplicationContext context;

	public SingleReactorServer(int port, WebApplicationContext context) {
		this.port = port;
		this.context = context;
	}

	private class Acceptor implements Runnable {
		@Override
		public void run() {
			try {
				SocketChannel clientSocket = server.accept();
				LOG.INFO("请求已被接受");
				if(clientSocket != null) {
					new Handler(channelSelector, clientSocket, context);
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
		channelSelector = Selector.open();
		server = ServerSocketChannel.open();
		server.bind(new InetSocketAddress(port));
		server.configureBlocking(false);	// 设置 IO 非阻塞
		SelectionKey sk = server.register(channelSelector, SelectionKey.OP_ACCEPT);		// 将 Channel 注册到 Selector 上，监听 Accept 事件
		sk.attach(new Acceptor());
	}

	/**
	 * 根据Selector选择结果进行分发
	 * 耗时计算分发给线程池处理，避免拖慢反应核速度
	 */
	protected void dispatch(SelectionKey key) throws IOException {
		Runnable task = (Runnable) key.attachment();
		new Thread(task).run();
	}

	/*
		Reactor 模型主要内容
	*/
	public void run() {
		try {
			LOG.INFO("正在启动... ...");
			// 核心启动
			init();
			LOG.INFO(String.format("已在端口 %s 上启动服务.", port));
			// 核心循环
			while(true) {
				if(channelSelector.select(TIMEOUT) >= 1) {
					Iterator<SelectionKey> iter = channelSelector.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						dispatch(key);
						iter.remove();
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}