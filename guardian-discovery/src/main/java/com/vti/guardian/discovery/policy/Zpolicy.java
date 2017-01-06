package com.vti.guardian.discovery.policy;

/**
 * 消费生产者的策略，目前提供：随机、轮询、(权重、熔断)暂未实现
 * @author vti
 *
 */
public enum Zpolicy implements EnumType {
	
	RANDOM(10, "random"),

	ROUNDROBIN(20, "RoundRobin"),

	WEIGHT(30, "weight"),

	FUSED(40, "fused"),

	;

	// 权重策略，产生随机数

	// 熔断策略，利用响应时间，优先使用响应时间短

	Zpolicy(int value, String text) {
		this.text = text;
		this.value = value;
	};

	private int value;
	private String text;

	@Override
	public int value() {
		return value;
	}

	@Override
	public String text() {
		return text;
	}

	public static Zpolicy nameOf(String name) {

		Zpolicy current = Zpolicy.RANDOM;

		for (Zpolicy policy : values()) {
			if (policy.text().equalsIgnoreCase(name)) {
				current = policy;
			}
		}

		return current;
	}
}
