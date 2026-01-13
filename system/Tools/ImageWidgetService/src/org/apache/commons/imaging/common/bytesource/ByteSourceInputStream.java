package org.apache.commons.imaging.common.bytesource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteSourceInputStream extends ByteSource {
    private final byte[] data;
    public ByteSourceInputStream(byte[] data) { this.data = data; }
    @Override
    public InputStream getInputStream() { return new ByteArrayInputStream(data); }
}
