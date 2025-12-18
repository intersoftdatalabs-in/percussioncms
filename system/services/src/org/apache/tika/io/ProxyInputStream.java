package org.apache.tika.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Minimal shim for legacy Tika class org.apache.tika.io.ProxyInputStream to satisfy parsers
 * that still reference it. Delegates to the wrapped InputStream.
 */
public class ProxyInputStream extends FilterInputStream {
    public ProxyInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return super.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return super.skip(n);
    }

    @Override
    public int available() throws IOException {
        return super.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        super.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        super.reset();
    }

    @Override
    public boolean markSupported() {
        return super.markSupported();
    }
}
