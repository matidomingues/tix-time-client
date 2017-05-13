package com.github.tix_measurements.time.client;

import com.github.tix_measurements.time.client.handler.TixUdpClientHandler;
import com.github.tix_measurements.time.core.data.TixDataPacket;
import com.github.tix_measurements.time.core.data.TixPacket;
import com.github.tix_measurements.time.core.data.TixPacketType;
import com.github.tix_measurements.time.core.decoder.TixMessageDecoder;
import com.github.tix_measurements.time.core.encoder.TixMessageEncoder;
import com.github.tix_measurements.time.core.util.TixCoreUtils;
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class TixTimeClient {

	private static final Logger logger = LogManager.getLogger();
	private static final Timer timer = new Timer();
	private static final KeyPair keyPair = TixCoreUtils.NEW_KEY_PAIR.get();

	public static final int WORKER_THREADS;
	public static final int DEFAULT_CLIENT_PORT;
	public static final int DEFAULT_SERVER_PORT;
	public static final InetSocketAddress DEFAULT_SERVER_ADDRESS;
	public static final int LONG_PACKET_MAX_RETRIES; /* how many times will payload with measurement data be sent after every minute */
    public static final String FILE_NAME; /* file used to persist incoming message data */
    public static final String FILE_EXTENSION;

    private static boolean longPacketReceived;

	static {
		WORKER_THREADS = 1;
		DEFAULT_CLIENT_PORT = 4501;
		DEFAULT_SERVER_PORT = 4500;
		LONG_PACKET_MAX_RETRIES = 5;
		FILE_NAME = "tempfile";
        FILE_EXTENSION = ".tix";
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

            final Path tempFile = Files.createTempFile(FILE_NAME, FILE_EXTENSION);
            tempFile.toFile().deleteOnExit();

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

			writeMessages(0,1000,clientAddress, serverAddress, channel);

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
		} catch (IOException e) {
            logger.fatal("Cannot persist incoming message data", e);
            logger.catching(Level.FATAL, e);
        } finally {
			logger.info("Shutting down");
			timer.cancel();
			workerGroup.shutdownGracefully();
		}
	}

	private void writeMessages(final int delay, final int period, final InetSocketAddress clientAddress, final InetSocketAddress serverAddress, final Channel channel){
		timer.scheduleAtFixedRate(new TimerTask() {
            int i = 0;
            TixPacket shortPacket;
            TixPacket longPacketWithData;
            byte[] lastDataSent;
            byte[] signature;

			@Override
			public void run() {
				// sending short message every second, no matter what
                shortPacket = new TixPacket(clientAddress, serverAddress, TixPacketType.SHORT,TixCoreUtils.NANOS_OF_DAY.get());
				channel.write(shortPacket);
				channel.flush();

                if( i == 60 ){
                    // send long packet once every minute, after short packet
                    signature = TixCoreUtils.sign("message".getBytes(),keyPair);
                    longPacketWithData = new TixDataPacket(clientAddress, serverAddress, TixCoreUtils.NANOS_OF_DAY.get(), 11, 1, "mac".getBytes(), "message".getBytes(), signature);
                    lastDataSent = "message".getBytes();
                    channel.writeAndFlush(longPacketWithData);
                    i = 0;
                    longPacketReceived = false;
                } else if ( i < LONG_PACKET_MAX_RETRIES ){
                    // resend long packet if needed, a limited number of times
                    if(longPacketReceived){
                        lastDataSent = null;
                        signature = null;
                    } else {
                        longPacketWithData = new TixDataPacket(clientAddress, serverAddress, TixCoreUtils.NANOS_OF_DAY.get(), 11, 1, "mac".getBytes(), lastDataSent, signature);
                        channel.writeAndFlush(longPacketWithData);
                    }
                } else if ( i == LONG_PACKET_MAX_RETRIES ){
                    // long packet could not be sent, discard temporary copy
                    lastDataSent = null;
                    signature = null;
                }
				i++;

			}
		}, delay,period);
	}

	private InetSocketAddress getClientAddress(int clientPort) throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getLocalHost(), clientPort);
	}

	public static void confirmLongPacketReceived(){
        longPacketReceived = true;
    }

}
