package org.apache.commons.imaging.common;

import java.io.InputStream;

/**
 * Compatibility shim for commons-imaging ByteSource used by older code.
 */
public abstract class ByteSource {
    public abstract InputStream getInputStream() throws java.io.IOException;
}
