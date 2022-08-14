package github.javaguide.registry.zk;

import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.loadbalance.LoadBalance;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.registry.zk.util.CuratorUtils;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * service discovery based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年06月01日 15:16:00
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {

    // 负载均衡插件
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    /**
     * @param rpcRequest:
     * @return: java.net.InetSocketAddress
     * @author: victor2022
     * @date: 2022/8/13 下午10:45
     * @description: 查找服务列表，找到自己所需的服务
     * dubbo实现了服务列表的主动拉取(第一次调用某服务时)和自动推送(服务列表发生变化时)
     * TODO 没有实现服务列表的自动推送
     */
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        // 获取服务名
        String rpcServiceName = rpcRequest.getRpcServiceName();
        // zk服务端
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        // 找到对应服务的列表
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        // 若服务列表为空则报错
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // load balancing
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
