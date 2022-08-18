package com.victor2022;

import com.victor2022.annotation.RpcScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
@RpcScan(basePackage = {"com.victor2022"})
public class NettyClientMain {
    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyClientMain.class);
        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            System.out.println(helloController.hello(new Hello("111","222")));
        }
        System.out.println((System.currentTimeMillis()-t0)/1000);
    }
}
