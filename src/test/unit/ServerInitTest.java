package test.unit;

import com.galaxyzeta.server.reactor.WebApplicationContext;
import com.galaxyzeta.util.Logger;

/**
 * 测试服务器加载过程是否有效
 */
public class ServerInitTest {
	public static void main(String[] args) {
		// "D:/--FILE DATA--/VSWorkSpace/JavaServer/config/config.property"
		Logger.disabled = false;
		new WebApplicationContext().runApplication("src/test/config.property");
	}
}