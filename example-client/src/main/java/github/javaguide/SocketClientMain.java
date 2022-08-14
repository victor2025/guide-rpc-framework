package github.javaguide;

import github.javaguide.config.RpcServiceConfig;
import github.javaguide.proxy.RpcClientProxy;
import github.javaguide.remoting.transport.RpcRequestTransport;
import github.javaguide.remoting.transport.socket.SocketRpcClient;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
public class SocketClientMain {
    public static void main(String[] args) {
        // 创建客户端
        RpcRequestTransport rpcRequestTransport = new SocketRpcClient();
        // 创建服务配置
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        // 创建代理对象
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcRequestTransport, rpcServiceConfig);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        // 记录当前时间
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            String hello = helloService.hello(new Hello("111", "222"));
            System.out.println(hello);
        }
        System.out.println((System.currentTimeMillis()-t0)/1000);
    }
}
