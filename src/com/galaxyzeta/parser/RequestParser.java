package com.galaxyzeta.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import com.galaxyzeta.exceptions.IllegalRequestException;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

public class RequestParser {

	private BufferedReader reader;
	private SocketChannel clientSocket;

	private static final int IO_PARSER = 1;
	private static final int NIO_PARSER = 2;
	private int parserMode;
	private static Logger LOG = LoggerFactory.getLogger(RequestParser.class);

	public RequestParser(InputStream istream) {
		this.reader = new BufferedReader(new InputStreamReader(istream));
		this.parserMode = IO_PARSER;
	}

	public RequestParser(SocketChannel channel) {
		this.clientSocket = channel;
		this.parserMode = NIO_PARSER;
	}

	public HttpRequest parse() throws IOException, IllegalRequestException {
		if(parserMode == IO_PARSER) {
			return IOparser();
		} else if (parserMode == NIO_PARSER) {
			return NIOParser();
		}
		return null;
	}

	private HttpRequest IOparser() throws IOException, IllegalRequestException {
		String line;
		HttpRequest req = new HttpRequest();
		HashMap<String, String> requestHeaders = new HashMap<>();
		// 1. 处理请求报文头部第一行
		line = reader.readLine();
		String[] stringArr = line.split("\s");
		if(stringArr.length != 3) {
			throw new IllegalRequestException("请求头部第一行必须长度为3");
		} else {
			req.setMethod(stringArr[0]);
			req.setUrl(stringArr[1]);
			req.setVersion(stringArr[2]);
		}

		// 2. 读取请求头部参数
		while((line = reader.readLine()) != null && ! line.trim().equals("")) {
			stringArr = line.trim().split("(?=.*): ");
			if(stringArr.length != 2) {
				throw new IllegalRequestException("请求头部格式错误");
			}
			requestHeaders.put(stringArr[0].trim(), stringArr[1].trim());
		}
		req.setHeaders(requestHeaders);

		// 3. 读取请求体
		StringBuilder sb = new StringBuilder();
		while(reader.ready() && (line = reader.readLine()) != null) {
			sb.append(line);
		}
		req.setBody(sb.toString());

		LOG.INFO("请求解析完毕 " + req.getMethod() +" "+ req.getUrl());
		return req;
	}

	private HttpRequest NIOParser() throws IOException{
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		StringBuffer sb = new StringBuffer();
		// 读取 buffer -> stringBuilder
		int bts;
		while ((bts = clientSocket.read(buffer)) > 0) {
			buffer.flip();
			while(buffer.hasRemaining()) {
				sb.append((char)buffer.get());
			}
			buffer.clear();
		}
		if(bts == 0) {
			// 数据已经全部接收
			LOG.INFO("Request读取完成");
		} else {
			// 连接被中止，外部进行处理
			LOG.WARN("request解析过程，没有多余内容，准备shutdown...");
			return null;
		}

		String target = sb.toString();
		if(target != null && !target.trim().equals("")) {
			this.reader = new BufferedReader(new StringReader(target));
			return IOparser();
		} else {
			return null;
		}
	}
}