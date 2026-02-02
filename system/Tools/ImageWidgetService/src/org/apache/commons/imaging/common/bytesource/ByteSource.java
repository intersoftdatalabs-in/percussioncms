package org.apache.commons.imaging.common.bytesource;

/**
 * Backward-compatible alias for the newer commons imaging ByteSource.
 * This class intentionally extends the primary implementation so code
 * that referenced the old package continues to work.
 */
@Deprecated
public abstract class ByteSource extends org.apache.commons.imaging.common.ByteSource {
}
