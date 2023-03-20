# 分析 Zookeeper 设置数据操作(SetData) 的处理流程

### 1.客户端封装请求

#### 1.1请求封装

请求入口方法为org.apache.zookeeper.ZooKeeper#setData(java.lang.String, byte[], int)

```java
    public Stat setData(final String path, byte[] data, int version) throws KeeperException, InterruptedException {
        final String clientPath = path;
        PathUtils.validatePath(clientPath);

        final String serverPath = prependChroot(clientPath);

        RequestHeader h = new RequestHeader();
        h.setType(ZooDefs.OpCode.setData);
        SetDataRequest request = new SetDataRequest();
        request.setPath(serverPath);
        request.setData(data);
        request.setVersion(version);
        SetDataResponse response = new SetDataResponse();
        ReplyHeader r = cnxn.submitRequest(h, request, response, null);
        if (r.getErr() != 0) {
            throw KeeperException.create(KeeperException.Code.get(r.getErr()), clientPath);
        }
        return response.getStat();
    }
```
这一步主要是封装请求，将节点path，数据data和数据版本version封装为`SetDataRequest`对象并提交请求
此方法为同步实现，同时zookeeper也提供异步版本

```java
    public void setData(final String path, byte[] data, int version, StatCallback cb, Object ctx) {
        final String clientPath = path;
        PathUtils.validatePath(clientPath);

        final String serverPath = prependChroot(clientPath);

        RequestHeader h = new RequestHeader();
        h.setType(ZooDefs.OpCode.setData);
        SetDataRequest request = new SetDataRequest();
        request.setPath(serverPath);
        request.setData(data);
        request.setVersion(version);
        SetDataResponse response = new SetDataResponse();
        cnxn.queuePacket(h, new ReplyHeader(), request, response, cb, clientPath, serverPath, ctx, null);
    }
```

主要区别在于异步通过手动设置回调的方式完成响应处理。

#### 1.2请求入队

上一步完成请求封装操作后，通过`org.apache.zookeeper.ClientCnxn#submitRequest`方法将请求入队

```java
    public ReplyHeader submitRequest(
        RequestHeader h,Record request,Record response,WatchRegistration watchRegistration,WatchDeregistration watchDeregistration) throws InterruptedException {
        ReplyHeader r = new ReplyHeader();
        Packet packet = queuePacket(h,r,request,response,null,null,null,null,watchRegistration,watchDeregistration);
        synchronized (packet) {
            if (requestTimeout > 0) {
                // Wait for request completion with timeout
                waitForPacketFinish(r, packet);
            } else {
                // Wait for request completion infinitely
                while (!packet.finished) {
                    packet.wait();
                }
            }
        }
        if (r.getErr() == Code.REQUESTTIMEOUT.intValue()) {
            sendThread.cleanAndNotifyState();
        }
        return r;
    }
```

`SetDataRequest`在这一步加入队列，并且等待(允许设置最大超时时间)服务端处理完成并响应。

```java
    public Packet queuePacket(
        RequestHeader h,ReplyHeader r,Record request,Record response,AsyncCallback cb,String clientPath,
        String serverPath,Object ctx,WatchRegistration watchRegistration,WatchDeregistration watchDeregistration) {
        Packet packet = null;
      
        packet = new Packet(h, r, request, response, watchRegistration);
        packet.cb = cb;
        packet.ctx = ctx;
        packet.clientPath = clientPath;
        packet.serverPath = serverPath;
        packet.watchDeregistration = watchDeregistration;

        synchronized (outgoingQueue) {
            if (!state.isAlive() || closing) {
                conLossPacket(packet);
            } else {
                // If the client is asking to close the session then
                // mark as closing
                if (h.getType() == OpCode.closeSession) {
                    closing = true;
                }
                outgoingQueue.add(packet);
            }
        }
        sendThread.getClientCnxnSocket().packetAdded();
        return packet;
    }
```

请求被进一步封装成`Packet`对象,并加入`outgoingQueue`，等`SendThread`线程发送给服务端

