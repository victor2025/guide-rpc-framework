package com.victor2022.remoting.transport.netty.client;


import com.victor2022.enums.CompressTypeEnum;
import com.victor2022.enums.SerializationTypeEnum;
import com.victor2022.factory.SingletonFactory;
import com.victor2022.registry.ServiceDiscovery;
import com.victor2022.remoting.constants.RpcConstants;
import com.victor2022.remoting.dto.RpcMessage;
import com.victor2022.remoting.dto.RpcRequest;
import com.victor2022.remoting.dto.RpcResponse;
import com.victor2022.remoting.transport.netty.codec.RpcMessageDecoder;
import com.victor2022.remoting.transport.netty.codec.RpcMessageEncoder;
import com.victor2022.extension.ExtensionLoader;
import com.victor2022.remoting.transport.RpcRequestTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * initialize and close Bootstrap object
 *
 * @author shuang.kou
 * @createTime 2020年05月29日 17:51:00
 */
@Slf4j
public final class NettyRpcClient implements RpcRequestTransport {

    // 负责从注册中心拉取服务列表，负载均衡在此发生
    private final ServiceDiscovery serviceDiscovery;
    // 保存未处理的请求(发送了请求但尚未收到返回消息)
    private final UnprocessedRequests unprocessedRequests;
    // channel缓存，保存已经建立的连接
    private final ChannelProvider channelProvider;
    // netty组件
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    public NettyRpcClient() {
        // initialize resources such as EventLoopGroup, Bootstrap
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //  The timeout period of the connection.
                //  If this time is exceeded or the connection cannot be established, the connection fails.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // If no data is sent to the server within 15 seconds, a heartbeat request is sent
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder());
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(new NettyRpcClientHandler());
                    }
                });
        // 基于zookeeper的服务发现，采用SPI机制获取
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        // 未处理的请求容器
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        // 已建立的连接容器
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * connect server and get the channel ,so that you can send rpc message to server
     *
     * @param inetSocketAddress server address
     * @return the channel
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        // build return value
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // get server address
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // get  server address related channel
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // put unprocessed request
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE).build();
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }

        return resultFuture;
    }

    /**
     * @param inetSocketAddress:
     * @return: io.netty.channel.Channel
     * @author: lihen
     * @date: 2022/8/14 15:33
     * @description: 获取与对应服务提供者的连接
     */
    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        // 先尝试从缓存中获取连接
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            // 若为空则重新建立连接
            channel = doConnect(inetSocketAddress);
            // 添加到缓存中
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
