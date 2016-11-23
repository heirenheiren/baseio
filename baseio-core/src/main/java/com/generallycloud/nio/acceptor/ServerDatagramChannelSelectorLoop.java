package com.generallycloud.nio.acceptor;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.generallycloud.nio.component.BaseContext;
import com.generallycloud.nio.component.DatagramChannelSelectorLoop;

public class ServerDatagramChannelSelectorLoop extends DatagramChannelSelectorLoop {

	public ServerDatagramChannelSelectorLoop(BaseContext context,SelectableChannel selectableChannel) {
		super(context,selectableChannel);
	}

	public Selector buildSelector(SelectableChannel channel) throws IOException {
		// 打开selector
		Selector selector = Selector.open();
		// 注册监听事件到该selector
		channel.register(selector, SelectionKey.OP_READ);
		
		return selector;
	}

	
	
}