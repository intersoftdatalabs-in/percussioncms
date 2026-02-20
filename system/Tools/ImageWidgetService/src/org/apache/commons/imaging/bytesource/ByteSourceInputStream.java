package org.apache.commons.imaging.bytesource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Compatibility shim matching older commons-imaging bytesource.ByteSourceInputStream.
 */
public class ByteSourceInputStream extends ByteSource {
    private final byte[] data;

    public ByteSourceInputStream(byte[] data) { this.data = data; }

    public ByteSourceInputStream(InputStream is, String name) throws IOException {
        this.data = is.readAllBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }
}
