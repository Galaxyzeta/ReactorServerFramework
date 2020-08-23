package com.galaxyzeta.server.ioc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class IocContainer {
	private final ConcurrentHashMap<String, BeanDefinition> registry = new ConcurrentHashMap<>();
	private final ArrayList<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

	private String xmlPath;
	private final Object singletonLock = new Object();
	
	
	// constructor
	public IocContainer(String xmlPath) {
		this.xmlPath = xmlPath;
	}

	/**
	 * 注册 BeanDefinition
	 * @param name
	 * @param beanDefinition
	 */
	public void registerBeanDefinition(String name, BeanDefinition beanDefinition) {
		this.registry.put(name, beanDefinition);
	}

	/**
	 * 注册 Bean 后置处理器
	 * @param beanPostProcessor
	 */
	private void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		this.beanPostProcessors.add(beanPostProcessor);
	}

	private void registerBeanPostProcessors() throws Exception {
		List<Object> list = getBeansByType(BeanPostProcessor.class);
		for(Object processor: list) {
			addBeanPostProcessor((BeanPostProcessor)processor);
		}
	}

	public List<Object> getBeansByType (Class<?> clazz) {
		ArrayList<Object> resList = new ArrayList<>();
		for(Iterator<Entry<String, BeanDefinition>> i = registry.entrySet().iterator(); i.hasNext();) {
			Entry<String, BeanDefinition> entry = i.next();
			BeanDefinition beanDefinition = entry.getValue();
			String beanClassName = beanDefinition.getClassname();
			try {
				if(clazz.isAssignableFrom(Class.forName(beanClassName))) {
					doCreateBean(beanDefinition);
					resList.add(beanDefinition.getBean());
				}
			} catch (Exception e) {

			}
		}
		return resList;
	}

	/**
	 * 容器初始化，制造bean并执行依赖注入
	 * @throws Exception
	 */
	public void init() throws Exception {
		new XMLBeanDefinitionParser(this, xmlPath).parse();
		registerBeanPostProcessors();
		createBean();
	}

	private void createBean() throws Exception {
		for(Iterator<Entry<String, BeanDefinition>> i = registry.entrySet().iterator(); i.hasNext();) {
			Entry<String, BeanDefinition> entry = i.next();
			BeanDefinition beanDefinition = entry.getValue();
			doCreateBean(beanDefinition);
		}
	}

	private void doCreateBean(BeanDefinition beanDefinition) throws Exception {
		String beanName = beanDefinition.getName();
		preInitSingleton(beanDefinition);
		initializeBean(beanDefinition.getBean(), beanName);
	}

	
	private void preInitSingleton(BeanDefinition beanDefinition) throws Exception {
		Object bean = beanDefinition.getBean();
		// bean 未实例化 双重校验锁新建bean
		compareAndSetBean(bean, null, beanDefinition);
		bean = beanDefinition.getBean();

		// 注入 bean 的依赖
		for(PropertyValue prop : beanDefinition.getProp()) {
			dependencyInjection(bean, prop);
		}
	}

	private void initializeBean(Object bean, String beanName) throws Exception {
		// 1. 初始化之前
		for(BeanPostProcessor processor: beanPostProcessors) {
			bean = processor.postProcessBeforeInitialization(bean, beanName);
		}

		// 2. 初始化
		String initMethod = registry.get(beanName).getInitMethod();
		if(! initMethod.equals("")) {
			bean.getClass().getMethod(initMethod).invoke(bean);
		}

		// 3. 初始化之后
		for(BeanPostProcessor processor: beanPostProcessors) {
			bean = processor.postProcessAfterInitialization(bean, beanName);
		}
	}

	/**
	 * 线程安全的 bean 设值
	 * @param bean
	 * @param expected
	 * @param beanDefinition
	 * @throws Exception
	 */
	private void compareAndSetBean(Object bean, Object expected, BeanDefinition beanDefinition) throws Exception {
		if(bean == expected) {
			synchronized(singletonLock) {
				if(bean == expected) {
					beanDefinition.setBean(instantiateBean(beanDefinition));
				}
			}
		}
	}

	/**
	 * 依赖注入
	 * @param target
	 * @param prop
	 * @throws Exception
	 */
	private void dependencyInjection(Object target, PropertyValue prop) throws Exception {
		String name = prop.getName();
		Object value = prop.getValue();
		// 如果是 ref，则在此处 ref 表示依赖的名字
		// 需要将其转换成依赖对象
		if(prop.getType() == PropertyValue.Type.REF) {
			// 从 registry 找到依赖的名字
			BeanDefinition dependency = registry.get(value);
			if(dependency == null) {
				throw new IllegalArgumentException("ref 名不存在");
			} else {
				// 检查是否存在此bean，如果存在，则直接赋值，否则注入新的对象
				compareAndSetBean(value, null, dependency);
				value = dependency.getBean();
			}
		}

		try {
			// 寻找setter方法
			String methodName = "set" + name.substring(0,1).toUpperCase() + name.substring(1);
			Method method = target.getClass().getDeclaredMethod(methodName, value.getClass());
			method.invoke(target, value);
		} catch (NoSuchMethodException e) {
			// 否则强行field注入
			target.getClass().getField(name).set(target, value);
		}
	}

	/**
	 * 初始化一个bean，但不进行依赖注入
	 * @param beanDefinition
	 * @return
	 * @throws Exception
	 */
	private Object instantiateBean(BeanDefinition beanDefinition) throws Exception {
		return Class.forName(beanDefinition.getClassname()).getDeclaredConstructor().newInstance();
	}

	/**
	 * 获得容器内部的bean，若不存在，返回null
	 * @param name
	 * @return
	 */
	public Object getBean(String name) {
		BeanDefinition beanDefinition = registry.get(name);
		// 不存在此bean定义
		if(beanDefinition == null) {
			return null;
		}
		Object bean = beanDefinition.getBean();
		// bean还未实例化
		if(bean == null) {
			try {
				doCreateBean(beanDefinition);
			} catch(Exception e) {
				return null;
			}
		}
		return beanDefinition.getBean();
	}

	public void refresh() throws Exception {
		
	}
}