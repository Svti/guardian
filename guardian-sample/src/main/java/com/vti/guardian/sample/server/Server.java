package com.vti.guardian.sample.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vti.guardian.common.bean.Zdata;
import com.vti.guardian.registry.core.ZookeeperRegistry;
import com.vti.guardian.sample.config.AppConfig;

public class Server extends Thread {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private ServerSocket socket;

	private Socket client;

	private ZookeeperRegistry registry;

	private Zdata node;

	public Server(String application, String zkurl, String host, int port) {
		try {
			// 绑定端口开始服务
			socket = new ServerSocket();

			socket.bind(new InetSocketAddress(host, port));

			// 连接zookeepr
			registry = new ZookeeperRegistry(application, zkurl);

			Zdata zdata = new Zdata();
			zdata.setHost(host);
			zdata.setPort(port);

			// 注册当前服务器的数据
			registry.regist(zdata);

			node = zdata;

			ExecutorService executor = Executors.newCachedThreadPool();

			while (true) {
				client = socket.accept();
				executor.execute(new ServerHandler(client));
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}
	}

	private class ServerHandler implements Runnable {
		private Socket client;
		private PrintStream out;
		private BufferedReader buf;

		public ServerHandler(Socket socket) {
			try {
				this.client = socket;
				out = new PrintStream(client.getOutputStream());
				buf = new BufferedReader(new InputStreamReader(client.getInputStream()));
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage(), e);
			}

		}

		@Override
		public void run() {
			try {
				boolean flag = true;
				while (flag) {
					String str = buf.readLine();
					if (str == null || "".equals(str)) {
						flag = false;
					} else {
						if ("bye".equals(str)) {
							flag = false;
						} else {
							String msg = "[Host]:" + node.getHost() + ":" + node.getPort() + " , [Echo]: " + str;
							logger.info("Server recieve:" + str);
							out.println(msg);
						}
					}
				}
				out.close();
				client.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		/*
		 * String application = "application";
		 * 
		 * String zkurl = "master:2181,datanodea:2181,datanodeb:2181";
		 * 
		 * String host = "192.168.134.1";
		 * 
		 * int port = 41414;
		 */

		Server server = new Server(AppConfig.getConfig().getName(), AppConfig.getConfig().getZkurl(),
				AppConfig.getConfig().getHost(), AppConfig.getConfig().getPort());

		server.setDaemon(Boolean.TRUE);

		server.start();
	}
}