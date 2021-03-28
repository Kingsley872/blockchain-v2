package com.xiongjin.process.models;

public class ProcessMessage {
    private String messageType;
    private int seqNum;
    private int pro_id;
    private int depth;
    private String val;

    public ProcessMessage(String messageType, int seqNum, int pro_id, int depth, String val) {
        this.messageType = messageType;
        this.seqNum = seqNum;
        this.pro_id = pro_id;
        this.depth = depth;
        this.val = val;
    }

    public String getMessageType() {
        return messageType;
    }

    public int getDepth() {
        return depth;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getPro_id() {
        return pro_id;
    }

    public String getVal() {
        return val;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public void setPro_id(int pro_id) {
        this.pro_id = pro_id;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setVal(String val) {
        this.val = val;
    }
}
