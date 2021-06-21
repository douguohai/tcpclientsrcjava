/*
 * Copyright (C) 2020  即时通讯网(52im.net) & Jack Jiang.
 * The MobileIMSDK_TCP (MobileIMSDK v5.x TCP版) Project.
 * All rights reserved.
 *
 * > Github地址：https://github.com/JackJiang2011/MobileIMSDK
 * > 文档地址：  http://www.52im.net/forum-89-1.html
 * > 技术社区：  http://www.52im.net/
 * > 技术交流群：320837163 (http://www.52im.net/topic-qqgroup.html)
 * > 作者公众号：“即时通讯技术圈】”，欢迎关注！
 * > 联系作者：  http://www.52im.net/thread-2792-1-1.html
 *
 * "即时通讯网(52im.net) - 即时通讯开发者社区!" 推荐开源工程。
 *
 * LocalUDPSocketProvider.java at 2020-8-6 14:24:51, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.CharsetUtil;
import net.x52im.mobileimsdk.ClientCoreSDK;
import net.x52im.mobileimsdk.conf.ConfigEntity;
import net.x52im.mobileimsdk.utils.Log;
import net.x52im.mobileimsdk.utils.MBObserver;

public class LocalSocketProvider {
    private final static String TAG = LocalSocketProvider.class.getSimpleName();
    /**
     * 4 bytes
     */
    public static int TCP_FRAME_FIXED_HEADER_LENGTH = 4;
    /**
     * 6K bytes
     */
    public static int TCP_FRAME_MAX_BODY_LENGTH = 6 * 1024;
    private static LocalSocketProvider instance = null;

    private Bootstrap bootstrap = null;
    private Channel localSocket = null;
    private ChannelFuture localConnectingFuture = null;
    private MBObserver connectionDoneObserver;

    public static LocalSocketProvider getInstance() {
        if (instance == null) {
            instance = new LocalSocketProvider();
        }
        return instance;
    }

    private LocalSocketProvider() {
        //
    }

    /**
     * 初始化服务端链接建立
     */
    private void initLocalBootstrap() {
        try {
            EventLoopGroup group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class);
            bootstrap.handler(new TCPChannelInitializerHandler());

            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            // 10 * 1000);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000);
        } catch (Exception e) {
            Log.w(TAG, "localSocket初始化时出错，原因是：" + e.getMessage(), e);
        }
    }

    /**
     * 设置回调
     *
     * @param connectionDoneObserver
     */
    public void setConnectionDoneObserver(MBObserver connectionDoneObserver) {
        this.connectionDoneObserver = connectionDoneObserver;
    }

    /**
     * 重置socket长链接
     *
     * @return
     */
    public Channel resetLocalSocket() {
        try {
            closeLocalSocket();
            initLocalBootstrap();
            tryConnectToHost();
            return localSocket;
        } catch (Exception e) {
            Log.w(TAG, "【IMCORE-TCP】重置localSocket时出错，原因是：" + e.getMessage(), e);
            closeLocalSocket();
            return null;
        }
    }

    /**
     * 尝试和服务端主机获取链接
     *
     * @return
     */
    private boolean tryConnectToHost() {
        if (ClientCoreSDK.DEBUG) {
            Log.d(TAG, "【IMCORE-TCP】tryConnectToHost并获取connection开始了...");
        }

        try {
            // .sync()
            ChannelFuture cf = bootstrap.connect(ConfigEntity.serverIP, ConfigEntity.serverPort);
            this.localSocket = cf.channel();
            this.localConnectingFuture = cf;

            cf.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isDone()) {
                        if (f.isCancelled()) {
                            Log.w(TAG, "【IMCORE-tryConnectToHost-异步回调】Connection attempt cancelled by user");
                        } else if (!f.isSuccess()) {
                            Log.w(TAG, "【IMCORE-tryConnectToHost-异步回调】连接失败，原因是：", f.cause());
                        } else {
                            Log.i(TAG, "【IMCORE-tryConnectToHost-异步回调】Connection established successfully");
                        }

                        if (LocalSocketProvider.this.connectionDoneObserver != null) {
                            connectionDoneObserver.update(f.isSuccess(), null);
                            LocalSocketProvider.this.connectionDoneObserver = null;
                        }
                    }
                }
            });

            this.localSocket.closeFuture().addListener(
                    new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            Log.i(TAG, "【IMCORE-TCP】channel优雅退出开始。。。");

                            if (future.channel() != null) {
                                future.channel().eventLoop().shutdownGracefully();
                            }

                            LocalSocketProvider.this.localSocket = null;
                            Log.i(TAG, "【IMCORE-TCP】channel优雅退出结束。");
                        }
                    });

            if (ClientCoreSDK.DEBUG) {
                Log.d(TAG, "【IMCORE-TCP】tryConnectToHost并获取connectio已完成。 .... continue ...");
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, String.format("【IMCORE-TCP】连接Server(IP[%s],PORT[%s])失败", ConfigEntity.serverIP, ConfigEntity.serverPort), e);
            return false;
        }
    }

    /**
     * 判断本地服务状态
     *
     * @return 准备好返回true 不好返回false
     */
    public boolean isLocalSocketReady() {
        return localSocket != null && localSocket.isActive();
    }

    /**
     * 获取本地socket远程链接对象
     *
     * @return
     */
    public Channel getLocalSocket() {
        if (isLocalSocketReady()) {
            // if(ClientCoreSDK.DEBUG)
            // Log.d(TAG,
            // "【IMCORE-TCP】isLocalSocketReady()==true，直接返回本地socket引用哦。");
            return localSocket;
        } else {
            // if(ClientCoreSDK.DEBUG)
            // Log.d(TAG,
            // "【IMCORE-TCP】isLocalSocketReady()==false，需要先resetLocalSocket()...");
            return resetLocalSocket();
        }
    }

    public void closeLocalSocket() {
        this.closeLocalSocket(true);
    }

    public void closeLocalSocket(boolean silent) {
        if (ClientCoreSDK.DEBUG && !silent) {
            Log.d(TAG, "【IMCORE-TCP】正在closeLocalSocket()...");
        }

        if (this.localConnectingFuture != null) {
            try {
                this.localConnectingFuture.cancel(true);
                this.localConnectingFuture = null;
            } catch (Exception e) {
                Log.w(TAG, "【IMCORE-TCP】在closeLocalSocket方法中试图释放localConnectingFuture资源时：", e);
            }
        }

        if (this.bootstrap != null) {
            try {
                this.bootstrap.config().group().shutdownGracefully();
                this.bootstrap = null;
            } catch (Exception e) {
                Log.w(TAG, "【IMCORE-TCP】在closeLocalSocket方法中试图释放bootstrap资源时：", e);
            }
        }

        if (this.localSocket != null) {
            try {
                this.localSocket.close();
                this.localSocket = null;
            } catch (Exception e) {
                Log.w(TAG, "【IMCORE-TCP】在closeLocalSocket方法中试图释放localSocket资源时：", e);
            }
        } else {
            if (!silent) {
                Log.d(TAG, "【IMCORE-TCP】Socket处于未初化状态（可能是您还未登陆），无需关闭。");
            }
        }
    }

    /**
     * netty 消息处理类，添加编码和解码
     */
    private class TCPChannelInitializerHandler extends ChannelInitializer<Channel> {
        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(TCP_FRAME_FIXED_HEADER_LENGTH + TCP_FRAME_MAX_BODY_LENGTH, 0, TCP_FRAME_FIXED_HEADER_LENGTH, 0, TCP_FRAME_FIXED_HEADER_LENGTH));
            pipeline.addLast("frameEncoder", new LengthFieldPrepender(TCP_FRAME_FIXED_HEADER_LENGTH));
            pipeline.addLast(TcpClientHandler.class.getSimpleName(), new TcpClientHandler());
        }
    }

    /**
     * 具体消息处理逻辑
     */
    private class TcpClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final String TAG = TcpClientHandler.class.getSimpleName();

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            if (ClientCoreSDK.DEBUG) {
                Log.d(TAG, "【IMCORE-netty-channelActive】连接已成功建立！(isLocalUDPSocketReady=" + isLocalSocketReady() + ")");
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            if (ClientCoreSDK.DEBUG) {
                Log.d(TAG, "【IMCORE-netty-channelInactive】连接已断开。。。。(isLocalUDPSocketReady="
                        + isLocalSocketReady() + ", ClientCoreSDK.connectedToServer="
                        + ClientCoreSDK.getInstance().isConnectedToServer() + ")");
            }

            if (ClientCoreSDK.getInstance().isConnectedToServer()) {
                if (ClientCoreSDK.DEBUG) {
                    Log.d(TAG, "【IMCORE-netty-channelInactive】连接已断开，立即提前进入框架的“通信通道”断开处理逻辑(而不是等心跳线程探测到，那就已经比较迟了)......");
                }
                KeepAliveDaemon.getInstance().notifyConnectionLost();
            }
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
            if (ClientCoreSDK.DEBUG) {
                Log.d(TAG, "【IMCORE-netty-channelRead0】【NOTE】>>>>>> 收到消息(原始内容)：" + buf.toString(CharsetUtil.UTF_8));
            }

            byte[] req = new byte[buf.readableBytes()];
            buf.readBytes(req);
            LocalDataReciever.getInstance().handleProtocal(req);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (ClientCoreSDK.DEBUG) {
                Log.w(TAG, "【IMCORE-netty-exceptionCaught】异常被触发了，原因是：" + cause.getMessage());
            }
            ctx.close();
        }
    }
}
