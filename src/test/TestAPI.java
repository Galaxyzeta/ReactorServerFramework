package test;

import com.galaxyzeta.server.reactor.WebApplicationContext;

public class TestAPI {
	public static void main(String[] args) {
		// "D:/--FILE DATA--/VSWorkSpace/JavaServer/config/config.property"
		WebApplicationContext.runApplication("src/test/config.property");
	}
}