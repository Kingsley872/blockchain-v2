package com.xiongjin.process.models;

import java.util.ArrayList;

/*
* Block Model
* The blockchain representation can be seen as linked-list
* In Java, it can be seen as a block contains another block.
* */

public class Block {

    private Block previousBlock;
    private final ArrayList<Transaction> transacList;
    private final String nonce;
    private final String hash;

    public Block(Block previousBlock, ArrayList<Transaction> transacList, String nonce, String hash) {
        this.previousBlock = previousBlock;
        this.transacList = transacList;
        this.nonce = nonce;
        this.hash = hash;
    }

    public void setPreviousBlock(Block previousBlock) {
        this.previousBlock = previousBlock;
    }

    public Block getPreviousBlock() {
        return previousBlock;
    }

    public ArrayList<Transaction> getTransacList() {
        return transacList;
    }

//    public String getNonce() {
//        return nonce;
//    }

    public String getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return "Block{\n" +
                "\tpreviousBlock=" + previousBlock + "\n" +
                "\ttransacToString=" + transacList.toString() + "\n" +
                "\tnonce='" + nonce + '\'' + "\n" +
                "\thash='" + hash + '\'' + "\n" +
                "}\n";
    }

}
