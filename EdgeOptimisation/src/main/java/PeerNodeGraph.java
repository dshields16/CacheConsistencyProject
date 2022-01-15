package main.java;

import java.util.ArrayList;
import java.util.List;

public class PeerNodeGraph {

    //number of peers (vertices) in the graph
    private static int numPeers = 0;

    //each index represents a source peer, list of destinations for each source
    private static List<PeerNodeEdge> [] adjacencyMatrix;

    public PeerNodeGraph(List<PeerConnection> peers) {

        numPeers = peers.size();
        adjacencyMatrix = new ArrayList[numPeers];

        for(int i = 0; i < numPeers; i++) {
            adjacencyMatrix[i] = new ArrayList<>();
        }

        GenerateFullGraph(peers, 33);
    }

    private void GenerateFullGraph(List<PeerConnection> peers, int latency) {

        for(int i = 0; i < numPeers; i++) {
            for(int j = 0; j < numPeers; j++) {

                if(i == j)
                    continue;

                adjacencyMatrix[i].add(new PeerNodeEdge(peers.get(i), peers.get(j), latency));
            }
        }
    }

    //Return the index of the peer in the matrix, -1 if no connections outwards
    private static int GetIndexOfSourcePeer(PeerConnection src) {

        for(int i = 0; i < numPeers; i++) {

            if(adjacencyMatrix[i].size() == 0)
                continue;

            if(adjacencyMatrix[i].get(0).source.equals(src)){
                return i;
            }
        }

        return -1;
    }

    //Assume direct path between services, -1 if no connection to destination
    public static int GetLatency(PeerConnection src, PeerConnection dst) {

        List<PeerNodeEdge> edges = adjacencyMatrix[GetIndexOfSourcePeer(src)];

        PeerNodeEdge connectingEdge = edges.stream()
                .filter(edge -> dst == edge.destination)
                .findAny()
                .orElse(null);

        if(connectingEdge != null){
            return connectingEdge.edgeLatency;
        }

        return -1;
    }
}
