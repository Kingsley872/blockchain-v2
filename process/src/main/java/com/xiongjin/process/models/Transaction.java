package com.xiongjin.process.models;

public class Transaction {
    private int senderId;
    private int receiverId;
    private int amt;

    public Transaction(int senderId, int receiverId, int amt) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amt = amt;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public void setAmt(int amt) {
        this.amt = amt;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public int getAmt() {
        return amt;
    }

    public String toString () {
        String result;
        return "{" + senderId + "," + receiverId + "," + amt + "}";
    }
}
