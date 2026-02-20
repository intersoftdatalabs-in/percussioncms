package com.percussion.imaging.shims.segments;

/**
 * Local shim for older code. Not used by ImageReader anymore; preserved
 * to avoid accidental compilation breakage for any other code that might
 * reference these types.
 */
@Deprecated
public class Segment {
    private final int marker;
    private final byte[] bytes;
    public Segment(int marker, byte[] bytes) { this.marker = marker; this.bytes = bytes; }
    public int getMarker() { return marker; }
    public byte[] getSegmentData() { return bytes; }
}
