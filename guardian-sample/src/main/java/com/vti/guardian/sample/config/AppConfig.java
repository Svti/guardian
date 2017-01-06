package com.vti.guardian.sample.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {

	private String name;

	private String host;

	private String zkurl;

	private int port;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getZkurl() {
		return zkurl;
	}

	public void setZkurl(String zkurl) {
		this.zkurl = zkurl;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public static AppConfig getConfig() {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(
					new File(Thread.class.getResource("/").getPath()).getParent() + "/conf/config.properties"));
			AppConfig appConfig = new AppConfig();
			appConfig.setHost(properties.getProperty("host"));
			appConfig.setName(properties.getProperty("name"));
			appConfig.setPort(Integer.valueOf(properties.getProperty("port")));
			appConfig.setZkurl(properties.getProperty("zkurl"));
			return appConfig;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
