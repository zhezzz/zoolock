package org.example;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class ZooLock implements Lock {

	private ZooKeeper zooKeeper;
	private String path;
	private Thread owningThread;
	private String lockPrefix = "lock-";
	private AtomicInteger lockCount = new AtomicInteger(0);
	private String lockPath;


	public ZooLock(ZooKeeper zooKeeper, String path) {
		this.zooKeeper = zooKeeper;
		this.path = path;
	}

	@Override
	public void acquire() throws Exception {
		Thread currentThread = Thread.currentThread();
		//一个FairLock对象只能由一个对象占用，
		if (owningThread != null && !currentThread.equals(owningThread)) {
			throw new Exception("");
		}
		if (currentThread.equals(owningThread)) {
			lockCount.incrementAndGet();
			return;
		}
		String lockPath = addLockPath();
		List<String> childList = this.zooKeeper.getChildren(this.path, false);
		Collections.sort(childList);
		String lockName = lockPath.substring(lockPath.lastIndexOf("/") + 1);
		int lockIndex = childList.indexOf(lockName);
		if (lockIndex == 0) {
			this.owningThread = currentThread;
			lockCount.incrementAndGet();
			return;
		}
		String preLockPath = this.path + "/" + childList.get(lockIndex - 1);
		this.owningThread = currentThread;
		//TODO 可能由于前一个节点掉线导致事件触发，如果当前不是第一个节点，需重新向前一个结点注册Watcher
		this.zooKeeper.addWatch(preLockPath, event -> LockSupport.unpark(this.owningThread), AddWatchMode.PERSISTENT);
		LockSupport.park();
	}

	public String addLockPath() throws InterruptedException, KeeperException {
		createPath(this.path);
		String lockPath = this.zooKeeper.create(this.path + "/" + this.lockPrefix, "".getBytes(StandardCharsets.UTF_8), Collections.singletonList(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE)), CreateMode.EPHEMERAL_SEQUENTIAL);
		this.lockPath = lockPath;
		return lockPath;
	}

	public void createPath(String path) throws InterruptedException, KeeperException {
		String[] paths = path.split("/");
		String currPath = "";
		for (int i = 1; i < paths.length; i++) {
			currPath = currPath + "/" + paths[i];
//			if (this.zooKeeper.exists(currPath, false) != null) {
//				continue;
//			}
			try {
				this.zooKeeper.create(currPath, "".getBytes(StandardCharsets.UTF_8), Collections.singletonList(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE)), CreateMode.PERSISTENT);
			} catch (InterruptedException | KeeperException e) {
				if (this.zooKeeper.exists(currPath, false) != null) {
					continue;
				}
				throw e;
			}
		}
	}

	@Override
	public void acquire(long tryTime, TimeUnit timeUnit) {
	}

	@Override
	public void release() throws Exception {
		int curr = this.lockCount.decrementAndGet();
		if (curr == 0) {
			this.zooKeeper.delete(this.lockPath, -1);
		}
	}
}
