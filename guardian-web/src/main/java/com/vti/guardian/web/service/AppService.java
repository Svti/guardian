package com.vti.guardian.web.service;

import java.util.List;

import com.vti.guardian.web.bean.Node;
import com.vti.guardian.web.bean.ZNode;

public interface AppService {

	public Node findStatus();

	public List<ZNode> findProviders();

	public List<ZNode> findConsumers();
}
