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
 * ConfigEntity.java at 2020-8-6 14:24:51, code by Jack Jiang.
 */
package net.x52im.mobileimsdk;

import net.x52im.mobileimsdk.event.ChatBaseEvent;
import net.x52im.mobileimsdk.event.ChatMessageEvent;
import net.x52im.mobileimsdk.event.MessageQoSEvent;
import net.x52im.mobileimsdk.core.AutoReLoginDaemon;
import net.x52im.mobileimsdk.core.KeepAliveDaemon;
import net.x52im.mobileimsdk.core.LocalSocketProvider;
import net.x52im.mobileimsdk.core.QoS4ReciveDaemon;
import net.x52im.mobileimsdk.core.QoS4SendDaemon;

public class ClientCoreSDK {
    private final static String TAG = ClientCoreSDK.class.getSimpleName();

    public static boolean DEBUG = true;
    public static boolean autoReLogin = true;
    private static ClientCoreSDK instance = null;

    private boolean _init = false;
    private boolean connectedToServer = true;
    private boolean loginHasInit = false;
    private String currentLoginUserId = null;
    private String currentLoginToken = null;
    private String currentLoginExtra = null;

    private ChatBaseEvent chatBaseEvent = null;
    private ChatMessageEvent chatMessageEvent = null;
    private MessageQoSEvent messageQoSEvent = null;

    public static ClientCoreSDK getInstance() {
        if (instance == null) {
            instance = new ClientCoreSDK();
        }
        return instance;
    }

    private ClientCoreSDK() {

    }

    public void init() {
        if (!_init) {
            _init = true;
        }
    }

    public void release() {
        LocalSocketProvider.getInstance().closeLocalSocket();
        AutoReLoginDaemon.getInstance().stop();
        QoS4SendDaemon.getInstance().stop();
        KeepAliveDaemon.getInstance().stop();

        QoS4ReciveDaemon.getInstance().stop();

        QoS4SendDaemon.getInstance().clear();
        QoS4ReciveDaemon.getInstance().clear();

        _init = false;
        this.setLoginHasInit(false);
        this.setConnectedToServer(false);
    }

    public String getCurrentLoginUserId() {
        return currentLoginUserId;
    }

    public ClientCoreSDK setCurrentLoginUserId(String currentLoginUserId) {
        this.currentLoginUserId = currentLoginUserId;
        return this;
    }

    public String getCurrentLoginToken() {
        return currentLoginToken;
    }

    public void setCurrentLoginToken(String currentLoginToken) {
        this.currentLoginToken = currentLoginToken;
    }

    public String getCurrentLoginExtra() {
        return currentLoginExtra;
    }

    public ClientCoreSDK setCurrentLoginExtra(String currentLoginExtra) {
        this.currentLoginExtra = currentLoginExtra;
        return this;
    }

    public boolean isLoginHasInit() {
        return loginHasInit;
    }

    public ClientCoreSDK setLoginHasInit(boolean loginHasInit) {
        this.loginHasInit = loginHasInit;
        return this;
    }

    public boolean isConnectedToServer() {
        return connectedToServer;
    }

    public void setConnectedToServer(boolean connectedToServer) {
        this.connectedToServer = connectedToServer;
    }

    public boolean isInitialed() {
        return this._init;
    }

    public void setChatBaseEvent(ChatBaseEvent chatBaseEvent) {
        this.chatBaseEvent = chatBaseEvent;
    }

    public ChatBaseEvent getChatBaseEvent() {
        return chatBaseEvent;
    }

    public void setChatMessageEvent(ChatMessageEvent chatMessageEvent) {
        this.chatMessageEvent = chatMessageEvent;
    }

    public ChatMessageEvent getChatMessageEvent() {
        return chatMessageEvent;
    }

    public void setMessageQoSEvent(MessageQoSEvent messageQoSEvent) {
        this.messageQoSEvent = messageQoSEvent;
    }

    public MessageQoSEvent getMessageQoSEvent() {
        return messageQoSEvent;
    }
}
