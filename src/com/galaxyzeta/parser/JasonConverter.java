package com.galaxyzeta.parser;

import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Set;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;

import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

public class JasonConverter {

	private static Logger LOG = LoggerFactory.getLogger(JasonConverter.class);

	/** 根据传参类型进行 json 转化 */
	public static String convert(Object obj) {
		try {
			if(obj == null) {
				return "null";
			} else if(judgeSimpleType(obj)) {
				// 基本类型，直接添加
				return simpleConvert(obj);
			} else if(obj instanceof List || obj instanceof Set) {
				// 迭代类型，递归添加
				return iterableConvert((Iterable<?>)obj);
			} else if (obj.getClass().isArray()) {
				return arrayConvert(obj);
			} else if(obj instanceof Map) {
				return mapConvert((Map<?,?>)obj);
			} else {
				// 对象，递归，且需要解析所有属性
				return objectConvert(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// === 具体转换方法 ====

	/** 将一个对象完整解析后生成 json  */
	private static String objectConvert(Object obj) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean first = true;
		Class<?> cls = obj.getClass();
		Field[] fields = cls.getFields();
		try {
			for(Field fd : fields) {

				// 正确添加逗号
				if(!first) sb.append(",");
				else first = !first;

				sb.append(String.format("\"%s\": %s", fd.getName(), convert(fd.get(obj))));
			}
			return sb.append("}").toString();
		} catch(Exception e) {
			return null;
		}
	}

	/** 判断是否属于简单类型 */
	private final static boolean judgeSimpleType(Object obj) {
		if(obj instanceof Number || obj instanceof String) {
			return true;
		} else {
			return false;
		}
	}

	private final static boolean isPrimitiveArray(Object arr) {
		if(arr instanceof Object[]) return false;
		return true;
	}
	/** 列表、集合转换 */
	private static String iterableConvert(Iterable<?> iterable) throws Exception {
		List<String> res = new LinkedList<>();
		for(Object item : iterable) {
			res.add(convert(item));
		}
		return res.toString();
	}

	/** 数组转化的 facade 方法 */
	private static String arrayConvert(Object arr) {
		if(isPrimitiveArray(arr)) {
			return doPrimitiveArrayConvert(arr);
		} else {
			return doArrayConvert((Object[])arr);
		}
	}

	private static String doPrimitiveArrayConvert(Object arr) {
		List<Object> res = new LinkedList<>();
		if(arr instanceof int[]) {
			for(int i: (int[])arr) {res.add(i);}
		} else if (arr instanceof char[]) {
			for(char i: (char[])arr) {res.add(i);}
		} else if (arr instanceof byte[]) {
			for(byte i: (byte[])arr) {res.add(i);}
		} else if (arr instanceof short[]) {
			for(short i: (short[])arr) {res.add(i);}
		} else if (arr instanceof long[]) {
			for(long i: (long[])arr) {res.add(i);}
		} else if (arr instanceof float[]) {
			for(float i: (float[])arr) {res.add(i);}
		} else if (arr instanceof double[]) {
			for(double i: (double[])arr) {res.add(i);}
		}
		return res.toString();
	}

	/** 数组转换 */
	private static String doArrayConvert(Object[] arr) {
		List<String> res = new LinkedList<>();
		for(Object item : arr) {
			res.add(convert(item));
		}
		return res.toString();
	}

	/** map转换 */
	private static String mapConvert(Map<?, ?> map) throws Exception {
		var entry = map.entrySet();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		sb.append("{");
		for(var ent: entry) {
			Object key = ent.getKey();
			Object value = ent.getValue();
			if(first) {
				sb.append(String.format("\"%s\": %s", key, convert(value)));
				first = !first;
			} else {
				sb.append(String.format(",\"%s\": %s", key, convert(value)));
			}
		}
		sb.append("}");
		return sb.toString();
	}

	/** 列表中原子元素的转换 */
	private static String simpleConvert(Object item) {
		if(item instanceof String) {
			return String.format("\"%s\"", item);
		} else if (item instanceof Number) {
			return item.toString();
		} else {
			return null;
		}
	}
}
