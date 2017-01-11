package com.vti.guardian.nio.client;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vti.guardian.common.bean.Zdata;
import com.vti.guardian.discovery.core.ZookeeperDiscovery;
import com.vti.guardian.discovery.policy.Zpolicy;
import com.vti.guardian.nio.config.AppConfig;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NioClient extends ZookeeperDiscovery implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(NioClient.class);

	private Channel channel;

	private Zdata provider;

	public NioClient(String application, String zkurl, Zpolicy policy) {
		super(application, zkurl, policy);
	}

	@Override
	public void discover(Zdata current) {
		logger.info("Found new server node {}:{}", current.getHost(), current.getPort());
		this.provider = current;
	}

	@Override
	public void destory() {
		if (channel != null) {
			channel.close();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (provider != null) {
					start(provider);
					provider = null;
				} else {
					Thread.sleep(3 * 1000);
				}
			} catch (Exception e) {
				continue;
			}
		}
	}

	private void start(Zdata provider) throws Exception {

		if (channel != null) {
			channel.close();
		}

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(new NioEventLoopGroup());
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new LineBasedFrameDecoder(Integer.MAX_VALUE));
				ch.pipeline().addLast(new StringEncoder());
				ch.pipeline().addLast(new StringDecoder());
				ch.pipeline().addLast(new ClientHandler());
			}
		});

		channel = bootstrap.connect(provider.getHost(), provider.getPort()).sync().channel(); // (5)

		InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.localAddress();

		consume(provider, inetSocketAddress.getHostString(), inetSocketAddress.getPort());

		channel.closeFuture().sync();
	}

	public static void main(String[] args) {
		NioClient client = new NioClient(AppConfig.getConfig().getName(), AppConfig.getConfig().getZkurl(),
				Zpolicy.RANDOM);
		Executors.newCachedThreadPool().execute(client);
	}

}
