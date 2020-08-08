package com.galaxyzeta.server.deprecated;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

import com.galaxyzeta.http.HttpRequest;

// 同步 服务端，曾用于发送高频请求
// 除了 Debug 请不要使用 
@Deprecated
public class WebClient {

	private static Consumer<String> DEBUG = new Consumer<>() {
		@Override
		public void accept(String in) {
			System.out.println("[DEBUG] " + in);
		}
	};
	
	public static void main(String[] args) {
		for(int i=0; i<1; i++) {
			
			try {
				Socket socket = new Socket("localhost", 8080);
				PrintWriter writer = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()));
	
				HttpRequest req = new HttpRequest("POST", "/asd", null, "Hello, this is client");
	
				DEBUG.accept(req.toString());
	
				writer.write(req.toString());
				writer.flush();
				socket.shutdownOutput();
	
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	
	
				while(reader.ready() && (line = reader.readLine()) != null) {
					DEBUG.accept(line);
				}
	
				socket.close();
	
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}