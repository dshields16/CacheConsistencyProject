package main.java;

public class PeerNodeEdge {

    PeerConnection source, destination;
    int edgeLatency;

    public PeerNodeEdge(PeerConnection src, PeerConnection dst, int latency) {
        source = src;
        destination = dst;
        edgeLatency = latency;
    }
}
