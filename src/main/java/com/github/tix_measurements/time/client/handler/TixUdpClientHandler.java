package com.github.tix_measurements.time.client.handler;

import com.github.tix_measurements.time.client.TixTimeClient;
import com.github.tix_measurements.time.core.data.TixDataPacket;
import com.github.tix_measurements.time.core.data.TixPacket;
import com.github.tix_measurements.time.core.data.TixPacketType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static com.github.tix_measurements.time.client.TixTimeClient.getTempFile;

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
        TixPacket incomingMessage = (TixPacket) msg;
        logger.info("Received package {} from {}", incomingMessage, incomingMessage.getFrom());
        persistTixPacket(incomingMessage);
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

    private void persistTixPacket(final TixPacket packet) {
        try {
            if (packet.getType() == TixPacketType.LONG) {

                TixTimeClient.setLongPacketReceived(true);

            } else if (packet.getFinalTimestamp() != 0) {

                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
                final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

                final long unixTime = System.currentTimeMillis() / 1000L; // seconds passed since UNIX epoch
                outputStream.write(longBuffer.putLong(unixTime).array());

                outputStream.write(packet.getType() == TixPacketType.LONG ? (byte) 'L' : (byte) 'S'); //char to byte cast should be OK for ASCII chars

                outputStream.write(intBuffer.putInt(packet.getType().getSize()).array());

                outputStream.write(longBuffer.putLong(packet.getInitialTimestamp()).array());

                outputStream.write(longBuffer.putLong(packet.getReceptionTimestamp()).array());

                outputStream.write(longBuffer.putLong(packet.getSentTimestamp()).array());

                outputStream.write(longBuffer.putLong(packet.getFinalTimestamp()).array());

                Files.write(getTempFile(), outputStream.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
