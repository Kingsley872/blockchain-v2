package com.xiongjin.process.models;

/*
* User Message Model
* */

public class UserMessage {
    private String messageType;
    private String src_id;
    private String des_id;
    private String amt;

    public UserMessage(String messageType, String src_id, String des_id, String amt) {
        this.messageType = messageType;
        this.src_id = src_id;
        this.des_id = des_id;
        this.amt = amt;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getSrc_id() {
        return src_id;
    }

    public String getDes_id() {
        return des_id;
    }

    public String getAmt() {
        return amt;
    }

    public void setSrc_id(String src_id) {
        this.src_id = src_id;
    }

    public void setDes_id(String des_id) {
        this.des_id = des_id;
    }

    public void setAmt(String amt) {
        this.amt = amt;
    }
}
