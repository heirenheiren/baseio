/*
 * Copyright 2015-2017 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.generallycloud.baseio.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.generallycloud.baseio.common.Logger;
import com.generallycloud.baseio.common.LoggerFactory;

public class ThreadEventLoop extends AbstractEventLoop implements ExecutorEventLoop {

	private static Logger			logger	= LoggerFactory.getLogger(ThreadEventLoop.class);

	private ExecutorEventLoopGroup	executorEventLoopGroup;

	public ThreadEventLoop(ExecutorEventLoopGroup eventLoopGroup, int eventQueueSize) {
		this.jobs = new ListQueueABQ<Runnable>(eventQueueSize);
		this.executorEventLoopGroup = eventLoopGroup;
	}

	private ListQueue<Runnable>	jobs;
	
	private AtomicBoolean dispathLock = new AtomicBoolean();

	@Override
	protected void doLoop() {

		try {

			Runnable runnable = jobs.poll(32);

			if (runnable == null) {
				return;
			}

			runnable.run();

		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void dispatch(Runnable job) throws RejectedExecutionException{
		
		if (!dispathLock.compareAndSet(false, true)) {
			return;
		}
		
		try {
			if (!isRunning() || !jobs.offer(job)) {
				throw new RejectedExecutionException();
			}
		} finally {
			dispathLock.set(false);
		}
	}

	//FIXME __与dispatch互斥
	@Override
	protected void doStop() {
		
		for(;!dispathLock.compareAndSet(false, true);){
		}
		
		for(;;){
			
			Runnable runnable = jobs.poll();
			
			if (runnable == null) {
				break;
			}
			
			try {
				runnable.run();
			} catch (Throwable e) {
				logger.errorDebug(e);
			}
		}
		
		dispathLock.set(false);
		
		super.doStop();
	}

	@Override
	public ExecutorEventLoopGroup getEventLoopGroup() {
		return executorEventLoopGroup;
	}

}