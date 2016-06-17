package ar.edu.itba.tix.time.client;

import ar.edu.itba.tix.time.client.handler.TixUdpClientHandler;
import ar.edu.itba.tix.time.core.data.TixTimestampPackage;
import ar.edu.itba.tix.time.core.decoder.TixMessageDecoder;
import ar.edu.itba.tix.time.core.encoder.TixMessageEncoder;
import ar.edu.itba.tix.time.core.util.TixTimeUitl;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

public class TixTimeClient {

	private static final Logger logger = LogManager.getLogger();

	public static final int WORKER_THREADS;
	public static final int DEFAULT_CLIENT_PORT;
	public static final int DEFAULT_SERVER_PORT;
	public static final InetSocketAddress DEFAULT_SERVER_ADDRESS;

	static {
		WORKER_THREADS = 1;
		DEFAULT_CLIENT_PORT = 4501;
		DEFAULT_SERVER_PORT = 4500;
		try {
			DEFAULT_SERVER_ADDRESS = new InetSocketAddress(InetAddress.getLocalHost(), DEFAULT_SERVER_PORT);
		} catch (UnknownHostException e) {
			logger.catching(e);
			logger.fatal("Could not initialize the default server address");
			throw new Error();
		}
	}

	public static void main(String[] args) {
		new TixTimeClient(DEFAULT_SERVER_ADDRESS, DEFAULT_CLIENT_PORT);
	}

	private TixTimeClient(InetSocketAddress serverAddress, int clientPort) {
		logger.info("Starting Client");
		logger.info("Server Address: {}:{}", serverAddress.getHostName(), serverAddress.getPort());
		EventLoopGroup workerGroup;
		Class<? extends Channel> datagramChannelClass;
		if (Epoll.isAvailable()) {
			logger.info("epoll available");
			workerGroup = new EpollEventLoopGroup(WORKER_THREADS);
			datagramChannelClass = EpollDatagramChannel.class;
		} else {
			logger.info("epoll unavailable");
			logger.warn("epoll unavailable performance may be reduced due to single thread scheme.");
			workerGroup = new NioEventLoopGroup(WORKER_THREADS, Executors.privilegedThreadFactory());
			datagramChannelClass = NioDatagramChannel.class;
		}

		try {
			logger.info("Setting up");
			InetSocketAddress clientAddress = getClientAddress(clientPort);
			logger.info("My Address: {}:{}", clientAddress.getAddress(), clientAddress.getPort());
			Bootstrap b = new Bootstrap();
			b.group(workerGroup)
					.channel(datagramChannelClass)
					.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
					.handler(new ChannelInitializer<DatagramChannel>() {
						@Override
						protected void initChannel(DatagramChannel ch)
								throws Exception {
							ch.pipeline().addLast(new TixMessageDecoder());
							ch.pipeline().addLast(new TixUdpClientHandler(clientAddress, serverAddress));
							ch.pipeline().addLast(new TixMessageEncoder());
						}
					});
			if (Epoll.isAvailable()) {
				b.option(EpollChannelOption.SO_REUSEPORT, true);
			}
			logger.info("Binding into port {}", clientAddress.getPort());
			Channel channel = b.bind(clientAddress).sync().channel();
			TixTimestampPackage timestampPackage = new TixTimestampPackage(clientAddress, serverAddress, TixTimeUitl.NANOS_OF_DAY.get());
			channel.writeAndFlush(timestampPackage);
			ChannelFuture future = channel.closeFuture().await();
			if (!future.isSuccess()) {
				logger.error("Error while transmitting");
			}
		} catch (InterruptedException e) {
			logger.fatal("Interrupted", e);
			logger.catching(Level.FATAL, e);
		} catch (UnknownHostException e) {
			logger.fatal("Cannot retrieve local host address", e);
			logger.catching(Level.FATAL, e);
		} finally {
			logger.info("Shutting down");
			workerGroup.shutdownGracefully();
		}
	}

	private InetSocketAddress getClientAddress(int clientPort) throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getLocalHost(), clientPort);
	}
}
