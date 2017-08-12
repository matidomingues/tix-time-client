package com.github.tix_measurements.time.client.reporting;

import com.github.tix_measurements.time.client.Main;
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
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class Reporter extends Service<Void> {

    public static final int WORKER_THREADS;
    public static final String SERVER_IP;
    public static final int DEFAULT_CLIENT_PORT;
    public static final int DEFAULT_SERVER_PORT;
    public static final InetSocketAddress DEFAULT_SERVER_ADDRESS;

    public static final int MAX_UDP_PACKET_SIZE;
    public static final int LONG_PACKET_MAX_RETRIES; /* how many times will payload with measurement data be sent after every minute */

    public static final String FILE_NAME; /* file used to persist incoming message data */
    public static final String FILE_EXTENSION;
    private static Path tempFile;

    private static final Logger logger = LogManager.getLogger();
    private static final Timer timer = new Timer();

    public static String PREFERENCES_NODE;
    private static final long USER_ID;
    private static final long INSTALLATION_ID;
    private static final KeyPair KEY_PAIR;

    private static boolean longPacketReceived = false;

    static {
        WORKER_THREADS = 1;
        SERVER_IP = "200.10.202.29";
        DEFAULT_CLIENT_PORT = 4501;
        DEFAULT_SERVER_PORT = 4500;

        MAX_UDP_PACKET_SIZE = 4096 + 1024;
        LONG_PACKET_MAX_RETRIES = 5;

        PREFERENCES_NODE = "/com/tix/client50";
        FILE_NAME = "tempfile";
        FILE_EXTENSION = ".tix";

        USER_ID = Main.preferences.getLong("userID", 0L);
        INSTALLATION_ID = Main.preferences.getLong("installationID", 0L);
        final byte[] keyPairBytes = Main.preferences.getByteArray("keyPair", null);
        try {
            KEY_PAIR = SerializationUtils.deserialize(keyPairBytes);
        } catch (Exception e) {
            logger.catching(e);
            logger.fatal("Could not read existing keyPair");
            throw new Error();
        }

        try {
            DEFAULT_SERVER_ADDRESS = new InetSocketAddress(SERVER_IP, DEFAULT_SERVER_PORT);
        } catch (Exception e) {
            logger.catching(e);
            logger.fatal("Could not initialize the default server address");
            throw new Error();
        }
    }

    public static void main(String[] args) {
        new Reporter();
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
                        System.out.println("Temp file path: " + tempFile.toAbsolutePath());
                    } catch (IOException e) {
                        logger.fatal("Could not read data from temp file", e);
                        logger.catching(Level.FATAL, e);
                    }
                    if (mostRecentData == null || mostRecentData.length < 1) {
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
                    } else if (mostRecentData != null && mostRecentData.length > 0) {
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

    private InetSocketAddress getClientAddress(int clientPort) throws IOException {
        // Adapted from https://stackoverflow.com/questions/8462498/how-to-determine-internet-network-interface-in-java
        // iterate over the network interfaces known to java
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface interface_ : Collections.list(interfaces)) {
            // we shouldn't care about loopback addresses
            if (interface_.isLoopback())
                continue;

            // if you don't expect the interface to be up you can skip this
            // though it would question the usability of the rest of the code
            if (!interface_.isUp())
                continue;

            // iterate over the addresses associated with the interface
            Enumeration<InetAddress> addresses = interface_.getInetAddresses();
            for (InetAddress address : Collections.list(addresses)) {
                // use a timeout big enough for your needs
                if (!address.isReachable(3000))
                    continue;

                // java 7's try-with-resources statement, so that
                // we close the socket immediately after use
                try (SocketChannel socket = SocketChannel.open()) {
                    // again, use a big enough timeout
                    socket.socket().setSoTimeout(3000);

                    // bind the socket to your local interface
                    socket.bind(new InetSocketAddress(address, 8080));

                    // try to connect to *somewhere*
                    socket.connect(new InetSocketAddress("tix.innova-red.net", 80));

                    System.out.format("ni: %s, ia: %s\n", interface_, address);

                    return new InetSocketAddress(address,clientPort);

                } catch (IOException ex) {
                    continue;
                }
            }
        }
        return new InetSocketAddress(InetAddress.getLocalHost(), clientPort);
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            protected Void call() {
                logger.info("Starting Client");
                logger.info("Server Address: {}:{}", DEFAULT_SERVER_ADDRESS.getHostName(), DEFAULT_SERVER_ADDRESS.getPort());

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
                    InetSocketAddress clientAddress = getClientAddress(DEFAULT_CLIENT_PORT);
                    logger.info("My Address: {}:{}", clientAddress.getAddress(), clientAddress.getPort());

                    tempFile = Files.createTempFile(FILE_NAME, FILE_EXTENSION);
                    System.out.println(tempFile.toString());
                    tempFile.toFile().deleteOnExit();

                    Bootstrap b = new Bootstrap();
                    b.group(workerGroup)
                            .channel(datagramChannelClass)
                            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                            .option(ChannelOption.SO_RCVBUF, MAX_UDP_PACKET_SIZE)
                            .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(MAX_UDP_PACKET_SIZE))
                            .handler(new ChannelInitializer<DatagramChannel>() {
                                @Override
                                protected void initChannel(DatagramChannel ch)
                                        throws Exception {
                                    ch.pipeline().addLast(new TixMessageDecoder());
                                    ch.pipeline().addLast(new TixUdpClientHandler());
                                    ch.pipeline().addLast(new TixMessageEncoder());
                                }
                            });
                    if (Epoll.isAvailable()) {
                        b.option(EpollChannelOption.SO_REUSEPORT, true);
                    }
                    logger.info("Binding into port {}", clientAddress.getPort());
                    Channel channel = b.bind(clientAddress).sync().channel();

                    writePackets(0, 1000, clientAddress, DEFAULT_SERVER_ADDRESS, channel, tempFile);

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
                return null;
            }
        };
    }
}