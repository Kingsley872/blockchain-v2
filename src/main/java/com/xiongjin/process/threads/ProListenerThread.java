package com.xiongjin.process.threads;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ProListenerThread implements Runnable{

    private final int myId;
    private final int inId;
    private final Socket socket;

    public ProListenerThread(int myId, int inId, Socket socket) {
        this.myId = myId;
        this.inId = inId;
        this.socket = socket;
    }

    @Override
    public void run() {
        DataInputStream dis = null;
        while (true) {
            try {
                dis = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                assert dis != null;
                dis.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
