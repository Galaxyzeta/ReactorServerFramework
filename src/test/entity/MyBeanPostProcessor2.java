package test.entity;

import com.galaxyzeta.server.ioc.BeanPostProcessor;

public class MyBeanPostProcessor2 implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
		System.out.println(String.format("Processor 2 - Before init - beanName = %s", beanName));
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
		System.out.println(String.format("Processor 2 - After init - beanName = %s", beanName));
		return bean;
	}
	
}