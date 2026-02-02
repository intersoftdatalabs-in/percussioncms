package org.apache.commons.imaging.bytesource;

/**
 * Backward-compatible alias for the commons-imaging bytesource ByteSource.
 * This bridges older code expecting org.apache.commons.imaging.bytesource
 * to the simpler common.ByteSource shim.
 */
@Deprecated
public abstract class ByteSource extends org.apache.commons.imaging.common.ByteSource {
}
