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

/*

public class PeerToPeerTesting {

    private static final int BASE_PORT = 6000, MESSAGES_SENT = 20;
    private static int peerId = -1, noPeers = 0;

    private static ServerSocket serverSocket;

    //private static Map<Integer, PeerConnection> peerConnectionMap = new HashMap<>();
    private static List<PeerConnection> peerConnectionList = new ArrayList<>();

    //store details about packets sent to monitor traffic
    private static int packetsSent, totalPacketSize, sequenceNo = 0;

    public static void main(String[] args) throws IOException, InterruptedException {



        try {
            peerId = Integer.parseInt(args[0]);
            noPeers = Integer.parseInt(args[1]);
        } catch (final NumberFormatException e) {
            System.err.println("Invalid port number");
        }
        int portNumber = BASE_PORT + peerId;
        System.out.printf("Peer Service %d running on port %d%n", peerId, portNumber);

        PeerService peerObj = new PeerService((short) peerId, noPeers, -1);

        //set up server
        serverSocket = new ServerSocket(portNumber);

        //send a message to each preceding peer
        for(int i = 0; i < peerId; i++) {

            System.out.printf("Establishing connection with peer %d%n", i);
            PeerConnection newPeer = new PeerConnection(BASE_PORT + i, peerObj);
            newPeer.SetLatency(GenerateLatencyValue(i));
            newPeer.SetMessagesToReceive(MESSAGES_SENT);
            peerConnectionList.add(newPeer);
            newPeer.start();
        }


        //receive message from subsequent peers
        for(int i = peerId + 1; i < noPeers; i++) {

            //client socket created when a client connects
            PeerConnection newPeer = new PeerConnection(serverSocket.accept(), peerObj);
            newPeer.SetLatency(GenerateLatencyValue(i));
            newPeer.SetMessagesToReceive(MESSAGES_SENT);
            peerConnectionList.add(newPeer);
            newPeer.start();
        }

        //sort list of connections by latency to make sending by latency easier
        peerConnectionList.sort(Comparator.comparing(PeerConnection::GetLatency));


        Thread thread = new Thread(() -> {

            for (int i = 0; i < MESSAGES_SENT; i++) {
                SendPacketToAllPeers(peerObj);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        thread.start();



        //wait for all connections to end
        for (PeerConnection peer:peerConnectionList) {
            peer.join();
        }

        peerObj.OutputStoredData();
        OutputAveragelatency();
        OutputOutgoingPacketStats();

        StopServer();

    }

    private static void SendPacketToAllPeers(PeerService peerObj) {

        System.out.printf("Sending packets at %s%n", LocalTime.now());

        //make a new thread to send packet
        Thread thread = new Thread(() -> {

            //keep a count of current latency
            int currentLatencyValue = 0;

            //generate packet, sequence number not used but required for integration with Server-Client code
            short[] packetData = peerObj.GenerateRandomClientUpdatePacket((short) sequenceNo++);
            //Utils.PrintShortArray(packetData);

            byte[] packetByteData = ConvertShortArrayToByte(packetData);

            //send to all peers
            for (PeerConnection peer : peerConnectionList) {

                //add packet stats
                packetsSent++;
                totalPacketSize += packetData.length;

                //get the difference in latency and sleep for that duration
                int peerLatency = peer.GetLatency();
                int addedLatency = peerLatency - currentLatencyValue;

                if(addedLatency > 0) {
                    try { Thread.sleep(addedLatency); }
                    catch (InterruptedException e) { e.printStackTrace(); }

                    currentLatencyValue += addedLatency;
                }

                try {
                    peer.SendMessage(packetByteData);
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

    private static void OutputAveragelatency() {

        float latencySum = 0;
        int baseLatency = 13, maxLatency = 0;

        for (PeerConnection connection:peerConnectionList) {
            latencySum += connection.GetLatency() + baseLatency;

            if(connection.GetLatency() > maxLatency) {
                maxLatency = connection.GetLatency() + baseLatency;
            }
        }

        System.out.printf("Max latency: %dms%n", maxLatency);
        System.out.printf("Mean latency: %dms%n", (int)(latencySum / peerConnectionList.size()));
    }

    private static void OutputOutgoingPacketStats() {

        float avgPacketSize = totalPacketSize / packetsSent;

        System.out.printf("Packets sent: %d%n", packetsSent);
        System.out.printf("Average packet size: %.2f bytes%n", avgPacketSize*2);
    }
}

 */
