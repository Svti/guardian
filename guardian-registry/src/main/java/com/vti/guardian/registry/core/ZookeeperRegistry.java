package com.vti.guardian.registry.core;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.vti.guardian.common.bean.Zdata;
import com.vti.guardian.common.cons.ZooKeeperConstant;

public class ZookeeperRegistry {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private CuratorFramework zkClient;

	private String APPLICATION_ZK_PATH;

	private Zdata current;

	public ZookeeperRegistry(String application, String zkurl) {

		APPLICATION_ZK_PATH = ZooKeeperConstant.ZKSPLIT + application;

		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);

		zkClient = CuratorFrameworkFactory.newClient(zkurl, 1000, 100 * 1000, retryPolicy);

		zkClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {

			@Override
			public void stateChanged(CuratorFramework framework, ConnectionState state) {
				if (state == ConnectionState.RECONNECTED) {
					//重新连接，重新注册生产者
					if (current != null) {
						regist(current);
					}
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

			logger.info("ZooKeeperFactory init in {} finish ", APPLICATION_ZK_PATH);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 注册服务端数据
	 * 
	 * @param zdata
	 */
	public void regist(Zdata zdata) {

		try {

			current = zdata;

			String data = new Gson().toJson(zdata);

			String path = APPLICATION_ZK_PATH + ZooKeeperConstant.PROVIDER_ZK_PATH + ZooKeeperConstant.ZKSPLIT
					+ zdata.getHost() + ZooKeeperConstant.ZKDOT + +zdata.getPort();

			if (zkClient.checkExists().forPath(path) != null) {
				zkClient.delete().forPath(path);
			}

			String create = zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path,
					data.getBytes());

			logger.info("create zookeeper node ({} => {})", create, data);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
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
