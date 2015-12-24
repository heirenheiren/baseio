package com.gifisan.mtp.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gifisan.mtp.AbstractLifeCycle;
import com.gifisan.mtp.FlushedException;
import com.gifisan.mtp.LifeCycle;
import com.gifisan.mtp.common.FileUtil;
import com.gifisan.mtp.common.LifeCycleUtil;
import com.gifisan.mtp.common.StringUtil;
import com.gifisan.mtp.server.Request;
import com.gifisan.mtp.server.Response;
import com.gifisan.mtp.server.ServletAccept;
import com.gifisan.mtp.server.context.ServletContext;
import com.gifisan.mtp.servlet.GenericServlet;
import com.gifisan.mtp.servlet.MTPFilterService;
import com.gifisan.mtp.servlet.MTPFilterServiceImpl;
import com.gifisan.mtp.servlet.impl.ErrorServlet;
import com.gifisan.mtp.servlet.impl.StopServerServlet;

public final class ServletService extends AbstractLifeCycle implements ServletAccept , LifeCycle{

	private ServletContext context = null;
	
	private final Logger logger = LoggerFactory.getLogger(ServletService.class);
	
	public ServletService(ServletContext context) {
		this.context = context;
	}
	
	private MTPFilterService service = null;

	private Map<String, GenericServlet> servlets = new LinkedHashMap<String, GenericServlet>();
	
	private Map<String, GenericServlet> errorServlets = new HashMap<String, GenericServlet>();
	
	public void accept(Request request, Response response) throws IOException{
		try {
			//TODO
			if (service.doFilter(request, response)) {
				return;
			}
		} catch (FlushedException e) {
			e.printStackTrace();
		} catch (MTPChannelException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
			this.acceptException(e, request, response);
		} catch (Exception e) {
			e.printStackTrace();
			this.acceptException(e, request, response);
		}
		
		this.acceptServlet(request, response);
	}
	
	public void acceptServlet(Request request, Response response) throws IOException {
		String serviceName = request.getServiceName();
		if (StringUtil.isNullOrBlank(serviceName)) {
			this.accept404(request, response);
		}else{
			this.acceptNormal(serviceName,request, response);
		}
	}
	
	private void acceptNormal0(ServletAccept servlet,Request request, Response response) throws IOException{
		try {
			servlet.accept(request, response);
		} catch (FlushedException e) {
			e.printStackTrace();
		} catch (MTPChannelException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
			this.acceptException(e, request, response);
		} catch (Exception e) {
			e.printStackTrace();
			this.acceptException(e, request, response);
		}
		
	}
	
	
	private void acceptNormal(String serviceName,Request request, Response response) throws IOException  {
		ServletAccept servlet = servlets.get(serviceName);
		if (servlet == null) {
			servlet = this.errorServlets.get(serviceName);
			if (servlet == null) {
				this.accept404(request, response);
			}else{
				this.acceptNormal0(servlet, request, response);
			}
		}else{
			this.acceptNormal0(servlet, request, response);
		}
	}
	
	private void acceptException(Exception exception,Request request, Response response) throws IOException{
		ErrorServlet servlet = new ErrorServlet(exception);
		try {
			servlet.accept(request, response);
		} catch(IOException e){
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void doStart() throws Exception {
		this.loadServlets(context);
		this.servlets.put(StopServerServlet.SERVICE_NAME, new StopServerServlet());
		this.service = new MTPFilterServiceImpl(context,this);
		this.service.start();
		this.initialize();
		
	}
	
	
	private void initialize(){
		Set<Entry<String, GenericServlet>> entries = servlets.entrySet();
		for(Entry<String, GenericServlet> entry : entries){
			GenericServlet servlet = entry.getValue();
			try {
				servlet.start();
			} catch (Exception e) {
				e.printStackTrace();
				String serviceName = entry.getKey();
				ErrorServlet error = new ErrorServlet(e);
				this.errorServlets.put(serviceName, error);
			}
		}
		
		Set<String> errorKeys = errorServlets.keySet();
		for(String key : errorKeys){
			this.servlets.remove(key);
		}
	}
	
	
	private void loadServlets(ServletContext context) {
		try {
			String str = FileUtil.readContentByCls("servlets.config", "UTF-8");
			JSONArray jArray = JSONObject.parseArray(str);
			for (int i = 0; i < jArray.size(); i++) {
				JSONObject object = jArray.getJSONObject(i);
				String clazz = object.getString("class");
				String serviceName = object.getString("serviceName");
				ServletConfig config = new ServletConfig();
				Map<String, Object> map = toMap(object);
				config.setConfig(map);
				try {
					GenericServlet servlet = (GenericServlet) Class.forName(
							clazz).newInstance();
					this.servlets.put(serviceName, servlet);
					servlet.setConfig(config);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<String, Object> toMap(JSONObject jsonObject) {
		Map<String, Object> result = new HashMap<String, Object>();
		Set enteys = jsonObject.entrySet();
		Iterator iterator = enteys.iterator();
		while (iterator.hasNext()) {
				Entry e = (Entry) iterator.next();
				String key = (String) e.getKey();
				Object value = e.getValue();
				result.put(key, value);
		}
		return result;
	}
	

	protected void doStop() throws Exception {
		LifeCycleUtil.stop(service);
		synchronized (servlets) {
			Set<Entry<String, GenericServlet>> entries = servlets.entrySet();
			for(Entry<String, GenericServlet> entry : entries){
				GenericServlet servlet = entry.getValue();
				try {
					LifeCycleUtil.stop(servlet);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void accept404(Request request,Response response) throws IOException{
		String serviceName = request.getServiceName();
		logger.info("[MTPServer] 未发现命令："+serviceName);
		response.write("404 not found service :".getBytes());
		if (!StringUtil.isNullOrBlank(serviceName)) {
			response.write(request.getServiceName().getBytes());
		}
		response.flush();
	}

}
