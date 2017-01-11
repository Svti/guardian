package com.vti.guardian.discovery.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vti.guardian.common.bean.Zdata;

public class DiscoveryContext {

	public static final String LAST_ZDATA = "last";

	private static final Map<String, Zdata> zdataMap = new ConcurrentHashMap<String, Zdata>();
	
	public static Zdata getLastZdata() {
		return zdataMap.get(LAST_ZDATA);
	}

	public static void setLastZdata(Zdata zdata) {
		zdataMap.put(LAST_ZDATA, zdata);
	}

}
