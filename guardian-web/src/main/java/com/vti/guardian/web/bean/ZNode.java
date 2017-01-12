package com.vti.guardian.web.bean;

import java.util.Date;

public class ZNode {
	
	private String host;

	private int port;
	
	private Date date;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