#### 1.3SendThread

`SendThread`线程承担了请求数据的发送工作，在其内部维护了一个`ClientCnxnSocket`对象，这是处理服务端交互的核心类库，Zookeeper提供了两种版本的实现

* org.apache.zookeeper.ClientCnxnSocketNIO 基于标准JavaNIO实现
* org.apache.zookeeper.ClientCnxnSocketNetty 基于Netty实现

这里我们主要聚集于`ClientCnxnSocketNIO`实现。

`ClientCnxnSocket`定义了`doTransport`方法实现传输细节，

```java
    void doTransport(
        int waitTimeOut,Queue<Packet> pendingQueue,ClientCnxn cnxn) throws IOException, InterruptedException {
        selector.select(waitTimeOut);
        Set<SelectionKey> selected;
        synchronized (this) {
            selected = selector.selectedKeys();
        }
        updateNow();
        for (SelectionKey k : selected) {
            SocketChannel sc = ((SocketChannel) k.channel());
            if ((k.readyOps() & SelectionKey.OP_CONNECT) != 0) {
                if (sc.finishConnect()) {
                    updateLastSendAndHeard();
                    updateSocketAddresses();
                    sendThread.primeConnection();
                }
            } else if ((k.readyOps() & (SelectionKey.OP_READ | SelectionKey.OP_WRITE)) != 0) {
                doIO(pendingQueue, cnxn);
            }
        }
        if (sendThread.getZkState().isConnected()) {
            if (findSendablePacket(outgoingQueue, sendThread.tunnelAuthInProgress()) != null) {
                enableWrite();
            }
        }
        selected.clear();
    }
```

可以看到在io管道处于可读写状态时，执行doIO方法。

doIO方法同时支持读写操作，这里暂时分析写操作

```java
void doIO(Queue<Packet> pendingQueue, ClientCnxn cnxn) throws InterruptedException, IOException {
        SocketChannel sock = (SocketChannel) sockKey.channel();
        if (sock == null) {
            throw new IOException("Socket is null!");
        }
        if (sockKey.isWritable()) {
            Packet p = findSendablePacket(outgoingQueue, sendThread.tunnelAuthInProgress());

            if (p != null) {
                updateLastSend();
                // If we already started writing p, p.bb will already exist
                if (p.bb == null) {
                    if ((p.requestHeader != null)
                        && (p.requestHeader.getType() != OpCode.ping)
                        && (p.requestHeader.getType() != OpCode.auth)) {
                        p.requestHeader.setXid(cnxn.getXid());
                    }
                    p.createBB();
                }
                sock.write(p.bb);
                if (!p.bb.hasRemaining()) {
                    sentCount.getAndIncrement();
                    outgoingQueue.removeFirstOccurrence(p);
                    if (p.requestHeader != null
                        && p.requestHeader.getType() != OpCode.ping
                        && p.requestHeader.getType() != OpCode.auth) {
                        synchronized (pendingQueue) {
                            pendingQueue.add(p);
                        }
                    }
                }
            }
        }
    }
```

步骤分析

1. 从`outgoingQueue`中，抓取允许发送的`Packet`对象
2. 生成xid，执行序列化，数据流写入到`ByteBuffer`中
3. `ByteBuffer`数据写入管道中，`Packet`加入`pendingQueue`队列，等待后续服务端完成响应时再取出



**总结：**

**在发送流程中，存在两个队列，`outgoingQueue`和`pendingQueue`.**

**客户端发送请求时，优先封装成`Packet`对象，写入至`outgoingQueue`队列，等待SendThread提取发送**

**当有可用的写通道时，从`outgoingQueue`中逐步取出`Packet`对象，生成xid和写入二进制流数据，提交给服务端**

**写入完成后，写入`pendingQueue`等待服务端响应，此时提交的顺序和入队的顺序是一致的。后续接受到的响应顺序也一定和出队的顺序是一致的。**

### 2.客户端序列化

上面分析了提交请求流程，在这里我们分析序列化操作。

