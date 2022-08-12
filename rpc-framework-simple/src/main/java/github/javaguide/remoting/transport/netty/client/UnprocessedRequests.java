package github.javaguide.remoting.transport.netty.client;

import github.javaguide.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * unprocessed requests by the server.
 *
 * @author shuang.kou
 * @createTime 2020年06月04日 17:30:00
 * 记录已经发出去但是还没有返回的rpc请求
 */
public class UnprocessedRequests {

    // 采用requestId-completableFuture的形式存储发出去的请求
    private static final Map<String, CompletableFuture<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        // 获取本地已发出的请求的completableFuture对象
        CompletableFuture<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            // 响应调用者
            future.complete(rpcResponse);
        } else {
            // 说明本地发出请求时就出现了错误，没有成功放入completableFuture对象
            throw new IllegalStateException();
        }
    }
}
