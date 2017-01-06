package com.vti.guardian.common.bean;

public class Session {
	
	private Zdata provider;
	
	private Zdata consumer;
	
	private long timestamp = System.currentTimeMillis();

	public Zdata getProvider() {
		return provider;
	}

	public void setProvider(Zdata provider) {
		this.provider = provider;
	}

	public Zdata getConsumer() {
		return consumer;
	}

	public void setConsumer(Zdata consumer) {
		this.consumer = consumer;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
