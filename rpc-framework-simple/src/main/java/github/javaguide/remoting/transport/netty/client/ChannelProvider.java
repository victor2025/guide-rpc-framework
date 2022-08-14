package github.javaguide.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * store and get Channel object
 *
 * @author shuang.kou
 * @createTime 2020年05月29日 16:36:00
 */
@Slf4j
public class ChannelProvider {

    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    /**
     * @param inetSocketAddress:
     * @return: io.netty.channel.Channel
     * @author: lihen
     * @date: 2022/8/14 15:31
     * @description: 按照服务提供者的地址获取对应channel
     */
    public Channel get(InetSocketAddress inetSocketAddress) {
        // 以服务提供者的地址为key
        String key = inetSocketAddress.toString();
        // determine if there is a connection for the corresponding address
        if (channelMap.containsKey(key)) {
            // 按照key获取对应的channel
            Channel channel = channelMap.get(key);
            // if so, determine if the connection is available, and if so, get it directly
            if (channel != null && channel.isActive()) {
                // 若该连接可用，则返回
                return channel;
            } else {
                // 若该连接不可用，则从缓存中删除
                channelMap.remove(key);
            }
        }
        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }
}
