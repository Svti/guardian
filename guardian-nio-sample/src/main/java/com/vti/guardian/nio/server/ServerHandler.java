package com.vti.guardian.nio.server;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerHandler extends ChannelInboundHandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(ServerHandler.class);

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
		String echo = "[Host]:" + inetSocketAddress.getHostString() + " , [Port]:" + inetSocketAddress.getPort()
				+ " , [Echo]:" + msg + "\r\n";
		logger.info("Server recieve:" + msg);
		ctx.writeAndFlush(echo);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}
