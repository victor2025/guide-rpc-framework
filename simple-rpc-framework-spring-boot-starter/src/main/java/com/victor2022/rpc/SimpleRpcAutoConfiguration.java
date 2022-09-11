package com.victor2022.rpc;

import com.victor2022.remoting.transport.netty.server.NettyRpcServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author victor2022
 * @creat 2022/9/11 15:03
 */
@Configuration
@ConditionalOnClass(NettyRpcServer.class)
public class SimpleRpcAutoConfiguration {

    @Autowired
    private NettyRpcServer nettyRpcServer;

    @Bean
    public NettyRpcServer getRpcServer(){
        return nettyRpcServer;
    }
}
