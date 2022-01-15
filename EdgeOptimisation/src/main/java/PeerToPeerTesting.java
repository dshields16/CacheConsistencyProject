package main.java;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeerToPeerTesting {

    private static final int BASE_PORT = 6000;

    private static ServerSocket serverSocket;

    private static Map<Integer, PeerConnection> peerConnectionMap = new HashMap<>();

    public static void main(String[] args) throws IOException {


        /*
        //generate peer services
        List<PeerService> peerServices = PeerSetup(2);

        //setup a graph of connections between peers
        PeerNodeGraph connectionGraph = new PeerNodeGraph(peerServices);

        //example of obtaining latency between peers
        int latency = PeerNodeGraph.GetLatency(peerServices.get(0), peerServices.get(1));
        System.out.printf("Latency: %dms%n", latency);
        */


        int peerId = -1, noPeers = 0;
        try {
            peerId = Integer.parseInt(args[0]);
            noPeers = Integer.parseInt(args[1]);
        } catch (final NumberFormatException e) {
            System.err.println("Invalid port number");
        }
        int portNumber = BASE_PORT + peerId;
        System.out.printf("Peer Service %d running on port %d%n", peerId, portNumber);

        PeerService peerObj = new PeerService((short) portNumber);

        //set up server

/*
        if(peerId == 0) {
            serverSocket = new ServerSocket(portNumber);
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String greeting = in.readLine();
            System.out.println("hello client");
        } else {
            clientSocket = new Socket("localhost", BASE_PORT);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            out.println("Test!");
        }
*/

        serverSocket = new ServerSocket(portNumber);

/*
        if(peerId == 0) {

            //client socket created when a client connects
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.printf("Establishing connection with peer %d%n", 1);
            String greeting = in.readLine();
            System.out.printf("Received message: %s%n", greeting);

            //PeerConnection newPeer = new PeerConnection();
            //peerConnectionMap.put(1, newPeer);
            //newPeer.StartConnection(BASE_PORT + 1);
        } else {

            System.out.printf("Establishing connection with peer %d%n", 0);
            PeerConnection newPeer = new PeerConnection(BASE_PORT + 0);
            //peerConnectionMap.put(0, newPeer);
            newPeer.start();
            //newPeer.StartConnection(BASE_PORT + 0);
            //newPeer.SendMessage(String.format("Hello peer %d from peer %d", 0, peerId));
        }
*/


        //send a message to each preceding peer
        for(int i = 0; i < peerId; i++) {

            System.out.printf("Establishing connection with peer %d%n", i);
            PeerConnection newPeer = new PeerConnection(BASE_PORT + i);
            peerConnectionMap.put(0, newPeer);
            newPeer.start();
        }


        //receive message from subsequent peers
        for(int i = peerId + 1; i < noPeers; i++) {

            //client socket created when a client connects
            PeerConnection newPeer = new PeerConnection(serverSocket.accept());
            peerConnectionMap.put(1, newPeer);
            newPeer.start();
        }



        StopServer();

    }

    private PeerConnection GetPeerConnection(int peerId){
        return peerConnectionMap.get(peerId);
    }

    private static void StopServer() throws IOException {
        serverSocket.close();
    }
}
