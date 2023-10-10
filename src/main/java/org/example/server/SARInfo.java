package org.example.server;

public class SARInfo {
    private final short refId;
    private final short segmentId;
    private final String assembledMessage;

    public SARInfo(short refId, short segmentId, String assembledMessage) {
        this.refId = refId;
        this.segmentId = segmentId;
        this.assembledMessage = assembledMessage;
    }

    public short getRefId() {
        return refId;
    }

    public short getSegmentId() {
        return segmentId;
    }

    public String getAssembledMessage() {
        return assembledMessage;
    }
}