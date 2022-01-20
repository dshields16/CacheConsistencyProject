package main.java;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

public class ServerClientTesting {

    private static final int BASE_PORT = 6000, MESSAGES_SENT = 2;
    private static int peerId = -1, noPeers = 0;
    private static short sequenceNo = 0;
    private static boolean isServer = false;

    private static ServerSocket serverSocket;

    private static PeerService peerObj;

    //private static Map<Integer, PeerConnection> peerConnectionMap = new HashMap<>();
    private static List<EdgeConnection> peerConnectionList = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {



        try {
            peerId = Integer.parseInt(args[0]);
            noPeers = Integer.parseInt(args[1]);
        } catch (final NumberFormatException e) {
            System.err.println("Invalid port number");
        }
        int portNumber = BASE_PORT + peerId;

        //identify the server client, middle of the list
        int serverId = noPeers / 2;

        if(peerId == serverId) {
            isServer = true;
        }

        if(isServer) {
            System.out.printf("Server Service %d running on port %d%n", peerId, portNumber);
        }
        else {
            System.out.printf("Client Service %d running on port %d%n", peerId, portNumber);
        }

        peerObj = new PeerService((short) peerId);

        //set up server socket
        serverSocket = new ServerSocket(portNumber);

        //if we are a server then we want to establish a connection with each client as peers would do
        if(isServer) {
            SetupServer();
        }
        else {
            //if we are a client, then establish a connection with the server ONLY
            SetupClient(serverId);
        }

        //sort list of connections by latency to make sending by latency easier
        if(isServer) {
            peerConnectionList.sort(Comparator.comparing(EdgeConnection::GetLatency));
            SendMessageToAllPeers(new byte[] {1});
            SendData();
        }

        //client - wait for server to send ok message, send data to server
        //server - connect with each client, send data to client, repeat received data to every other client


        //wait for all connections to end
        for (EdgeConnection peer:peerConnectionList) {
            peer.join();
        }

        peerObj.OutputStoredData();


        StopServer();

    }

    public static void SendData() {

        Thread thread = new Thread(() -> {

            for(int i = 0; i < MESSAGES_SENT; i++) {
                SendPacketToAllPeers();
                try { Thread.sleep(200); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }

        });
        thread.start();
    }

    private static void SetupServer() throws IOException {



        //send a message to each preceding peer
        for(int i = 0; i < peerId; i++) {

            System.out.printf("Establishing connection with peer %d%n", i);
            EdgeConnection newPeer = new EdgeConnection(BASE_PORT + i, peerObj);
            newPeer.SetLatency(GenerateLatencyValue(i));
            newPeer.SetMessagesToReceive(MESSAGES_SENT);
            newPeer.SetIsServer();
            newPeer.SetMessagesToReceive(MESSAGES_SENT);

            peerConnectionList.add(newPeer);
            newPeer.start();
        }

        //receive message from subsequent peers
        for(int i = peerId + 1; i < noPeers; i++) {

            //client socket created when a client connects
            EdgeConnection newPeer = new EdgeConnection(serverSocket.accept(), peerObj);

            System.out.printf("Establishing connection with peer %d%n", i);
            newPeer.SetLatency(GenerateLatencyValue(i));
            newPeer.SetMessagesToReceive(MESSAGES_SENT);
            newPeer.SetIsServer();
            peerConnectionList.add(newPeer);
            newPeer.start();
        }

    }

    private static void SetupClient(int serverId) throws IOException {

        //init signal, server + other clients
        int messagesToReceive = 1 + (MESSAGES_SENT * (noPeers - 1));

        EdgeConnection newPeer;

        //if we have a peer id lower than the server, listen for a connection
        if(peerId < serverId) {
            newPeer = new EdgeConnection(serverSocket.accept(), peerObj);
        }
        else {
            newPeer = new EdgeConnection(BASE_PORT + serverId, peerObj);
        }

        newPeer.SetLatency(GenerateLatencyValue(serverId));
        newPeer.SetMessagesToReceive(messagesToReceive);
        peerConnectionList.add(newPeer);
        newPeer.start();

    }

    private static void SendPacketToAllPeers() {
        //generate packet
        short[] packetData = peerObj.GenerateUpdatePacket(sequenceNo++);

        byte[] packetByteData = ConvertShortArrayToByte(packetData);

        SendMessageToAllPeers(packetByteData);
    }

    private static void SendMessageToAllPeers(byte[] message) {

        System.out.printf("Sending packet at %s%n", LocalTime.now());

        //make a new thread to send packet
        Thread thread = new Thread(() -> {

            //keep a count of current latency
            int currentLatencyValue = 0;

            System.out.println("Sent packet: ");
            String messageS = "";
            for(int i = 0; i < message.length; i++){
                messageS += String.format("%d", message[i]);
                if(i < message.length - 1) {
                    messageS += String.format(", ", message[i]);
                }
            }
            System.out.println(messageS);

            //send to all peers
            for (EdgeConnection peer : peerConnectionList) {

                //get the difference in latency and sleep for that duration
                int peerLatency = peer.GetLatency();
                int addedLatency = peerLatency - currentLatencyValue;

                if(addedLatency > 0) {
                    try { Thread.sleep(addedLatency); }
                    catch (InterruptedException e) { e.printStackTrace(); }

                    currentLatencyValue += addedLatency;
                }

                try {
                    peer.SendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        thread.start();
    }

    //Server should repeat received packets to every other client
    public static void ServerSendPacketToAllPeersExceptOne(byte[] message, EdgeConnection sender) {

        System.out.printf("Server repeating packet at %s%n", LocalTime.now());

        //make a new thread to send packet
        Thread thread = new Thread(() -> {

            //keep a count of current latency
            int currentLatencyValue = 0;

            //send to all peers
            for (EdgeConnection peer : peerConnectionList) {

                if(peer.equals(sender)){
                    continue;
                }

                //get the difference in latency and sleep for that duration
                int peerLatency = peer.GetLatency();
                int addedLatency = peerLatency - currentLatencyValue;

                if(addedLatency > 0) {
                    try { Thread.sleep(addedLatency); }
                    catch (InterruptedException e) { e.printStackTrace(); }

                    currentLatencyValue += addedLatency;
                }

                try {
                    peer.SendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        thread.start();
    }

    private static void StopServer() throws IOException {
        serverSocket.close();
    }

    private static byte[] ConvertShortArrayToByte(short[] shortArray) {

        ByteBuffer buffer = ByteBuffer.allocate(2*shortArray.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.asShortBuffer().put(shortArray);
        byte[] bytes = buffer.array();
        return bytes;
    }

    private static int GenerateLatencyValue(int otherPeerId){

        //peer 0 and peer 1 have a (base + 5)ms

        return 5 * Math.abs(peerId - otherPeerId);
    }
}
