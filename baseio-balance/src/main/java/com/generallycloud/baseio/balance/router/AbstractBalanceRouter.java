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
package com.generallycloud.baseio.balance.router;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.generallycloud.baseio.balance.BalanceFacadeSocketSession;
import com.generallycloud.baseio.balance.BalanceReverseSocketSession;

public abstract class AbstractBalanceRouter implements BalanceRouter{

	private ConcurrentMap<Long, BalanceFacadeSocketSession> clients = new ConcurrentHashMap<>();

	@Override
	public void addClientSession(BalanceFacadeSocketSession session) {
		this.clients.put(session.getToken(), session);
	}

	@Override
	public BalanceFacadeSocketSession getClientSession(Long token) {
		return clients.get(token);
	}

	@Override
	public void removeClientSession(BalanceFacadeSocketSession session) {
		this.clients.remove(session.getToken());
	}
	
	@Override
	public BalanceReverseSocketSession getRouterSession(BalanceFacadeSocketSession session) {
		return session.getReverseSocketSession();
	}
}