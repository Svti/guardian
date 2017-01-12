package com.vti.guardian.web.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.vti.guardian.common.bean.Session;
import com.vti.guardian.common.bean.Zdata;
import com.vti.guardian.common.cons.ZooKeeperConstant;
import com.vti.guardian.web.bean.Node;
import com.vti.guardian.web.bean.ZNode;
import com.vti.guardian.web.service.AppService;

@Component
public class AppServiceImpl implements AppService {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Resource
	private Environment environment;

	@Resource
	private CuratorFramework zkClient;

	@Override
	public Node findStatus() {

		String application = environment.getProperty("name");

		Node source = new Node();
		source.setName(application);

		String APPLICATION_ZK_PATH = ZooKeeperConstant.ZKSPLIT + application;

		try {
			List<String> providers = zkClient.getChildren()
					.forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH);

			for (String node : providers) {

				Node pnode = new Node();
				pnode.setName(node);
				source.getChildren().add(pnode);
			}

			List<String> sessions = zkClient.getChildren()
					.forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.CONTROLLER_ZK_PATH);

			for (String node : sessions) {

				byte[] bytes = zkClient.getData().forPath(
						APPLICATION_ZK_PATH + ZooKeeperConstant.CONTROLLER_ZK_PATH + ZooKeeperConstant.ZKSPLIT + node);
				Session session = new Gson().fromJson(new String(bytes), Session.class);

				String before = session.getProvider().getHost() + ZooKeeperConstant.ZKDOT
						+ session.getProvider().getPort();
				String next = session.getConsumer().getHost() + ZooKeeperConstant.ZKDOT
						+ session.getConsumer().getPort();

				for (Node pnode : source.getChildren()) {
					if (pnode.getName().equals(before)) {
						Node nnode = new Node();
						nnode.setName(next);
						pnode.getChildren().add(nnode);
					} else {
						continue;
					}
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}

		return source;
	}

	@Override
	public List<ZNode> findProviders() {
		String application = environment.getProperty("name");

		List<ZNode> nodes = new ArrayList<ZNode>();

		String APPLICATION_ZK_PATH = ZooKeeperConstant.ZKSPLIT + application;

		try {
			List<String> providers = zkClient.getChildren()
					.forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH);

			for (String node : providers) {

				byte[] bytes = zkClient.getData().forPath(
						APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH + ZooKeeperConstant.ZKSPLIT + node);

				Zdata zdata = new Gson().fromJson(new String(bytes), Zdata.class);
				
				ZNode znode = new ZNode();
				znode.setHost(zdata.getHost());
				znode.setPort(zdata.getPort());
				znode.setDate(new Date(zdata.getTimestamp()));

				nodes.add(znode);
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}

		return nodes;
	}

	@Override
	public List<ZNode> findConsumers() {
		String application = environment.getProperty("name");

		List<ZNode> nodes = new ArrayList<ZNode>();

		String APPLICATION_ZK_PATH = ZooKeeperConstant.ZKSPLIT + application;

		try {
			List<String> consumers = zkClient.getChildren()
					.forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.CONSUMER_ZK_PATH);

			for (String node : consumers) {

				byte[] bytes = zkClient.getData().forPath(
						APPLICATION_ZK_PATH + ZooKeeperConstant.CONSUMER_ZK_PATH + ZooKeeperConstant.ZKSPLIT + node);

				Zdata zdata = new Gson().fromJson(new String(bytes), Zdata.class);

				ZNode znode = new ZNode();
				znode.setHost(zdata.getHost());
				znode.setPort(zdata.getPort());
				znode.setDate(new Date(zdata.getTimestamp()));

				nodes.add(znode);
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}

		return nodes;
	}

}
