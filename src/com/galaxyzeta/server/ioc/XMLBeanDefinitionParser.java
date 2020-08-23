package com.galaxyzeta.server.ioc;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLBeanDefinitionParser {
	private String fileName;
	private IocContainer context;

	XMLBeanDefinitionParser(IocContainer context, String fileName) {
		this.fileName = fileName;
		this.context = context;
	}

	public void parse() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xml = builder.parse(this.fileName);
		Element root = xml.getDocumentElement();
		registerBeanDefinitions(root);

	}

	private void registerBeanDefinitions(Element root) {
		NodeList childs = root.getChildNodes();
		int length = childs.getLength();
		for(int i=0; i<length; i++) {
			Node node = childs.item(i);
			if(node instanceof Element) {
				Element tag = (Element)node;
				if (tag.getTagName().equals("bean")) {
					// 读取基本信息 -- name, classname, init-method, destroy
					String name = tag.getAttribute("name");
					String classname = tag.getAttribute("class");
					String initMethod = tag.getAttribute("init-method");
					BeanDefinition beanDefinition = new BeanDefinition(name, classname, initMethod);
					// 试图读取 bean 结点下的子结点 property
					registerProperty(tag.getChildNodes(), beanDefinition);
					// 注册 beanDefinition 到 registry
					context.registerBeanDefinition(name, beanDefinition);
				}
			}
		}
	}

	private void registerProperty(NodeList propNodes, BeanDefinition definitionRef) {
		int propLength = propNodes.getLength();
		for(int j=0; j<propLength; j++) {
			Node prop = propNodes.item(j);
			if(prop instanceof Element) {
				Element tag = (Element) prop;
				String tagname = tag.getTagName();
				if(tagname.equals("property")) {
					doRegisterProperty(tag, definitionRef);
				}
			}
		}
	}

	private void doRegisterProperty(Element prop, BeanDefinition definition) {
		String name = prop.getAttribute("name");
		String value = prop.getAttribute("value");
		String ref = prop.getAttribute("ref");
		
		if(!value.equals("")) {
			definition.getProp().add(new PropertyValue(name, value, PropertyValue.Type.VALUE));
		} else if (!ref.equals("")) {
			definition.getProp().add(new PropertyValue(name, ref, PropertyValue.Type.REF));
		} else {
			throw new IllegalArgumentException("[Error] Xml 存在问题，property下必须有value / ref！");
		}
	}
}