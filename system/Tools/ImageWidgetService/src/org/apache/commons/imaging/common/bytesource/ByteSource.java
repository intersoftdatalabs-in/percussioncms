package org.apache.commons.imaging.common.bytesource;

import java.io.InputStream;

public abstract class ByteSource {
    public abstract InputStream getInputStream() throws java.io.IOException;
}
