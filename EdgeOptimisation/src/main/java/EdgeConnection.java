import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalTime;

public class EdgeConnection extends Thread{

    private Socket peerSocket;
    private OutputStream out;
    private InputStream in;

    private int latency = 0;
    private int messagesToReceive = 1;

    private PeerService peerService;

    private boolean isServer = false;

    //server make connection with communicating client
    public EdgeConnection(Socket socket, PeerService peerService) {

        this.peerSocket = socket;
        this.peerService = peerService;
    }

    //client start connection with server
    public EdgeConnection(int port, PeerService peerService) throws IOException {
        peerSocket = new Socket("localhost", port);
        this.peerService = peerService;
    }

    public void SetIsServer() {
        isServer = true;
    }

    public void SetMessagesToReceive(int value) {
        messagesToReceive = value;
    }

    public void StartConnection() throws IOException {
        in = new DataInputStream(peerSocket.getInputStream());
        out = new DataOutputStream(peerSocket.getOutputStream());
    }

    public void SendMessage(byte[] msg) throws IOException {

        out.write(msg);
    }

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

    //read in data, length sent first
    public void run() {

        System.out.println("Starting client thread");
        byte[] bytes = new byte[34];
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

                System.out.printf("Received packet at %s%n", LocalTime.now());
                String message = "";
                for(int i = 0; i < bytes.length; i++){
                    message += String.format("%d", bytes[i]);
                    if(i < bytes.length - 1) {
                        message += String.format(", ", bytes[i]);
                    }
                }
                System.out.println(message);

                //if this is the first message received as a client, server has connected to all clients
                if(!isServer && currentMessages == 0) {

                    currentMessages++;
                    if(bytes[0] == 1) {
                        System.out.println("Server has connected with all clients");
                        ServerClientTesting.SendData();
                        continue;
                    }
                    else {
                        break;
                    }

                }

                currentMessages++;

                //if server, repeat message to all other clients
                if(isServer) {
                    ServerClientTesting.ServerSendPacketToAllPeersExceptOne(bytes, this);
                }

                //convert to short array
                short[] sendPacket = new short[bytes.length / 2];
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

                //logic for received data
                peerService.ReceivePacket(sendPacket);

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

    private static boolean CheckMessageBlank(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
