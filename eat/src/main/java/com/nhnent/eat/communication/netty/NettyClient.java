package com.nhnent.eat.communication.netty;

import co.paralleluniverse.fibers.SuspendExecution;
import com.nhnent.eat.common.Config.Config;
import com.nhnent.eat.communication.netty.stream.StreamSocketPacketClientHandler;
import com.nhnent.eat.communication.netty.ws.SslHandler;
import com.nhnent.eat.communication.netty.ws.WebSocketClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;


/**
 * Created by NHNEnt on 2017-03-28.
 */
public class NettyClient implements Closeable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String serverIp;
    private final int svrPort;

    private EventLoopGroup[] groups;

    private Boolean isConnected = Boolean.FALSE;

    private StreamSocketPacketClientHandler streamSocketPacketClientHandler;
    private WebSocketClientHandler webSocketClientHandler;

    private Config config;

    public NettyClient(Config config) {
        this.config = config;
        this.serverIp = this.config.getServer().getIp();
        this.svrPort = config.getServer().getPort();
    }

    public void initEventLoopGroup() {
        int cntOfRealThread = config.getCommon().getCountOfRealThread();
        groups = new EventLoopGroup[cntOfRealThread];
        for (int i = 0; i < cntOfRealThread; i++) {
            groups[i] = new NioEventLoopGroup();
        }
    }



    /**
     * Start up Netty client
     */
    public void startUp(int actorIndex) {
        int cntOfRealThread = config.getCommon().getCountOfRealThread();
        int cntOfPort = config.getServer().getCountOfPort();
        int portNo = svrPort + (actorIndex % cntOfPort);
        int groupIndex = actorIndex % cntOfRealThread;

        logger.debug("actorIndex=>{}, portNo=>{}, groupIndex=>{}", actorIndex, portNo, groupIndex);

        try {

            int timeout = config.getCommon().getReceiveTimeoutSec();

            Bootstrap b = new Bootstrap();
            b.group(groups[groupIndex])
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {

                            if (config.getServer().getSsl() != null) {
                                SslContext sslContext = SslHandler.initSSL(config);
                                if (sslContext != null) {
                                    logger.info("use ssl");
                                    ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                                }
                            }

                            if (config.getServer().getSocketType().equals("STREAM")) {

                                ch.pipeline().addLast("readTimeoutHandler",
                                        new ReadTimeoutWithNoClose(timeout));
                                ch.pipeline().addLast("streamSocketPacketClientHandler",
                                        streamSocketPacketClientHandler);

                            } else if (config.getServer().getSocketType().equals("WEBSOCKET")) {

                                ch.pipeline().addLast("http-codec", new HttpClientCodec());
                                ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                                ch.pipeline().addLast("ws-handler", webSocketClientHandler);

                            } else {
                                logger.error("Unsupported socket type");
                            }
                        }
                    });

            Channel ch = b.connect(serverIp, portNo).sync().channel();
            logger.debug("Channel==> {}", ch.toString());

            isConnected = Boolean.TRUE;

        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * Set Packet Listener
     *
     * @param packetListener PacketListener
     */
    public void setPacketListener(PacketListener packetListener) {
        if (config.getServer().getSocketType().equals("STREAM")) {
            streamSocketPacketClientHandler = new StreamSocketPacketClientHandler();
            streamSocketPacketClientHandler.setPacketListener(packetListener);
        } else {

            //If socket type is `WEBSOCKET`
            final String uriString = String.format("ws://%s:%s/%s", config.getServer().getIp(),
                    config.getServer().getPort(),
                    config.getServer().getSubUriOfWS());
            URI uri = URI.create(uriString);

            webSocketClientHandler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory
                                    .newHandshaker(
                                            uri, WebSocketVersion.V13, null, false,
                                            new DefaultHttpHeaders())
                    );
            webSocketClientHandler.setPacketListener(packetListener);
        }
    }

    /**
     * Transfer packet to server
     *
     * @param sendPck Packet to transmit
     * @throws SuspendExecution     Throw exception
     * @throws InterruptedException Throw exception
     */
    public void transferPacket(final byte[] sendPck) throws SuspendExecution, InterruptedException {
        int actorIndex = 0;
        if (!isConnected) {
            startUp(actorIndex);
        }

        if (config.getServer().getSocketType().equals("STREAM")) {
            streamSocketPacketClientHandler.transferPacket(sendPck);
        } else {
            webSocketClientHandler.transferPacket(sendPck);
        }
    }

    /**
     * close netty socket and destroy Netty Worker Group
     */
    public void disconnect() {
        if (config.getServer().getSocketType().equals("STREAM")) {
            streamSocketPacketClientHandler.close();
        } else {
            webSocketClientHandler.close();
        }

        isConnected = Boolean.FALSE;
    }

    @Override
    public void close() {
        shutdownEventLoopGroup();
    }

    public void shutdownEventLoopGroup() {
        int cntOfRealThread = config.getCommon().getCountOfRealThread();
        for (int i = 0; i < cntOfRealThread; i++) {
            if (!groups[i].isShutdown()) {
                groups[i].shutdownGracefully();
            }
        }
    }

    public Boolean getConnected() {
        return isConnected;
    }
}
