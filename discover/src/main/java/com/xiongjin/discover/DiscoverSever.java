package com.xiongjin.discover;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;


public class DiscoverSever {
    public static void main(String[] args) throws IOException {
        System.out.println("Discover server is started");
        ServerSocket serverSocket = new ServerSocket(5000);
        Socket socket;
        DataOutputStream dos;
        String message;
        AtomicInteger portNum = new AtomicInteger(1233);
        AtomicInteger id = new AtomicInteger(0);

         do {
            socket = serverSocket.accept();
            System.out.println("New connection");

            dos = new DataOutputStream(socket.getOutputStream());

            message = id.incrementAndGet() + "," + portNum.incrementAndGet();
            dos.writeUTF(message);
             System.out.println("Send: " + message);

            socket.close();
            dos.close();
        } while (id.get() < 5);
         serverSocket.close();
        System.out.println("Done sending info to all five processes");
    }
}
