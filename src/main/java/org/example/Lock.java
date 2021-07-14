package org.example;

import org.apache.zookeeper.KeeperException;

import java.util.concurrent.TimeUnit;

public interface Lock {
	void acquire() throws Exception;


	void acquire(long tryTime, TimeUnit timeUnit);


	void release() throws Exception;


}
