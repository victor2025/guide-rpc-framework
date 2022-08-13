package github.javaguide.remoting.transport.netty.server;

import github.javaguide.enums.CompressTypeEnum;
import github.javaguide.enums.RpcResponseCodeEnum;
import github.javaguide.enums.SerializationTypeEnum;
import github.javaguide.factory.SingletonFactory;
import github.javaguide.remoting.constants.RpcConstants;
import github.javaguide.remoting.dto.RpcMessage;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;
import github.javaguide.remoting.handler.RpcRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Customize the ChannelHandler of the server to process the data sent by the client.
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 *
 * @author shuang.kou
 * @createTime 2020年05月25日 20:44:00
 * 服务器入站处理器
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            // 若收到的信息属于Rpc信息
            // 解码后的rpc信息对象
            if (msg instanceof RpcMessage) {
                log.info("server receive msg: [{}] ", msg);
                // 获取类型
                byte messageType = ((RpcMessage) msg).getMessageType();
                // 创建返回信息
                RpcMessage rpcMessage = new RpcMessage();
                // 设置编码器，硬编码为Hessian？
                rpcMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
                // 设置压缩方式
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                // 若是心跳包
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    // 设置心跳包对应信息
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    // 设置普通rpc请求对应信息
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    // Execute the target method (the method the client needs to execute) and return the method result
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("server get result: %s", result.toString()));
                    // 设置为rpc返回类型
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    // 判断通道是否连接且可写
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        // 设置成功标记
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        // 设置返回信息
                        rpcMessage.setData(rpcResponse);
                    } else {
                        // 设置失败标记
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        // 设置返回信息
                        rpcMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                // 返回消息，若失败则关闭
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            //Ensure that ByteBuf is released, otherwise there may be memory leaks
            // msg在byteBuf中存储，若不释放的化会出现内存的泄露
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * @param ctx:
     * @param evt:
     * @return: void
     * @author: victor2022
     * @date: 2022/8/13 下午10:27
     * @description: 服务提供端的心跳机制实现
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
