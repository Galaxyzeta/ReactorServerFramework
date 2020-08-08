package com.galaxyzeta.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class ConfigParser {
	private String configFilePath = "";
	public ConfigParser(String filePath) {
		this.configFilePath = filePath;
	}

	public HashMap<String, String> parse() {
		HashMap<String, String> map = new HashMap<>();
		try (BufferedReader fr = new BufferedReader(new FileReader(this.configFilePath))) {
			String line;
			while((line = fr.readLine()) != null) {
				String[] arr = line.split(":");
				if(arr.length != 2) {
					throw new IOException("配置文件格式错误");
				}
				map.put(arr[0].trim(), arr[1].trim());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
}