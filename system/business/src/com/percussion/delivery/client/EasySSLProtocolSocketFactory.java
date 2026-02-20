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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EasySSLProtocolSocketFactory can be used to create SSL {@link Socket}s
 * that accept self-signed certificates. For development or testing only.
 * <p>
 * Example usage:
 * <pre>
 * Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 * URI uri = new URI("https://localhost/", true);
 * GetMethod httpget = new GetMethod(uri.getPathQuery());
 * HostConfiguration hc = new HostConfiguration();
 * hc.setHost(uri.getHost(), uri.getPort(), easyhttps);
 * HttpClient client = new HttpClient();
 * client.executeMethod(hc, httpget);
 * </pre>
 * </p>
 * <p>
 * DISCLAIMER: Not for production use unless you understand the security risks.
 * </p>
 */
public class EasySSLProtocolSocketFactory implements SecureProtocolSocketFactory {
    private static final String[] PROTOCOLS = {"TLSv1.1", "TLSv1.2"};
    private static final Logger LOG = LogManager.getLogger(EasySSLProtocolSocketFactory.class);
    private SSLContext sslContext;

    public EasySSLProtocolSocketFactory() {
        // No-op constructor
    }

    private static SSLContext createEasySSLContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {new EasyX509TrustManager(null)}, new java.security.SecureRandom());
            return context;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new HttpClientError(e.toString());
        }
    }

    private SSLContext getSSLContext() {
        if (sslContext == null) {
            sslContext = createEasySSLContext();
        }
        return sslContext;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
            throws IOException {
        SSLSocket socket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
        socket.setEnabledProtocols(PROTOCOLS);
        socket.addHandshakeCompletedListener(new MyHandshakeListener());
        return socket;
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params)
            throws IOException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        javax.net.SocketFactory socketFactory = getSSLContext().getSocketFactory();
        SSLSocket socket;
        if (timeout == 0) {
            socket = (SSLSocket) socketFactory.createSocket(host, port, localAddress, localPort);
        } else {
            socket = (SSLSocket) socketFactory.createSocket();
            InetSocketAddress localAddr = new InetSocketAddress(localAddress, localPort);
            InetSocketAddress remoteAddr = new InetSocketAddress(host, port);
            socket.bind(localAddr);
            socket.connect(remoteAddr, timeout);
        }
        socket.setEnabledProtocols(PROTOCOLS);
        socket.addHandshakeCompletedListener(new MyHandshakeListener());
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(host, port);
        socket.setEnabledProtocols(PROTOCOLS);
        socket.addHandshakeCompletedListener(new MyHandshakeListener());
        return socket;
    }

    @Override
    public Socket createSocket(Socket socket1, String host, int port, boolean autoClose) throws IOException {
        SSLSocket socket = (SSLSocket) getSSLContext().getSocketFactory().createSocket(socket1, host, port, autoClose);
        socket.setEnabledProtocols(PROTOCOLS);
        socket.addHandshakeCompletedListener(new MyHandshakeListener());
        return socket;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass().equals(EasySSLProtocolSocketFactory.class);
    }

    @Override
    public int hashCode() {
        return EasySSLProtocolSocketFactory.class.hashCode();
    }

    /**
     * Listener for SSL handshake completion events.
     */
    static class MyHandshakeListener implements HandshakeCompletedListener {
        @Override
        public void handshakeCompleted(HandshakeCompletedEvent e) {
            LOG.debug("Handshake successful!");
            LOG.debug("Using cipher suite: {}", e.getCipherSuite());
            LOG.debug("Using protocol: {}", e.getSession().getProtocol());
        }
    }
}
