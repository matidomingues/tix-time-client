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
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class TixTimeClient {

    public static final int WORKER_THREADS;
    public static final String SERVER_IP;
    public static final int DEFAULT_CLIENT_PORT;
    public static final int DEFAULT_SERVER_PORT;
    public static final InetSocketAddress DEFAULT_SERVER_ADDRESS;
    public static final int LONG_PACKET_MAX_RETRIES; /* how many times will payload with measurement data be sent after every minute */
    public static final String FILE_NAME; /* file used to persist incoming message data */
    public static final String FILE_EXTENSION;
    private static final Logger logger = LogManager.getLogger();
    private static final Timer timer = new Timer();
    private static final String USERNAME;
    private static final String PASSWORD;
    private static final long USER_ID;
    private static final long INSTALLATION_ID;
    private static final KeyPair KEY_PAIR;
    public static String PREFERENCES_NODE;
    private static Path tempFile;
    private static boolean longPacketReceived = false;

    static {
        WORKER_THREADS = 1;
        SERVER_IP = "200.10.202.29";
        DEFAULT_CLIENT_PORT = 4501;
        DEFAULT_SERVER_PORT = 4500;
        LONG_PACKET_MAX_RETRIES = 5;

        PREFERENCES_NODE = "/com/tix/client";
        FILE_NAME = "tempfile";
        FILE_EXTENSION = ".tix";

        final Preferences prefs = Preferences.userRoot().node(PREFERENCES_NODE);
        USERNAME = prefs.get("username", "chavipc@gmail.com");
        PASSWORD = prefs.get("password", "12345678");
        USER_ID = prefs.getLong("userID", 6);
        INSTALLATION_ID = prefs.getLong("installationID", 5);
        final byte[] keyPairBytes = prefs.getByteArray("keyPair", SerializationUtils.serialize(TixCoreUtils.NEW_KEY_PAIR.get()));
        KEY_PAIR = SerializationUtils.deserialize(keyPairBytes);

        try {
            DEFAULT_SERVER_ADDRESS = new InetSocketAddress(SERVER_IP, DEFAULT_SERVER_PORT);
        } catch (Exception e) {
            logger.catching(e);
            logger.fatal("Could not initialize the default server address");
            throw new Error();
        }
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

            tempFile = Files.createTempFile(FILE_NAME, FILE_EXTENSION);
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

            writePackets(0, 1000, clientAddress, serverAddress, channel, tempFile);

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

    public static void main(String[] args) {
        new TixTimeClient(DEFAULT_SERVER_ADDRESS, DEFAULT_CLIENT_PORT);
    }

    public static void setLongPacketReceived(boolean value) {
        longPacketReceived = value;
    }

    public static Path getTempFile() {
        return tempFile;
    }

    private void writePackets(final int delay, final int period, final InetSocketAddress clientAddress, final InetSocketAddress serverAddress, final Channel channel, final Path tempFile) {
        timer.scheduleAtFixedRate(new TimerTask() {
            int i = 0;
            TixPacket shortPacket;
            TixPacket longPacketWithData;
            byte[] mostRecentData;
            byte[] signature;

            @Override
            public void run() {
                // sending short message every second, no matter what
                shortPacket = new TixPacket(clientAddress, serverAddress, TixPacketType.SHORT, TixCoreUtils.NANOS_OF_DAY.get());
                channel.writeAndFlush(shortPacket);

                if (i == 60) {
                    // send long packet once every minute, after short packet
                    try {
                        mostRecentData = Files.readAllBytes(tempFile);
                        System.out.println(tempFile.toAbsolutePath());
                    } catch (IOException e) {
                        logger.fatal("Could not read data from temp file", e);
                        logger.catching(Level.FATAL, e);
                    }
                    if (mostRecentData == null) {
                        logger.error("No measurements recorded in the last minute");
                    } else {
                        signature = TixCoreUtils.sign(mostRecentData, KEY_PAIR);
                        longPacketWithData = new TixDataPacket(clientAddress, serverAddress, TixCoreUtils.NANOS_OF_DAY.get(), USER_ID, INSTALLATION_ID, KEY_PAIR.getPublic().getEncoded(), mostRecentData, signature);
                        channel.writeAndFlush(longPacketWithData);
                        try {
                            byte[] emptyByteArray = new byte[0];
                            Files.write(tempFile, emptyByteArray, StandardOpenOption.TRUNCATE_EXISTING);
                        } catch (IOException e) {
                            logger.fatal("Could not empty contents of temp file", e);
                            logger.catching(Level.FATAL, e);
                        }
                        longPacketReceived = false;
                    }
                } else if (i > 60 && i < (60 + LONG_PACKET_MAX_RETRIES)) {
                    // resend long packet if needed, a limited number of times
                    if (longPacketReceived) {
                        mostRecentData = null;
                        signature = null;
                        i = 0;
                    } else if (mostRecentData != null) {
                        longPacketWithData = new TixDataPacket(clientAddress, serverAddress, TixCoreUtils.NANOS_OF_DAY.get(), USER_ID, INSTALLATION_ID, KEY_PAIR.getPublic().getEncoded(), mostRecentData, signature);
                        channel.writeAndFlush(longPacketWithData);
                    }
                } else if (i == (60 + LONG_PACKET_MAX_RETRIES)) {
                    // long packet could not be sent, discard temporary copy
                    mostRecentData = null;
                    signature = null;
                    i = 0;
                }
                i++;

            }
        }, delay, period);
    }

    private InetSocketAddress getClientAddress(int clientPort) throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getLocalHost(), clientPort);
    }

}
