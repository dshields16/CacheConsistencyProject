import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Random;

public class NodeConsistencyControlMain {

    private static int nodeId = -1, numNodes = 0, timeBetweenUpdates = 200, currentDelay = 200;
    private static long seed;
    private static final int BASE_PORT = 6000, MESSAGES_SENT = 15;
    private static boolean useOptimisation = false;

    private static ServerSocket serverSocket;   //receive messages on this port
    private static PeerConnection node1Connection, node2Connection;

    public static long SYSTEM_START = 0;
    //store details about packets sent to monitor traffic
    private static int packetsSent, totalPacketSize;

    public static void main(String[] args) throws IOException, InterruptedException {

        try {
            nodeId = Integer.parseInt(args[0]);
            numNodes = Integer.parseInt(args[1]);
            useOptimisation = Boolean.parseBoolean(args[2]);
            timeBetweenUpdates = Integer.parseInt(args[3]);
            currentDelay = timeBetweenUpdates;

            if(args.length > 4) {
                seed= Long.parseLong(args[4]);
            }
            else {
                seed = new Random().nextLong();
                System.out.printf("Random seed used: %d%n", seed);
            }

        } catch (final NumberFormatException e) {
            System.err.println("Invalid port number");
        }

        //generate node map using seed
        NodeGeneration nodeGen = new NodeGeneration(seed, numNodes, nodeId);

        int portNumber = BASE_PORT + nodeId;
        System.out.printf("Peer Service %d running on port %d%n", nodeId, portNumber);
        System.out.printf("Neighbours are nodes %d and %d%n", nodeGen.GetNeighbour1(nodeId), nodeGen.GetNeighbour2(nodeId));

        NodeUpdateProcessing updateProcessing = new NodeUpdateProcessing((short) nodeId);

        //set up server
        serverSocket = new ServerSocket(portNumber);

        //establish connection with neighbours
        if(nodeGen.GetNeighbour1(nodeId) < nodeId) {
            node1Connection = ConnectWithPrecedingNodeIndex(nodeGen.GetNeighbour1(nodeId), updateProcessing, nodeGen.GetNeighbour1Latency(nodeId));

            if(nodeGen.GetNeighbour2(nodeId) < nodeId) {
                node2Connection = ConnectWithPrecedingNodeIndex(nodeGen.GetNeighbour2(nodeId), updateProcessing, nodeGen.GetNeighbour2Latency(nodeId));
            }
            else {
                node2Connection = ConnectWithSubsequentNodeIndex(nodeGen.GetNeighbour2(nodeId), updateProcessing, nodeGen.GetNeighbour2Latency(nodeId));
            }
        }
        else {
            //both neighbours are subsequent
            node1Connection = ConnectWithSubsequentNodeIndex(nodeGen.GetNeighbour1(nodeId), updateProcessing, nodeGen.GetNeighbour1Latency(nodeId));
            node2Connection = ConnectWithSubsequentNodeIndex(nodeGen.GetNeighbour2(nodeId), updateProcessing, nodeGen.GetNeighbour2Latency(nodeId));
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SYSTEM_START = timestamp.getTime();


        //start sending updates
        DataGeneration dataGen = new DataGeneration(seed, numNodes, nodeGen, updateProcessing);

        Thread thread = new Thread(() -> {

            for (int i = 0; i < MESSAGES_SENT; i++) {
                byte[] packetData = dataGen.GenerateUpdate(nodeId);

                //convert to short array
                short[] sendPacket = new short[packetData.length / 2];
                ByteBuffer.wrap(packetData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

                updateProcessing.ReceivePacket(sendPacket, (short) nodeId);

                //generate an update packet based on which values have changed
                currentDelay -= 200;
                if(currentDelay <= 0) {
                    System.out.println("Sending packets");
                    byte[] updatePacket = updateProcessing.GenerateDelayedUpdate((short) timeBetweenUpdates);
                    SendPacketToAllNeighbours(updatePacket);
                    //add packet stats
                    packetsSent += 2;
                    totalPacketSize += 2 * updatePacket.length;

                    currentDelay = timeBetweenUpdates;
                }


                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        thread.start();



        //wait for all connections to end
        thread.join();
        node1Connection.join();
        node2Connection.join();

        updateProcessing.OutputStoredData();
        OutputOutgoingPacketStats();
        updateProcessing.OutputStalenessData();

        StopServer();

    }

    private static PeerConnection ConnectWithPrecedingNodeIndex(int nodeId, NodeUpdateProcessing updateProcessing, int latency) throws IOException {
        System.out.printf("Establishing connection with node %d%n", nodeId);
        PeerConnection newPeer = new PeerConnection(BASE_PORT + nodeId, updateProcessing, (short) nodeId);
        newPeer.SetLatency(latency);
        //newPeer.SetMessagesToReceive((numNodes - 1) * MESSAGES_SENT);
        newPeer.SetMessagesToReceive((int) Math.floor((200*MESSAGES_SENT)/timeBetweenUpdates));
        newPeer.start();

        return newPeer;
    }

    private static PeerConnection ConnectWithSubsequentNodeIndex(int nodeId, NodeUpdateProcessing updateProcessing, int latency) throws IOException {

        //client socket created when a client connects
        PeerConnection newPeer = new PeerConnection(serverSocket.accept(), updateProcessing, (short) nodeId);
        newPeer.SetLatency(latency);
        //newPeer.SetMessagesToReceive((numNodes - 1) * MESSAGES_SENT);
        newPeer.SetMessagesToReceive((int) Math.floor((200*MESSAGES_SENT)/timeBetweenUpdates));
        newPeer.start();

        return newPeer;
    }

    private static void SendPacketToAllNeighbours(byte[] message) {

        //System.out.printf("Sending packets at %s%n", LocalTime.now());

        //make a new thread to send packet
        Thread thread = new Thread(() -> {

            //send to all neighbours
            PeerConnection lowerNode, higherNode;
            if(node1Connection.GetLatency() < node2Connection.GetLatency()) {

                lowerNode = node1Connection;
                higherNode = node2Connection;
            }
            else {
                lowerNode = node2Connection;
                higherNode = node1Connection;
            }

            //keep a count of current latency
            int baseLatency = 13;   //base latency for sockets

            //System.out.printf("First node latency: %d, base latency: %d, sleep time: %d%n", lowerNode.GetLatency(), baseLatency, lowerNode.GetLatency() - baseLatency);

            //node 1
            try { Thread.sleep(lowerNode.GetLatency() - baseLatency); }
            catch (InterruptedException e) { e.printStackTrace(); }

            try { node1Connection.SendMessage(message); }
            catch (IOException e) { e.printStackTrace();}

            //System.out.printf("Second node latency: %d, First node latency: %d, sleep time: %d%n", higherNode.GetLatency(), lowerNode.GetLatency(), higherNode.GetLatency() - lowerNode.GetLatency());

            //node 2
            try { Thread.sleep(higherNode.GetLatency() - lowerNode.GetLatency()); }
            catch (InterruptedException e) { e.printStackTrace(); }

            try { node2Connection.SendMessage(message); }
            catch (IOException e) { e.printStackTrace();}

        });

        thread.start();
    }

    private static void StopServer() throws IOException {
        serverSocket.close();
    }

    private static void OutputOutgoingPacketStats() {

        float avgPacketSize = totalPacketSize / packetsSent;

        System.out.printf("Packets sent: %d%n", packetsSent);
        System.out.printf("Total bytes sent: %d%n", totalPacketSize);
        System.out.printf("Average packet size: %.2f bytes%n", avgPacketSize);
    }
}
