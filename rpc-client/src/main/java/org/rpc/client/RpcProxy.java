package org.rpc.client;

import org.rpc.common.RpcRequest;
import org.rpc.common.RpcResponse;
import org.rpc.registry.ServiceDiscovery;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Created by xinghang on 17/9/22.
 * RPC 代理
 */
public class RpcProxy {
  @Autowired
  private String serverAddress;
  @Autowired
  private ServiceDiscovery serviceDiscovery;

  @SuppressWarnings("unchecked")
  public <T> T create(Class<?> interfaceClass) {
    return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
      new Class<?>[]{interfaceClass}, new InvocationHandler() {
        public Object invoke(Object proxy, Method method,
                             Object[] args) throws Throwable {
          //创建RpcRequest，封装被代理类的属性
          RpcRequest request = new RpcRequest();
          request.setRequestId(UUID.randomUUID().toString());
          //拿到声明这个方法的业务接口名称
          request.setClassName(method.getDeclaringClass()
            .getName());
          request.setMethodName(method.getName());
          request.setParameterTypes(method.getParameterTypes());
          request.setParameters(args);
          //查找服务
          if (serviceDiscovery != null) {
            serverAddress = serviceDiscovery.discover();
          }
          //随机获取服务的地址
          String[] array = serverAddress.split(":");
          String host = array[0];
          int port = Integer.parseInt(array[1]);
          //创建Netty实现的RpcClient，链接服务端
          RpcClient client = new RpcClient(host, port);
          //通过netty向服务端发送请求
          RpcResponse response = client.send(request);
          //返回信息
          if (response.isError()) {
            throw response.getError();
          } else {
            return response.getResult();
          }
        }
      }
    );
  }

}