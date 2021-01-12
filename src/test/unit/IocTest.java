package test.unit;

import com.galaxyzeta.server.ioc.BeanDefinition;
import com.galaxyzeta.server.ioc.IocContainer;
import com.galaxyzeta.server.ioc.PropertyValue;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

import test.entity.Address;
import test.entity.CycleInjection;
import test.entity.CycleInjection2;
import test.entity.User;

/**
 * 测试 Ioc bean 注册，依赖注入，后置处理器是否正常工作
 */
public class IocTest {
	public static void main(String[] args) throws Exception {
		
		IocContainer context = new IocContainer("src/test/iocxml/bean.xml");
		context.init();
		
		// === 1st init ===
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
		logger.DEBUG("======== 1st init OK =======");

		BeanDefinition def = new BeanDefinition("newBean", Address.class.getName(), null);
		def.getProp().add(new PropertyValue("street", "Palm Drive", PropertyValue.Type.VALUE));
		def.getProp().add(new PropertyValue("city", "Neon City", PropertyValue.Type.VALUE));
		context.registerBeanDefinition("newBean", def);
		
		context.refresh();

		logger.DEBUG("======== 2nd init OK =======");

		logger.INFO(((CycleInjection)context.getBean("cycleInjection")).getCycleInjection().toString());
		logger.INFO(((CycleInjection2)context.getBean("cycleInjection2")).getCycleInjection().toString());

		logger.DEBUG("======== check cycle injection ok ========");

	}
}