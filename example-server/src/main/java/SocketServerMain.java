import com.victor2022.HelloService;
import com.victor2022.config.RpcServiceConfig;
import com.victor2022.remoting.transport.socket.SocketRpcServer;
import com.victor2022.serviceimpl.HelloServiceImpl;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
public class SocketServerMain {
    public static void main(String[] args) {
        // 创建服务提供者
        HelloService helloService = new HelloServiceImpl();
        // 创建socket服务对象
        SocketRpcServer socketRpcServer = new SocketRpcServer();
        // 创建rpc配置对象
        RpcServiceConfig rpcServiceConfig = new RpcServiceConfig();
        // 在配置对象中注册服务提供者
        rpcServiceConfig.setService(helloService);
        // 在服务对象中注册配置
        socketRpcServer.registerService(rpcServiceConfig);
        // 开启socketRpc服务
        socketRpcServer.start();
    }
}
