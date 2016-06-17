package ar.edu.itba.tix.time.client.handler;

import ar.edu.itba.tix.time.core.data.TixTimestampPackage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class TixUdpClientHandler extends ChannelInboundHandlerAdapter {

	private final Logger logger = LogManager.getLogger();
	private final InetSocketAddress fromAddress;
	private final InetSocketAddress toAddress;

	public TixUdpClientHandler(InetSocketAddress fromAddress, InetSocketAddress toAddress) {
		this.fromAddress = fromAddress;
		this.toAddress = toAddress;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.entry(ctx, msg);
		TixTimestampPackage timestampPackage = (TixTimestampPackage)msg;
		logger.info("Received package {} from {}", timestampPackage, timestampPackage.getFrom());
		super.channelRead(ctx, msg);
		logger.exit();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.entry(ctx, cause);
		logger.catching(cause);
		logger.error("exception caught", cause);
		logger.exit();
	}
}
