package com.xiongjin.process.handler;

public class PortHandler {
    public static PortHandler instance;

    public static void init(int portNum) {
        if (instance != null) {
            throw new RuntimeException("Port instance can only init once");
        }
        instance = new PortHandler(portNum);
    }

    public static PortHandler getInstance() {
        if (instance == null) {
            throw new RuntimeException("Port instance is not init yet");
        }
        return instance;
    }

    public PortHandler(int portNum) {
        this.portNum = portNum;
    }

    private int portNum;

    public int newPortNum () {
        return portNum++;
    }
}