在上面代码中，我们可以看到，请求执行序列化的时机是在`Packet`对象从`outgoingQueue`中取出时，通过`org.apache.zookeeper.ClientCnxn.Packet#createBB`执行

```java
        public void createBB() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BinaryOutputArchive boa = BinaryOutputArchive.getArchive(baos);
                boa.writeInt(-1, "len"); // We'll fill this in later
                if (requestHeader != null) {
                    requestHeader.serialize(boa, "header");
                }
                if (request instanceof ConnectRequest) {
                    request.serialize(boa, "connect");
                    // append "am-I-allowed-to-be-readonly" flag
                    boa.writeBool(readOnly, "readOnly");
                } else if (request != null) {
                    request.serialize(boa, "request");
                }
                baos.close();
                this.bb = ByteBuffer.wrap(baos.toByteArray());
                this.bb.putInt(this.bb.capacity() - 4);
                this.bb.rewind();
            } catch (IOException e) {
                LOG.warn("Unexpected exception", e);
            }
        }
```

除了一些常规header字段，其余字段的序列化操作由具体的`org.apache.jute.Record`对象实现，由于是`SetData`操作，我们关注`SetDataRequest`的代码实现

```java
    public void serialize(OutputArchive a_, String tag) throws IOException {
        a_.startRecord(this, tag);
        a_.writeString(this.path, "path");
        a_.writeBuffer(this.data, "data");
        a_.writeInt(this.version, "version");
        a_.endRecord(this, tag);
    }
```

可以看到，主要序列化节点路径path，二进制数据data和数据版本version

### 3.服务端接受请求

Zookeeper服务端接受请求的IO模型参考了Reactor模型，由`AcceptThread`控制连接建立和断开，`SelectorThread`控制数据读写

一个`AcceptThread`可以关联多个`SelectorThread`，接受到的请求数据采取轮训的方式交给`SelectorThread`

因此我们主要关注`SelectorThread`,在其内部，实现了`select`方法，用于处理请求

```java
        private void select() {
            try {
                selector.select();

                Set<SelectionKey> selected = selector.selectedKeys();
                ArrayList<SelectionKey> selectedList = new ArrayList<SelectionKey>(selected);
                Collections.shuffle(selectedList);
                Iterator<SelectionKey> selectedKeys = selectedList.iterator();
                while (!stopped && selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selected.remove(key);

                    if (!key.isValid()) {
                        cleanupSelectionKey(key);
                        continue;
                    }
                    if (key.isReadable() || key.isWritable()) {
                        handleIO(key);
                    } else {
                        LOG.warn("Unexpected ops in select {}", key.readyOps());
                    }
                }
            } catch (IOException e) {
                LOG.warn("Ignoring IOException while selecting", e);
            }
        }
```

同时，`SelectorThread`内部维护了一个`WorkerService`对象，每个`WorkerServic`e内部维护一个执行器数组，每当有请求接受时，轮询选择一个执行器接受请求

```java
//org.apache.zookeeper.server.WorkerService#schedule(org.apache.zookeeper.server.WorkerService.WorkRequest, long)    
public void schedule(WorkRequest workRequest, long id) {
        if (stopped) {
            workRequest.cleanup();
            return;
        }

        ScheduledWorkRequest scheduledWorkRequest = new ScheduledWorkRequest(workRequest);

        // If we have a worker thread pool, use that; otherwise, do the work
        // directly.
        int size = workers.size();
        if (size > 0) {
            try {
                // make sure to map negative ids as well to [0, size-1]
                int workerNum = ((int) (id % size) + size) % size;
                ExecutorService worker = workers.get(workerNum);
                worker.execute(scheduledWorkRequest);
            } catch (RejectedExecutionException e) {
                LOG.warn("ExecutorService rejected execution", e);
                workRequest.cleanup();
            }
        } else {
            // When there is no worker thread pool, do the work directly
            // and wait for its completion
            scheduledWorkRequest.run();
        }
    }
```

