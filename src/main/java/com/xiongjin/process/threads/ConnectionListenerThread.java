package com.xiongjin.process.threads;

import com.xiongjin.process.handler.ConnectionListenerHandler;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class ConnectionListenerThread implements Runnable {
    private final ServerSocket serverSocket;

    public ConnectionListenerThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        System.out.println("Connection listener started");

        Socket socket = null;
        DataInputStream dis = null;
        String message = null;
        ConnectionListenerHandler connectionListenerHandler = null;

        try {
            connectionListenerHandler = ConnectionListenerHandler.getInstance();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                assert serverSocket != null;
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                assert socket != null;
                dis = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                assert dis != null;
                message = dis.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }

            connectionListenerHandler.processInConnection(message, socket);
        }
    }
}
