import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
    Manages the socket connections between each running process
    Receives and sends byte arrays and sends to the update processing module
 */
public class NodeConnection extends Thread{

    private Socket peerSocket;
    private OutputStream out;
    private InputStream in;

    private int latency = 0, messagesToReceive = 1;
    private short nodeId;

    private NodeUpdateProcessing updateProcessing;

    /*
        socket - pre-existing socket connection
        updateProcessing - update processing module, receives packets
        nodeId - node id for this process

        server receives connection with another communicating process
     */
    public NodeConnection(Socket socket, NodeUpdateProcessing updateProcessing, short nodeId) {

        this.peerSocket = socket;
        this.updateProcessing = updateProcessing;
        this.nodeId = nodeId;
    }

    /*
        port - number of port the socket is opened on
        updateProcessing - update processing module, receives packets
        nodeId - node id for this process

        server opens socket and starts connection with another communicating process
     */
    public NodeConnection(int port, NodeUpdateProcessing updateProcessing, short nodeId) throws IOException {
        peerSocket = new Socket("localhost", port);
        this.updateProcessing = updateProcessing;
        this.nodeId = nodeId;
    }

    /*
        Starts the socket connection
     */
    public void StartConnection() throws IOException {
        in = new DataInputStream(peerSocket.getInputStream());
        out = new DataOutputStream(peerSocket.getOutputStream());
    }

    /*
        Sends a message through the socket
     */
    public void SendMessage(byte[] msg) throws IOException {

        out.write(msg);
    }

    /*
        Stop the connection
     */
    public void StopConnection() throws IOException {
        in.close();
        out.close();
        peerSocket.close();
    }

    public void SetLatency(int latency) {
        this.latency = latency;
    }

    public int GetLatency() {
        return latency;
    }

    public short GetNodeId() {
        return nodeId;
    }

    /*
        value - number of messages to receive

        how many messages should be received from the connected socket
     */
    public void SetMessagesToReceive(int value) {
        messagesToReceive = value;
    }

    /*
        Start the process communication thread, send and receieve byte array messages
     */
    public void run() {

        System.out.println("Starting client thread");
        byte[] bytes = new byte[256];
        int currentMessages = 0;

        try { StartConnection(); }
        catch (IOException e) { e.printStackTrace(); }

        while(true) {

            try {
                //wait to receive message
                in.read(bytes);

                //a blank response is sent from recipient, ignore if so
                if(CheckMessageBlank(bytes)){
                    continue;
                }

                currentMessages++;

                //convert to short array
                short[] sendPacket = new short[bytes.length / 2];
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

                //logic for received data
                updateProcessing.ReceivePacket(sendPacket, nodeId);

                if(currentMessages >= messagesToReceive) {
                    try { Thread.sleep(1000); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

        }

        try { StopConnection(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    /*
        array - received byte array message

        return true if a received message is blank
     */
    private static boolean CheckMessageBlank(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
