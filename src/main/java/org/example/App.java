package org.example;

import org.apache.zookeeper.*;

/**
 * Hello world!
 */
public class App {
	public static void main(String[] args) throws Exception {
		ZooKeeper zoo = new ZooKeeper("172.26.23.219:2181", 30000, watchedEvent -> System.out.println(watchedEvent.toString()));

		ZooLock fairLock = new ZooLock(zoo,"/lock/account/user1");
		fairLock.acquire();

		new Thread(() -> {
			try {
				fairLock.acquire();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();




//
//		zoo.create("/zoolock", "".getBytes(StandardCharsets.UTF_8), Collections.singletonList(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE)), CreateMode.PERSISTENT);
//		System.out.println(zoo.getState());
//		System.out.println(zoo.getSessionId());

//		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
//		CuratorFramework client = CuratorFrameworkFactory.newClient("172.29.117.78:2181", retryPolicy);
//		client.start();
//
//		InterProcessMutex lock = new InterProcessMutex(client, "/acquire/account/user1");
//		lock.acquire();
//
//		lock.release();
//
//
//
//		client.close();

//        System.out.println( "Hello World!" );
	}
}
