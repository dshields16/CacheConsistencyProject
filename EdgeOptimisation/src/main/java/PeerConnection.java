import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalTime;

public class PeerConnection extends Thread{

    private Socket peerSocket;
    private OutputStream out;
    private InputStream in;

    private int latency = 0;

    private PeerService peerService;

    //server make connection with communicating client
    public PeerConnection(Socket socket, PeerService peerService) {

        this.peerSocket = socket;
        this.peerService = peerService;
    }

    //client start connection with server
    public PeerConnection(int port, PeerService peerService) throws IOException {
        peerSocket = new Socket("localhost", port);
        this.peerService = peerService;
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
        byte[] bytes = new byte[32];
        int messagesToReceive = 2, currentMessages = 0;

        try { StartConnection(); }
        catch (IOException e) { e.printStackTrace(); }

        while(true) {

            try {
                //wait to receive message
                in.read(bytes);

                //a blank response is sent from recipient, ignore if so
                if(CheckMessageBlank(bytes)){
                    //if(currentMessages >= messagesToReceive)
                    //    break;
                    continue;
                }

                System.out.printf("Received packet at %s%n", LocalTime.now());

                currentMessages++;

                //convert to short array
                short[] sendPacket = new short[bytes.length / 2];
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sendPacket);

                System.out.println("Received packet: ");
                for(int i = 0; i < 16; i++){
                    System.out.printf("%d, ", sendPacket[i]);
                }
                System.out.println();

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
