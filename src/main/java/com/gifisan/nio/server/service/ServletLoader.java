package com.gifisan.nio.server.service;

import com.gifisan.nio.LifeCycle;
import com.gifisan.nio.component.HotDeploy;
import com.gifisan.nio.server.FilterAcceptor;

public interface ServletLoader extends LifeCycle, HotDeploy {

	public abstract FilterAcceptor getServlet(String serviceName);

}