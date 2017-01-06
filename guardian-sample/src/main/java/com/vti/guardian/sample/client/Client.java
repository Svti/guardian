package com.vti.guardian.sample.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vti.guardian.common.bean.Zdata;
import com.vti.guardian.discovery.core.ZookeeperDiscovery;
import com.vti.guardian.discovery.policy.Zpolicy;
import com.vti.guardian.sample.config.AppConfig;

public class Client extends ZookeeperDiscovery implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Client.class);

	private Socket client;

	private Zdata provider;

	private String zkurl;

	public Client(String application, String zkurl, Zpolicy policy) {
		super(application, zkurl, policy);
		this.zkurl = zkurl;
		conn();
	}

	// 等待zookeeper的消息回调
	@Override
	public void discover(Zdata current) {
		provider = current;
		logger.info("Found new server node {}:{}", provider.getHost(), provider.getPort());
		//TODO 控制重连的方式，目前通过异常重连。后面 一旦有新生产者加入 ，考虑 权重，熔断机制，来判断是否放弃当前可用连接
	}

	// 等待zookeeper的消息回调
	@Override
	public void destory(){
		// zookeeper断开，停止消费，即使能够连上生产者，也要停止，为了保持一致。
		if (client != null) {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void conn() {
		try {
			if (provider == null) {
				logger.error("Not found server in zk {}", zkurl);
				// 没有则一直等待
				Thread.sleep(3000);
				conn();
			} else {
				
				// 即使有连接，断掉。用优质的连接
				if (client != null) {
					client.close();
				}

				// 开始socket连接
				client = new Socket(provider.getHost(), provider.getPort());
				
				// 注册消费者节点
				consume(provider, client.getLocalAddress().getHostAddress(), client.getLocalPort());

				// 清除用过的消费数据
				provider = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info(e.getMessage(), e);
		}
	}

	@Override
	public void run() {

		while (true) {

			try {
				Thread.sleep(3000);

				// 客户端为空，跳过
				if (client == null) {
					continue;
				}

				// 读取当前数据
				BufferedReader buf = new BufferedReader(new InputStreamReader(client.getInputStream()));

				OutputStream outputStream = client.getOutputStream();
				String message = "time is " + System.currentTimeMillis() + "\r\n";
				outputStream.write(message.getBytes());
				outputStream.flush();

				String msg = buf.readLine();

				logger.info("Client recieve:" + msg);

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				// 异常重连
				conn();
				continue;
			}
		}
	}

	public static void main(String[] args) {
		// zookeeper 根节点名称，可填用程序名称
		/*String application = "application";

		String zkurl = "master:2181,datanodea:2181,datanodeb:2181";*/

		Client client = new Client(AppConfig.getConfig().getName(), AppConfig.getConfig().getZkurl(), Zpolicy.RANDOM);

		Executors.newCachedThreadPool().execute(client);
	}
}
