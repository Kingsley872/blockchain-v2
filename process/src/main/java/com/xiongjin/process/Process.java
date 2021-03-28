package com.xiongjin.process;

import com.xiongjin.process.handlers.ConnectionHandler;

import java.io.IOException;

public class Process {
    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("==> App starts");

        ConnectionHandler connectionHandler = new ConnectionHandler(args[0], args[1]);
        connectionHandler.start();
        UserInteraction userInteraction = new UserInteraction();
        Thread t = new Thread(userInteraction);
        t.start();
    }
}
