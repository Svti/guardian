package com.vti.guardian.nio.server;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vti.guardian.common.bean.Zdata;
import com.vti.guardian.nio.config.AppConfig;
import com.vti.guardian.registry.core.ZookeeperRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NioServer extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(NioServer.class);

	private ZookeeperRegistry registry;

	private ChannelFuture channelFuture;

	public NioServer(String name, String zkurl, String host, int port) {

		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							//分包拆包器
							ch.pipeline().addLast(new LineBasedFrameDecoder(Integer.MAX_VALUE));
							ch.pipeline().addLast(new StringDecoder());
							ch.pipeline().addLast(new StringEncoder());
							ch.pipeline().addLast(new ServerHandler());
						}
					}).option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);

			channelFuture = b.bind(new InetSocketAddress(host, port)).sync();

			// 连接zookeepr
			registry = new ZookeeperRegistry(name, zkurl);

			Zdata zdata = new Zdata();
			zdata.setHost(host);
			zdata.setPort(port);

			// 注册当前服务器的数据
			registry.regist(zdata);

			channelFuture.channel().closeFuture().sync();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) {

		NioServer server = new NioServer(AppConfig.getConfig().getName(), AppConfig.getConfig().getZkurl(),
				AppConfig.getConfig().getHost(), AppConfig.getConfig().getPort());

		server.setDaemon(Boolean.TRUE);

		server.start();
	}
}