请求在执行前，会被封装成`ScheduledWorkRequest`，该方法会调用`WorkRequest`的doWork方法，最终会引导到Zookeeper的processPacket

```java
public void processPacket(ServerCnxn cnxn, ByteBuffer incomingBuffer) throws IOException {
    // We have the request, now process and setup for next
    InputStream bais = new ByteBufferInputStream(incomingBuffer);
    BinaryInputArchive bia = BinaryInputArchive.getArchive(bais);
    RequestHeader h = new RequestHeader();
    h.deserialize(bia, "header");
    cnxn.incrOutstandingAndCheckThrottle(h);
    incomingBuffer = incomingBuffer.slice();
    if (h.getType() == OpCode.auth) {
        //认证相关操作
        return;
    } else if (h.getType() == OpCode.sasl) {
        processSasl(incomingBuffer, cnxn, h);
    } else {
        if (!authHelper.enforceAuthentication(cnxn, h.getXid())) {
            // Authentication enforcement is failed
            // Already sent response to user about failure and closed the session, lets return
            return;
        } else {
            Request si = new Request(cnxn, cnxn.getSessionId(), h.getXid(), h.getType(), incomingBuffer, cnxn.getAuthInfo());
            int length = incomingBuffer.limit();
            if (isLargeRequest(length)) {
                // checkRequestSize will throw IOException if request is rejected
                checkRequestSizeWhenMessageReceived(length);
                si.setLargeRequestSize(length);
            }
            si.setOwner(ServerCnxn.me);
            submitRequest(si);
        }
    }
}
```

submitRequest就是最终的处理方法，到这里接受请求的部分已经完成，后续就是请求处理的逻辑。

### 4.服务端处理

Zookeeper服务端处理由`RequestProcessor`实现，其中，SetData操作由`org.apache.zookeeper.server.PrepRequestProcessor`对象完成

`PrepRequestProcessor`采用异步处理的方式,请求提交后，先进行入队操作，数据写入`submittedRequests`，然后外部启动守护线程循环处理`submittedRequests`

针对每个请求，请求逻辑代码位于`org.apache.zookeeper.server.PrepRequestProcessor#pRequest2Txn`

```java
private void pRequest2Txn(int type, long zxid, Request request, Record record, boolean deserialize) throws KeeperException, IOException, RequestProcessorException {
      try {
          switch (request.type) {
          //...其他操作
          case OpCode.setData:
            zks.sessionTracker.checkSession(request.sessionId, request.getOwner());
            SetDataRequest setDataRequest = (SetDataRequest) record;
            if (deserialize) {
                ByteBufferInputStream.byteBuffer2Record(request.request, setDataRequest);
            }
            path = setDataRequest.getPath();
            validatePath(path, request.sessionId);
            nodeRecord = getRecordForPath(path);
            zks.checkACL(request.cnxn, nodeRecord.acl, ZooDefs.Perms.WRITE, request.authInfo, path, null);
            zks.checkQuota(path, nodeRecord.data, setDataRequest.getData(), OpCode.setData);
            int newVersion = checkAndIncVersion(nodeRecord.stat.getVersion(), setDataRequest.getVersion(), path);
            request.setTxn(new SetDataTxn(path, setDataRequest.getData(), newVersion));
            nodeRecord = nodeRecord.duplicate(request.getHdr().getZxid());
            nodeRecord.stat.setVersion(newVersion);
            nodeRecord.stat.setMtime(request.getHdr().getTime());
            nodeRecord.stat.setMzxid(zxid);
            nodeRecord.data = setDataRequest.getData();
            nodeRecord.precalculatedDigest = precalculateDigest(
                    DigestOpCode.UPDATE, path, nodeRecord.data, nodeRecord.stat);
            setTxnDigest(request, nodeRecord.precalculatedDigest);
            addChangeRecord(nodeRecord);
          break;
          //其他操作
          }
      } catch (KeeperException e) {
         //...异常处理
      } catch (Exception e) {
         //...异常处理
      }
  }
```

 处理步骤

