package org.apache.commons.imaging.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Compatibility shim that provides a ByteSource backed by an InputStream.
 */
public class ByteSourceInputStream extends ByteSource {
    private final byte[] data;

    public ByteSourceInputStream(byte[] data) { this.data = data; }

    /**
     * Compatibility constructor: read all bytes from InputStream.
     * @param is input stream to read from
     * @param name optional name (ignored)
     * @throws IOException on read error
     */
    public ByteSourceInputStream(InputStream is, String name) throws IOException {
        this.data = is.readAllBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(data); }
}
