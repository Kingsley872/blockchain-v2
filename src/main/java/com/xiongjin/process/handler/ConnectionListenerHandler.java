package com.xiongjin.process.handler;

import com.xiongjin.process.threads.ConnectionListenerThread;
import com.xiongjin.process.threads.ProListenerThread;
import javafx.util.Pair;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import static com.xiongjin.process.Constatns.*;

public class ConnectionListenerHandler {
    public static ConnectionListenerHandler instance = null;
    public static ConnectionListenerHandler getInstance() throws UnknownHostException {
        if (instance == null) {
            instance = new ConnectionListenerHandler();
        }
        return instance;
    }
    public ConnectionListenerHandler() throws UnknownHostException {}

    private ServerSocket serverSocket;
    private int id;
    private int port;
    private final InetAddress ip = InetAddress.getByName("localhost");
    private final ArrayList<Pair<Integer, Integer>> proInfoList = new ArrayList<Pair<Integer, Integer>>() {
        {
            add(new Pair(P1, P1_PORT));
            add(new Pair(P2, P2_PORT));
            add(new Pair(P3, P3_PORT));
            add(new Pair(P4, P4_PORT));
            add(new Pair(P5, P5_PORT));
        }
    };
    private final ArrayList<Pair<Integer, Socket>> outSocketList = new ArrayList<>();
    private final ArrayList<Pair<Integer, Socket>> inSocketList = new ArrayList<>();

    public void start() throws IOException, InterruptedException {
        identifyMyself();
        Thread.sleep(5000);
        connectEachProcess();
    }

    public void identifyMyself() throws IOException {
        String message;
        Socket socket;
        DataInputStream dis;
        StringTokenizer st;

        socket = new Socket(ip, this.port);
        dis = new DataInputStream(socket.getInputStream());
        message = dis.readUTF();
        st = new StringTokenizer(message, ",");
        this.id = Integer.parseInt(st.nextToken());
        this.port = Integer.parseInt(st.nextToken());

        System.out.println("==> ID: " + this.id);
        System.out.println("==> PORT: " + this.port);

        socket.close();
        dis.close();
    }


    public void connectEachProcess() throws IOException, InterruptedException {
        serverSocket = new ServerSocket(this.port);
        ConnectionListenerThread connectionListenerThread = new ConnectionListenerThread(serverSocket);
        connectionListenerThread.run();
        connectionRequest();
    }

    public void connectionRequest() throws IOException, InterruptedException {
        DataOutputStream dos;
        for (int i = 1; i <= MAX_NODES; i++) {
            if (this.id != i) {
                Socket socket = new Socket(ip, proInfoList.get(i).getValue());
                outSocketList.add(new Pair(i, socket));
                Thread.sleep(1000);

                dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(String.valueOf(this.id));
            }
        }
    }

    public void processInConnection(String inId, Socket socket) {
        inSocketList.add(new Pair(inId, socket));
        ProListenerThread proListenerThread = new ProListenerThread(this.id, Integer.parseInt(inId), socket);
        proListenerThread.run();
    }
}

