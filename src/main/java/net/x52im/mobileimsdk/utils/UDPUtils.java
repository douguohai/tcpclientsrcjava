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
 * UDPUtils.java at 2020-8-6 14:24:51, code by Jack Jiang.
 */
package net.x52im.mobileimsdk.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;

/**
 * 发送消息工具类
 */
public class UDPUtils {
    private final static String TAG = UDPUtils.class.getSimpleName();

    public static boolean send(Channel skt, byte[] d, int dataLen) {
        return UDPUtils.send(skt, d, dataLen, null);
    }

    public static synchronized boolean send(Channel skt, byte[] d, final int dataLen, final MBObserver resultObserver) {
        boolean sendSucess = false;
        if ((skt != null) && (d != null)) {
            Log.d(TAG, "【IMCORE-TCP】正在send()TCP数据时，[d.len=" + d.length + ",remoteIpAndPort=" + UDPUtils.getSocketAdressInfo(skt.remoteAddress()) + "]"
                    + "，本地端口是：" + UDPUtils.getSocketAdressInfo(skt.localAddress()) + " ...");

            if (skt.isActive()) {
                try {
                    ByteBuf to = Unpooled.copiedBuffer(d, 0, dataLen);
                    ChannelFuture cf = skt.writeAndFlush(to);//.sync();
                    sendSucess = true;
                    cf.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                Log.i(TAG, "[IMCORE-netty-send异步回调] >> 数据已成功发出[dataLen=" + dataLen + "].");
                            } else {
                                Log.w(TAG, "[IMCORE-netty-send异步回调] >> 数据发送失败！[dataLen=" + dataLen + "].");
                            }

                            if (resultObserver != null) {
                                resultObserver.update(future.isSuccess(), null);
                            }
                        }
                    });

                    return sendSucess;
                } catch (Exception e) {
                    Log.e(TAG, "【IMCORE】send方法中》》发送TCP数据报文时出错了，原因是：" + e.getMessage(), e);
                    if (resultObserver != null) {
                        resultObserver.update(false, null);
                    }
                }
            } else {
                Log.e(TAG, "【IMCORE】send方法中》》无法发送TCP数据，原因是：skt.isActive()=" + skt.isActive());
            }
        } else {
            Log.w(TAG, "【IMCORE】send方法中》》无效的参数：skt==null || d == null!");
        }

        if (resultObserver != null) {
            resultObserver.update(false, null);
        }

        return sendSucess;
    }

    public static String getSocketAdressInfo(SocketAddress s) {
        return (s != null) ? s.toString() : null;
    }
}
