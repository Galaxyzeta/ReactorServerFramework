package test.unit;

import com.galaxyzeta.server.ioc.IocContainer;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

import test.entity.Address;
import test.entity.User;

/**
 * 测试 Ioc bean 注册，依赖注入，后置处理器是否正常工作
 */
public class DefinitionRegisterTest {
	public static void main(String[] args) throws Exception {
		
		IocContainer context = new IocContainer("src/test/bean.xml");
		context.init();
		LoggerFactory loggerFactory = (LoggerFactory)context.getBean("loggerfactory");
		Logger logger = loggerFactory.getLogger();
		logger.DEBUG("Instanitiation is working...");
		
		Address tomAddress = (Address)context.getBean("tomAddress");
		logger.DEBUG(tomAddress.getCity() + "|" + tomAddress.getStreet());
		logger.DEBUG("Normal value injection is OK...");

		User tom = (User)context.getBean("tom");
		logger.DEBUG(tom.getPassword() + "|" + tom.getUsername());
		
		if(tom.getAddress() == tomAddress) {
			logger.DEBUG("Ref injection is OK...");
		}
		
	}
}