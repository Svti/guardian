package com.vti.guardian.common.bean;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Zdata {

	private String host;

	private int port;
	
	private String ext;
	
	private long timestamp = System.currentTimeMillis();

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

	public long getTimestamp() {
		return timestamp;
	}
	
	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	@Override
	public boolean equals(Object obj) {
		Zdata other = (Zdata) obj;
		if (other.getHost().equals(this.getHost()) && other.getPort() == this.getPort()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		final int time = Long.valueOf(getTimestamp()).intValue();
		return new HashCodeBuilder(time % 2 == 0 ? time + 1 : time, PRIME).toHashCode();
	}
}
