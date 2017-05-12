package com.github.tix_measurements.time.client.handler;

import com.github.tix_measurements.time.core.data.TixDataPacket;
import com.github.tix_measurements.time.core.data.TixPacket;
import com.github.tix_measurements.time.core.data.TixPacketType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;

import static com.github.tix_measurements.time.client.TixTimeClient.FILE_EXTENSION;
import static com.github.tix_measurements.time.client.TixTimeClient.FILE_NAME;

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
		TixPacket incomingMessage = (TixPacket)msg;
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

	private void persistTixPacket(final TixPacket packet){
        try {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if(packet.getFinalTimestamp() != 0){

				final ByteBuffer charBuffer = ByteBuffer.allocate(Character.BYTES);
				final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
				final ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

				final long unixTime = System.currentTimeMillis() / 1000L; // seconds passed since unix epoch
				outputStream.write(longBuffer.putLong(unixTime).array());

				outputStream.write(TixDataPacket.DATA_DELIMITER.getBytes());

				outputStream.write(packet.getType() == TixPacketType.LONG ? charBuffer.putChar('L').array() : charBuffer.putChar('S').array());

				outputStream.write(TixDataPacket.DATA_DELIMITER.getBytes());

				outputStream.write(intBuffer.putInt(packet.getType().getSize()).array());

				outputStream.write(TixDataPacket.DATA_DELIMITER.getBytes());

				outputStream.write(longBuffer.putLong(packet.getInitialTimestamp()).array());

				outputStream.write(TixDataPacket.DATA_DELIMITER.getBytes());

				outputStream.write(longBuffer.putLong(packet.getReceptionTimestamp()).array());

				outputStream.write(TixDataPacket.DATA_DELIMITER.getBytes());

				outputStream.write(longBuffer.putLong(packet.getSentTimestamp()).array());

				outputStream.write(TixDataPacket.DATA_DELIMITER.getBytes());

				outputStream.write(longBuffer.putLong(packet.getFinalTimestamp()).array());

				outputStream.write("\r\n".getBytes());

				final Path tempFile = Files.createTempFile(FILE_NAME, FILE_EXTENSION);
				Files.write(tempFile, outputStream.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
			}

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
