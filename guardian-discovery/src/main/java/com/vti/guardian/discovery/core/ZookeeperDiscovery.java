package com.vti.guardian.discovery.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.vti.guardian.common.bean.Session;
import com.vti.guardian.common.bean.Zdata;
import com.vti.guardian.common.cons.ZooKeeperConstant;
import com.vti.guardian.discovery.context.DiscoveryContext;
import com.vti.guardian.discovery.policy.Zpolicy;

public abstract class ZookeeperDiscovery {

	private static final Logger logger = LoggerFactory.getLogger(ZookeeperDiscovery.class);

	private CuratorFramework zkClient;

	private String APPLICATION_ZK_PATH;

	private Zpolicy zpolicy;

	public abstract void discover(Zdata current);

	public abstract void destory();

	public ZookeeperDiscovery(String application, String zkurl, Zpolicy policy) {

		this.zpolicy = policy;

		APPLICATION_ZK_PATH = ZooKeeperConstant.ZKSPLIT + application;

		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, Integer.MAX_VALUE);

		zkClient = CuratorFrameworkFactory.newClient(zkurl, 1000, 100 * 1000, retryPolicy);

		zkClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {

			@Override
			public void stateChanged(CuratorFramework framework, ConnectionState state) {
				if (state == ConnectionState.SUSPENDED || state == ConnectionState.LOST) {
					// 断开连接
					destory();
				}
				if (state == ConnectionState.RECONNECTED) {
					// 网络重连
					watchNode(framework);
				}
			}
		});

		zkClient.start();

		try {

			if (zkClient.checkExists().forPath(APPLICATION_ZK_PATH) == null) {
				zkClient.create().withMode(CreateMode.PERSISTENT).forPath(APPLICATION_ZK_PATH);
			}

			if (zkClient.checkExists().forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.CONSUMER_ZK_PATH) == null) {
				zkClient.create().withMode(CreateMode.PERSISTENT)
						.forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.CONSUMER_ZK_PATH);
			}

			if (zkClient.checkExists().forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH) == null) {
				zkClient.create().withMode(CreateMode.PERSISTENT)
						.forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH);
			}

			if (zkClient.checkExists().forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.CONTROLLER_ZK_PATH) == null) {
				zkClient.create().withMode(CreateMode.PERSISTENT)
						.forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.CONTROLLER_ZK_PATH);
			}

			logger.info("ZookeeperDiscovery init in {} finish ", APPLICATION_ZK_PATH);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}

		watchNode(zkClient);
	}

	/**
	 * 消费数据
	 * 
	 * @param provider
	 * @param host
	 * @param port
	 * @throws Exception
	 */
	public void consume(Zdata provider, String host, int port) throws Exception {

		Session session = new Session();
		session.setProvider(provider);

		Zdata consumer = new Zdata();
		consumer.setHost(host);
		consumer.setPort(port);

		session.setConsumer(consumer);

		byte[] data = new Gson().toJson(session).getBytes();

		String path = APPLICATION_ZK_PATH + ZooKeeperConstant.CONSUMER_ZK_PATH + ZooKeeperConstant.ZKSPLIT
				+ consumer.getHost() + ZooKeeperConstant.ZKDOT + +consumer.getPort();

		if (zkClient.checkExists().forPath(path) != null) {
			zkClient.delete().forPath(path);
		}

		zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);

		zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(
				APPLICATION_ZK_PATH + ZooKeeperConstant.CONTROLLER_ZK_PATH + ZooKeeperConstant.SESSION_ZK_PATH, data);
	}

	private void watchNode(CuratorFramework zkClient) {

		try {
			// 获取变化的消费者
			List<String> providers = zkClient.getChildren().usingWatcher(new CuratorWatcher() {
				@Override
				public void process(WatchedEvent event) {
					// 节点变化
					if (event.getType() == EventType.NodeChildrenChanged) {
						watchNode(zkClient);
					}
				}
			}).forPath(APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH);

			List<Zdata> nodes = new ArrayList<>();

			for (String node : providers) {
				byte[] bytes = zkClient.getData().forPath(
						APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH + ZooKeeperConstant.ZKSPLIT + node);

				Zdata zdata = new Gson().fromJson(new String(bytes), Zdata.class);
				nodes.add(zdata);
			}

			if (!nodes.isEmpty()) {

				Zdata provider = null;

				switch (zpolicy) {

				case ROUNDROBIN:
					provider = roundRobin(nodes);
					break;
				default:
					provider = random(nodes);
					break;
				}

				discover(provider);

				DiscoveryContext.setLastZdata(provider);
			}

			// 销毁上次的生产者数据
			validate(nodes);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}
	}

	private Zdata random(List<Zdata> nodes) {
		// random

		Collections.shuffle(nodes);

		Zdata current = nodes.iterator().next();

		return current;
	}

	private Zdata roundRobin(List<Zdata> nodes) throws Exception {
		// Round-Robin，保存标记，下一次标记+1

		Zdata last = DiscoveryContext.getLastZdata();

		if (last == null || nodes.size() == 1) {
			return nodes.iterator().next();
		} else {
			Zdata current = null;
			for (int i = 0; i < nodes.size(); i++) {
				if (nodes.get(i).equals(last)) {
					int next = i + 1;
					if (next == nodes.size()) {
						current = nodes.iterator().next();
					} else {
						current = nodes.get(next);
					}
					break;
				}
			}
			if (current == null) {
				return nodes.iterator().next();
			}
			return current;
		}
	}

	private void validate(List<Zdata> providers) throws Exception {

		if (providers.isEmpty()) {
			return;
		}

		String path = APPLICATION_ZK_PATH + ZooKeeperConstant.CONTROLLER_ZK_PATH;

		List<String> sessions = zkClient.getChildren().forPath(path);

		for (String node : sessions) {

			String npath = APPLICATION_ZK_PATH + ZooKeeperConstant.CONTROLLER_ZK_PATH + ZooKeeperConstant.ZKSPLIT
					+ node;

			if (zkClient.checkExists().forPath(npath) == null) {
				continue;
			}

			byte[] bytes = zkClient.getData().forPath(npath);

			Session session = new Gson().fromJson(new String(bytes), Session.class);

			if (!providers.contains(session.getProvider())) {

				String cpath = APPLICATION_ZK_PATH + ZooKeeperConstant.CONSUMER_ZK_PATH + ZooKeeperConstant.ZKSPLIT
						+ session.getConsumer().getHost() + ZooKeeperConstant.ZKDOT + +session.getConsumer().getPort();

				if (zkClient.checkExists().forPath(cpath) != null) {
					zkClient.delete().inBackground().forPath(cpath);
				}

				if (zkClient.checkExists().forPath(npath) != null) {
					zkClient.delete().inBackground().forPath(npath);
				}
			}
		}
	}

	/**
	 * 关闭客户端
	 */
	public void close() {
		if (zkClient != null) {
			zkClient.close();
		}
	}
}
