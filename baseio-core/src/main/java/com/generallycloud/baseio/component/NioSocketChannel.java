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
package com.generallycloud.baseio.component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

import com.generallycloud.baseio.ClosedChannelException;
import com.generallycloud.baseio.buffer.ByteBuf;
import com.generallycloud.baseio.common.ReleaseUtil;
import com.generallycloud.baseio.protocol.ChannelFuture;

public class NioSocketChannel extends AbstractSocketChannel implements SelectorLoopEvent {

    private SocketChannel           channel;
    private volatile boolean        closing;
    private NioSocketChannelContext context;
    private SelectionKey            selectionKey;
    private SocketSelectorEventLoop selectorEventLoop;

    public NioSocketChannel(SocketSelectorEventLoop selectorLoop, SelectionKey selectionKey,
            int channelId) {
        super(selectorLoop, channelId);
        this.selectorEventLoop = selectorLoop;
        this.context = selectorLoop.getChannelContext();
        this.selectionKey = selectionKey;
        this.channel = (SocketChannel) selectionKey.channel();
    }

    @Override
    public void close() throws IOException {
        if (!isOpened()) {
            return;
        }
        if (closing && !inSelectorLoop()) {
            return;
        }
        ReentrantLock lock = getCloseLock();
        lock.lock();
        try {
            if (inSelectorLoop()) {
                if (!isOpened()) {
                    return;
                }
                physicalClose();
                return;
            } else {
                if (closing || !isOpened()) {
                    return;
                }
                closing = true;
                dispatchEvent(new CloseSelectorLoopEvent(this));
            }
        } finally {
            lock.unlock();
        }
    }

    protected void dispatchEvent(SelectorLoopEvent event) {
        this.selectorEventLoop.dispatch(event);
    }

    @Override
    protected void doFlush0() {
        selectorEventLoop.dispatch(this);
    }

    @Override
    public void fireEvent(SocketSelectorEventLoop selectorLoop) throws IOException {
        if (!isOpened()) {
            throw new ClosedChannelException("closed");
        }
        flush(selectorLoop);
    }

    protected void flush(SocketSelectorEventLoop selectorLoop) throws IOException {
        ChannelFuture f = writeFuture;
        if (f == null) {
            f = writeFutures.poll();
        }
        if (f == null) {
            return;
        }
        for (;;) {
            try {
                f.write(this);
            } catch (Throwable e) {
                writeFuture = null;
                ReleaseUtil.release(f);
                throw e;
            }
            if (!f.isWriteCompleted()) {
                writeFuture = f;
                interestWrite(selectionKey);
                return;
            }
            writeFutureLength(-f.getByteBufLimit());
            f.onSuccess(session);
            f = writeFutures.poll();
            if (f == null) {
                break;
            }
        }
        interestRead(selectionKey);
        writeFuture = null;
    }

    @Override
    public NioSocketChannelContext getContext() {
        return context;
    }

    @Override
    protected InetSocketAddress getLocalSocketAddress0() throws IOException {
        return (InetSocketAddress) channel.getLocalAddress();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return channel.getOption(name);
    }

    @Override
    protected InetSocketAddress getRemoteSocketAddress0() throws IOException {
        return (InetSocketAddress) channel.getRemoteAddress();
    }

    @Override
    protected SocketChannelThreadContext getSocketChannelThreadContext() {
        return selectorEventLoop;
    }

    private void interestRead(SelectionKey key) {
        int interestOps = key.interestOps();
        if (SelectionKey.OP_READ != interestOps) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void interestWrite(SelectionKey key) {
        if ((SelectionKey.OP_READ | SelectionKey.OP_WRITE) != key.interestOps()) {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }

    @Override
    public boolean isBlocking() {
        return channel.isBlocking();
    }

    // FIXME 这里有问题
    @Override
    protected void physicalClose() {
        opened = false;
        closeSSL();
        // 最后一轮 //FIXME once
        try {
            flush(selectorEventLoop);
        } catch (IOException e) {}
        releaseFutures();
        selectionKey.attach(null);
        try {
            channel.close();
        } catch (Exception e) {}
        selectionKey.cancel();
        fireClosed();
    }

    protected int read(ByteBuf buf) throws IOException {
        int length = read(buf.getNioBuffer());
        if (length > 0) {
            buf.reverse();
        }
        return length;
    }

    private int read(ByteBuffer buffer) throws IOException {
        return channel.read(buffer);
    }

    @Override
    public <T> void setOption(SocketOption<T> name, T value) throws IOException {
        channel.setOption(name, value);
    }

    @Override
    public void write(ByteBuf buf) throws IOException {
        int length = write(buf.getNioBuffer());
        if (length < 1) {
            return;
        }
        buf.reverse();
    }

    private int write(ByteBuffer buffer) throws IOException {
        return channel.write(buffer);
    }

}
