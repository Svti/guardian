Guardian
=====
![image](http://7xi2xp.com1.z0.glb.clouddn.com/guardian-logo.jpg)

**让你的程序快速接入Zookeeper来实现高可用**

# 介绍

- 使用curator 连接zookeeper，保证连接质量。
- 实时更新zookeeper的节点数据，保证消费和生产一致。
- 实现了服务治理，目前提供了 随机 和 轮询两种方式，后续会加入 `权重` 和 `熔断`两种消费方式。
- sample 是一个示例程序，欢迎大家下载使用。

# 说明

![image](http://7xi2xp.com1.z0.glb.clouddn.com/guardian-sequence.png)

# 使用

```java

//TODO 三个步骤

//生产者，初始化注册节点
ZookeeperRegistry zr = new ZookeeperRegistry ();
Zdata zdata = new Zdata();
zr.regist(zdata);


//消费者，继承ZookeeperDiscovery，等待回调

discover(Zdata provider);//可用的节点

destory();//一旦断开zookeeper，消费者也要断开，保持一致


//消费者如果成功消费，需要告诉zookeeper
consume(Zdata provider, String host, int port)//提供消费者是谁，以及自身的ip和端口

```

- 以上三步实现了，程序就能正常运行了。


# 测试效果

![image](http://7xi2xp.com1.z0.glb.clouddn.com/guardian.png)


# 其他
**后续会提交到中心仓库**