1. 读取节点path，校验是否具有更新权限
2. 检查传递的数据版本，并将版本号加1
3. 写入数据并返回响应



### 5.客户端响应处理

在上面提及过，`Packet`对象发送给服务端的同时，会加入`pendingQueue`队列处理，处理入口在doIO方法

```java
void doIO(Queue<Packet> pendingQueue, ClientCnxn cnxn) throws InterruptedException, IOException {
        SocketChannel sock = (SocketChannel) sockKey.channel();
        if (sock == null) {
            throw new IOException("Socket is null!");
        }
        if (sockKey.isReadable()) {
            int rc = sock.read(incomingBuffer);
            if (rc < 0) {
                throw new EndOfStreamException("Unable to read additional data from server sessionid 0x"
                                               + Long.toHexString(sessionId)
                                               + ", likely server has closed socket");
            }
            if (!incomingBuffer.hasRemaining()) {
                incomingBuffer.flip();
                if (incomingBuffer == lenBuffer) {
                    recvCount.getAndIncrement();
                    readLength();
                } else if (!initialized) {
                    readConnectResult();
                    enableRead();
                    if (findSendablePacket(outgoingQueue, sendThread.tunnelAuthInProgress()) != null) {
                        // Since SASL authentication has completed (if client is configured to do so),
                        // outgoing packets waiting in the outgoingQueue can now be sent.
                        enableWrite();
                    }
                    lenBuffer.clear();
                    incomingBuffer = lenBuffer;
                    updateLastHeard();
                    initialized = true;
                } else {
                    sendThread.readResponse(incomingBuffer);
                    lenBuffer.clear();
                    incomingBuffer = lenBuffer;
                    updateLastHeard();
                }
            }
        }
    }
```

接受到的二进制流数据存储在ByteBuffer中，并提交给`SendThread`。

在`SendThread#readResponse`中，定义了读取响应的具体逻辑



```java
        void readResponse(ByteBuffer incomingBuffer) throws IOException {
            ByteBufferInputStream bbis = new ByteBufferInputStream(incomingBuffer);
            BinaryInputArchive bbia = BinaryInputArchive.getArchive(bbis);
            ReplyHeader replyHdr = new ReplyHeader();

            replyHdr.deserialize(bbia, "header");
          
						//...省略部分代码

            Packet packet;
            synchronized (pendingQueue) {
                if (pendingQueue.size() == 0) {
                    throw new IOException("Nothing in the queue, but got " + replyHdr.getXid());
                }
                packet = pendingQueue.remove();
            }
            /*
             * Since requests are processed in order, we better get a response
             * to the first request!
             */
            try {
                if (packet.requestHeader.getXid() != replyHdr.getXid()) {
                    packet.replyHeader.setErr(KeeperException.Code.CONNECTIONLOSS.intValue());
                    throw new IOException("Xid out of order. Got Xid " + replyHdr.getXid()
                                          + " with err " + replyHdr.getErr()
                                          + " expected Xid " + packet.requestHeader.getXid()
                                          + " for a packet with details: " + packet);
                }

                packet.replyHeader.setXid(replyHdr.getXid());
                packet.replyHeader.setErr(replyHdr.getErr());
                packet.replyHeader.setZxid(replyHdr.getZxid());
                if (replyHdr.getZxid() > 0) {
                    lastZxid = replyHdr.getZxid();
                }
                if (packet.response != null && replyHdr.getErr() == 0) {
                    packet.response.deserialize(bbia, "response");
                }

                LOG.debug("Reading reply session id: 0x{}, packet:: {}", Long.toHexString(sessionId), packet);
            } finally {
                finishPacket(packet);
            }
        }
```

处理步骤如下



1. 执行反序列化，将二进制数据转换成`ReplyHeader`
2. 从`pendingQueue`中取出之前提交的`Packet`
3. 校验xid，如果不相等，则代表当前请求与当前响应不匹配，触发IOException
4. 相等，将`ReplyHeader`写入`Packet`中，并在`finishPacket`方法中将`Packet`中的状态设置为finish