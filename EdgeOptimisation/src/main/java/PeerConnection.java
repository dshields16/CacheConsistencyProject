package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PeerConnection extends Thread{

    private Socket peerSocket;
    private PrintWriter out;
    private BufferedReader in;

    //server make connection with communicating client
    public PeerConnection(Socket socket) {
        this.peerSocket = socket;
    }

    //client start connection with server
    public PeerConnection(int port) throws IOException {
        peerSocket = new Socket("localhost", port);
    }

    public void StartConnection() throws IOException {
        out = new PrintWriter(peerSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
    }

    public void SendMessage(String msg) throws IOException {
        out.println(msg);
    }

    public void StopConnection() throws IOException {
        in.close();
        out.close();
        peerSocket.close();
    }

    public void run() {

        System.out.println("Starting client thread");

        try { StartConnection(); }
        catch (IOException e) { e.printStackTrace(); }





        try { StopConnection(); }
        catch (IOException e) { e.printStackTrace(); }
    }
}
