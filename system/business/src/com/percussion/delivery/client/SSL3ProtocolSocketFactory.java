/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.delivery.client;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * Implements a socket factory supporting SSL3
 * 
 * @author natechadwick
 *
 */
public class SSL3ProtocolSocketFactory extends SSLProtocolSocketFactory {
    private final SSLSocketFactory socketFactory;
    private static final String[] PROTOCOLS = {"SSLv3"};

    public SSL3ProtocolSocketFactory(SSLContext sslContext) {
        super();
        this.socketFactory = sslContext.getSocketFactory();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        SSLSocket sslSocket = (SSLSocket) this.socketFactory.createSocket(host, port);
        sslSocket.setEnabledProtocols(PROTOCOLS);
        return sslSocket;
    }

    @Override
    public Socket createSocket(String host, int port, java.net.InetAddress localAddress, int localPort, org.apache.commons.httpclient.params.HttpConnectionParams params) throws IOException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        if (localPort <= 0) {
            localPort = 443;
        }
        int timeout = params.getConnectionTimeout();
        SSLSocket sslSocket;
        if (timeout == 0 && localAddress != null) {
            sslSocket = (SSLSocket) socketFactory.createSocket(host, port, localAddress, localPort);
        } else {
            sslSocket = (SSLSocket) socketFactory.createSocket();
            InetSocketAddress localAddr = null;
            if (localAddress != null) {
                localAddr = new InetSocketAddress(localAddress, localPort);
            }
            InetSocketAddress remoteAddr = new InetSocketAddress(host, port);
            sslSocket.setEnabledProtocols(PROTOCOLS);
            if (localAddress != null) {
                sslSocket.bind(localAddr);
            }
            sslSocket.connect(remoteAddr, timeout);
        }
        return sslSocket;
    }

    @Override
    public Socket createSocket(String host, int port, java.net.InetAddress clientHost, int clientPort) throws IOException {
        SSLSocket sslSocket = (SSLSocket) socketFactory.createSocket();
        InetSocketAddress localAddr = new InetSocketAddress(clientHost, clientPort);
        InetSocketAddress remoteAddr = new InetSocketAddress(host, port);
        sslSocket.setEnabledProtocols(PROTOCOLS);
        sslSocket.bind(localAddr);
        sslSocket.connect(remoteAddr);
        return sslSocket;
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return super.createSocket(socket, host, port, autoClose);
    }
}
