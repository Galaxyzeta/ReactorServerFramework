package com.galaxyzeta.server.ioc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class IocContainer {
	private final HashMap<String, BeanDefinition> registry = new HashMap<>();
	private final ArrayList<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
	private String xmlPath;
	
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
	 * 添加 Bean 后置处理器
	 * @param beanPostProcessor
	 */
	private void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		this.beanPostProcessors.add(beanPostProcessor);
	}

	/**
	 * 注册后置处理器
	 */
	private void registerBeanPostProcessors() throws Exception {
		List<Object> list = getBeansByType(BeanPostProcessor.class);
		for(Object processor: list) {
			addBeanPostProcessor((BeanPostProcessor)processor);
		}
	}

	/**
	 * 根据类找 bean
	 */
	public List<Object> getBeansByType(Class<?> clazz) {
		ArrayList<Object> resList = new ArrayList<>();
		for(Iterator<Entry<String, BeanDefinition>> i = registry.entrySet().iterator(); i.hasNext();) {
			Entry<String, BeanDefinition> entry = i.next();
			BeanDefinition beanDefinition = entry.getValue();
			String beanClassName = beanDefinition.getClassname();
			try {
				if(clazz.isAssignableFrom(Class.forName(beanClassName))) {
					resList.add(getBean(beanDefinition.getName()));
				}
			} catch (Exception e) {}
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
		refresh();
	}

	/**
	 * 进行 bean 的创建、属性注入
	 */
	private void createAndPopulateBean(BeanDefinition beanDefinition) {
		Object bean = beanDefinition.getBean();
		// bean 未实例化 双重校验锁新建bean
		setBean(bean, beanDefinition);
		bean = beanDefinition.getBean();

		// 注入 bean 的依赖
		populateBean(beanDefinition);
	}

	/**
	 * 对一个 bean 注入全部依赖
	 * @param beanDefinition
	 */
	private void populateBean(BeanDefinition beanDefinition) {
		// 注入 bean 的依赖
		for(PropertyValue prop : beanDefinition.getProp()) {
			dependencyInjection(beanDefinition.getBean(), prop);
		}
		beanDefinition.setStatus(Status.POPULATED);
	}

	/**
	 * 调用后置处理器，对 bean 进行前置初始化，初始化，后置初始化
	 */
	private void initializeBean(BeanDefinition beanDefinition, String beanName) throws Exception {
		
		Object bean = beanDefinition.getBean();
		// 1. 初始化之前
		for(BeanPostProcessor processor: beanPostProcessors) {
			bean = processor.postProcessBeforeInitialization(bean, beanName);
		}

		// 2. 初始化
		String initMethod = registry.get(beanName).getInitMethod();
		if(initMethod != null && ! initMethod.equals("")) {
			bean.getClass().getMethod(initMethod).invoke(bean);
		}

		// 3. 初始化之后
		for(BeanPostProcessor processor: beanPostProcessors) {
			bean = processor.postProcessAfterInitialization(bean, beanName);
		}

		beanDefinition.setStatus(Status.INITIALIZED);

	}

	/**
	 * 线程安全的 bean 设值
	 * @param bean
	 * @param expected
	 * @param beanDefinition
	 * @throws Exception
	 */
	private void setBean(Object bean, BeanDefinition beanDefinition) {
		beanDefinition.setBean(instantiateBean(beanDefinition));
	}

	/**
	 * 依赖注入
	 * @param target
	 * @param prop
	 * @throws Exception
	 */
	private void dependencyInjection(Object target, PropertyValue prop) {
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
				setBean(value = dependency.getBean(), dependency);
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
			try {
				target.getClass().getField(name).set(target, value);
			} catch (Exception e1) {
				critical(e1);
			}
		} catch (Exception e) {
			critical(e);
		}
	}

	/**
	 * 创建一个bean，但不进行依赖注入
	 * @param beanDefinition
	 * @return
	 * @throws Exception
	 */
	private Object instantiateBean(BeanDefinition beanDefinition) {
		try {
			Object ret = Class.forName(beanDefinition.getClassname()).getDeclaredConstructor().newInstance();
			beanDefinition.setStatus(Status.CREATED);
			return ret;
		} catch(Exception e) {
			critical(e);
		}
		return null;
	}

	/**
	 * 发生了严重错误，不能继续执行，程序立即退出
	 */
	private void critical(Exception e) {
		System.out.println("[! CRITICAL !] Bean instantiation failed! Exception is " + e);
		e.printStackTrace();
		System.exit(1);
	}

	/**
	 * 获得容器内部的bean，若不存在bean定义，返回null。若 bean 未实例化，实例化并执行依赖注入之后返回。
	 * @param name
	 * @return
	 */
	public Object getBean(String name) {
		BeanDefinition beanDefinition = registry.get(name);
		// 不存在此bean定义
		if(beanDefinition == null) {
			critical(new RuntimeException("beanDefinition不存在!"));
		}
		// bean还未实例化
		if(beanDefinition.getStatus() == Status.NULL) {
			createAndPopulateBean(beanDefinition);
		} else if (beanDefinition.getStatus() == Status.CREATED) {
			populateBean(beanDefinition);
		}
		return beanDefinition.getBean();
	}

	/**
	 * 所有未初始化的 bean 调用初始化方法
	 */
	private void initialize() throws Exception {
		for(Iterator<Entry<String, BeanDefinition>> i = registry.entrySet().iterator(); i.hasNext();) {
			Entry<String, BeanDefinition> entry = i.next();
			BeanDefinition beanDefinition = entry.getValue();
			if(beanDefinition.getStatus() == Status.POPULATED) {
				initializeBean(beanDefinition, beanDefinition.getName());
			}
		}
	}

	/**
	 * 试图根据现有的 beanDefinition 创建全部的 bean，若 bean 已经存在，则不用再创建
	 */
	public void refresh() throws Exception {
		for(Iterator<Entry<String, BeanDefinition>> i = registry.entrySet().iterator(); i.hasNext();) {
			Entry<String, BeanDefinition> entry = i.next();
			BeanDefinition beanDefinition = entry.getValue();
			getBean(beanDefinition.getName());
		}
		initialize();
	}
}