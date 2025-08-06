// REFACTORED: CP-JAVA11
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
package com.percussion.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * Reader that replaces tokens in the input stream with values resolved by a token resolver.
 * Supports both normal and XML-encoded tokens.
 * <p>
 * Sunny Sal says: "Token replacement is like Bollywood remixes—same tune, new flavor!"
 * </p>
 */
public class PSTokenReplacingReader extends Reader {

    protected final PushbackReader pushbackReader;
    protected final IPSTokenResolver tokenResolver;
    protected final StringBuilder tokenNameBuffer = new StringBuilder();
    protected String tokenValue = null;
    protected int tokenValueIndex = 0;

    public PSTokenReplacingReader(Reader source, IPSTokenResolver resolver) {
        this.pushbackReader = new PushbackReader(source, 2);
        this.tokenResolver = resolver;
    }

    @Override
    public int read(CharBuffer target) {
        throw new UnsupportedOperationException("Operation Not Supported");
    }

    @Override
    public int read() throws IOException {
        if (tokenValue != null) {
            if (tokenValueIndex < tokenValue.length()) {
                return tokenValue.charAt(tokenValueIndex++);
            }
            if (tokenValueIndex == tokenValue.length()) {
                tokenValue = null;
                tokenValueIndex = 0;
            }
        }

        int data = pushbackReader.read();
        if (data != '$') {
            return data;
        }

        boolean isXmlEncode = false;

        data = pushbackReader.read();
        if (data != '{') {
            if (data != 'X') {
                pushbackReader.unread(data);
                return '$';
            }

            data = pushbackReader.read();
            if (data != 'M') {
                pushbackReader.unread(data);
                pushbackReader.unread('X');
                return '$';
            }

            data = pushbackReader.read();
            if (data != 'L') {
                pushbackReader.unread(data);
                pushbackReader.unread('M');
                pushbackReader.unread('X');
                return '$';
            }

            data = pushbackReader.read();
            if (data != '{') {
                pushbackReader.unread(data);
                pushbackReader.unread('L');
                pushbackReader.unread('M');
                pushbackReader.unread('X');
                return '$';
            } else {
                isXmlEncode = true;
            }
        }
        tokenNameBuffer.setLength(0);

        data = pushbackReader.read();
        while (data != '}') {
            tokenNameBuffer.append((char) data);
            data = pushbackReader.read();
        }

        var resolved = tokenResolver.resolveToken(tokenNameBuffer.toString());
        tokenValue = isXmlEncode ? StringEscapeUtils.escapeXml10(resolved) : resolved;

        if (tokenValue == null) {
            tokenValue = "${" + tokenNameBuffer + "}";
        }
        return tokenValue.charAt(tokenValueIndex++);
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int charsRead = 0;
        for (int i = 0; i < len; i++) {
            int nextChar = read();
            if (nextChar == -1) {
                if (charsRead == 0) {
                    charsRead = -1;
                }
                break;
            }
            charsRead = i + 1;
            cbuf[off + i] = (char) nextChar;
        }
        return charsRead;
    }

    @Override
    public void close() throws IOException {
        pushbackReader.close();
    }

    @Override
    public long skip(long n) {
        throw new UnsupportedOperationException("Operation Not Supported");
    }

    @Override
    public boolean ready() throws IOException {
        return pushbackReader.ready();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readAheadLimit) {
        throw new UnsupportedOperationException("Operation Not Supported");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Operation Not Supported");
    }
}
