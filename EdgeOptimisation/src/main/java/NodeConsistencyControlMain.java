import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Random;

/*
    The main module of the program, each module is created and started
    Socket connections are opened
    Cache updates are generated and sent to connected processes
 */
public class NodeConsistencyControlMain {

    //simulation parameters
    private static int nodeId = -1, numNodes = 0, timeBetweenUpdates = 200, currentDelay = 200, messagesSent = 15;
    private static long seed;
    private static final int BASE_PORT = 6000;
    private static boolean useOptimisation = false;

    //receive messages on this port
    private static ServerSocket serverSocket;
    //socket connection modules for both neighbours
    private static NodeConnection node1Connection, node2Connection;

    //start time of the program
    public static long SYSTEM_START = 0;
    //store details about packets sent to monitor traffic
    private static int packetsSent, totalPacketSize;

    public static void main(String[] args) throws IOException, InterruptedException {

        //command line arguments passed into program on execution
        try {
            nodeId = Integer.parseInt(args[0]);
            numNodes = Integer.parseInt(args[1]);
            useOptimisation = Boolean.parseBoolean(args[2]);
            timeBetweenUpdates = Integer.parseInt(args[3]);
            currentDelay = timeBetweenUpdates;

            messagesSent = Integer.parseInt(args[4]);

            if(args.length > 5) {
                seed= Long.parseLong(args[5]);
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

            for (int i = 0; i < messagesSent; i++) {
                byte[] packetData = dataGen.GenerateUpdate(nodeId);

                //System.out.printf("Generated data of length %d%n", packetData.length);

                //convert to short array
                short[] sendPacket = new short[packetData.length / 2];
                ByteBuffer.wrap(packetData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

                updateProcessing.ReceivePacket(sendPacket, (short) nodeId);

                //generate an update packet based on which values have changed
                currentDelay -= 200;
                if(currentDelay <= 0) {
                    //System.out.println("Sending packets");
                    int node1 = node1Connection.GetNodeId();
                    int node2 = node2Connection.GetNodeId();
                    byte[] updatePacket1 = updateProcessing.GenerateDelayedUpdate((short) timeBetweenUpdates, useOptimisation, node1);
                    byte[] updatePacket2 = updateProcessing.GenerateDelayedUpdate((short) timeBetweenUpdates, useOptimisation, node2);
                    SendPacketToAllNeighbours(updatePacket1, updatePacket2);
                    //add packet stats
                    packetsSent += 2;
                    totalPacketSize += updatePacket1.length + updatePacket2.length;

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
        updateProcessing.OutputCacheRatioData();
        updateProcessing.OutputDataFrequencyValues();

        StopServer();
    }

    /*
        nodeId - node id of the process being connected to
        updateProcessing - the update processing module which will receive messages
        latency - the latency of the new connection

        Receive a connection with another process to start communication
     */
    private static NodeConnection ConnectWithPrecedingNodeIndex(int nodeId, NodeUpdateProcessing updateProcessing, int latency) throws IOException {
        System.out.printf("Establishing connection with node %d%n", nodeId);
        NodeConnection newPeer = new NodeConnection(BASE_PORT + nodeId, updateProcessing, (short) nodeId);
        newPeer.SetLatency(latency);
        newPeer.SetMessagesToReceive((int) Math.floor((200*messagesSent)/timeBetweenUpdates));
        newPeer.start();

        return newPeer;
    }

    /*
        nodeId - node id of the process being connected to
        updateProcessing - the update processing module which will receive messages
        latency - the latency of the new connection

        Starts a connection with another process
     */
    private static NodeConnection ConnectWithSubsequentNodeIndex(int nodeId, NodeUpdateProcessing updateProcessing, int latency) throws IOException {

        //client socket created when a client connects
        NodeConnection newPeer = new NodeConnection(serverSocket.accept(), updateProcessing, (short) nodeId);
        newPeer.SetLatency(latency);
        newPeer.SetMessagesToReceive((int) Math.floor((200*messagesSent)/timeBetweenUpdates));
        newPeer.start();

        return newPeer;
    }

    /*
        message1 - generated message to be sent to neighbour 1
        message2 - generate message to be sent to neighbour 2

        Sends generated cache update messages to both neighbours
     */
    private static void SendPacketToAllNeighbours(byte[] message1, byte[] message2) {

        //make a new thread to send packet
        Thread thread = new Thread(() -> {

            //send to all neighbours
            NodeConnection lowerNode, higherNode;
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

            //node 1
            try { Thread.sleep(lowerNode.GetLatency() - baseLatency); }
            catch (InterruptedException e) { e.printStackTrace(); }

            if(lowerNode == node1Connection) {
                try { lowerNode.SendMessage(message1); }
                catch (IOException e) { e.printStackTrace();}
            }
            else {
                try { lowerNode.SendMessage(message2); }
                catch (IOException e) { e.printStackTrace();}
            }

            //node 2
            try { Thread.sleep(higherNode.GetLatency() - lowerNode.GetLatency()); }
            catch (InterruptedException e) { e.printStackTrace(); }


            if(higherNode == node1Connection) {
                try { higherNode.SendMessage(message1); }
                catch (IOException e) { e.printStackTrace();}
            }
            else {
                try { higherNode.SendMessage(message2); }
                catch (IOException e) { e.printStackTrace();}
            }

        });

        thread.start();
    }

    /*
        Ends all connections
     */
    private static void StopServer() throws IOException {
        serverSocket.close();
    }

    /*
        Print the final simulation details for performance
     */
    private static void OutputOutgoingPacketStats() {

        float avgPacketSize = totalPacketSize / packetsSent;

        System.out.printf("Packets sent: %d%n", packetsSent);
        System.out.printf("Total bytes sent: %d%n", totalPacketSize);
        System.out.printf("Average packet size: %.2f bytes%n", avgPacketSize);
    }
}
