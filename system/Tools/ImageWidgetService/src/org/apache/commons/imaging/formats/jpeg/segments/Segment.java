package org.apache.commons.imaging.formats.jpeg.segments;

public class Segment {
    private final int marker;
    private final byte[] bytes;
    public Segment(int marker, byte[] bytes) { this.marker = marker; this.bytes = bytes; }
    public int getMarker() { return marker; }
    public byte[] getSegmentData() { return bytes; }
}
