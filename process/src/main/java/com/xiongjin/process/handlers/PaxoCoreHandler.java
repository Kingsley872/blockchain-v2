package com.xiongjin.process.handlers;

import com.google.gson.Gson;
import com.xiongjin.process.models.Block;
import com.xiongjin.process.models.ProcessMessage;
import com.xiongjin.process.models.Transaction;
import com.xiongjin.process.models.Tuple;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class PaxoCoreHandler {

    public static PaxoCoreHandler instance;
    public static void init(int id) {
        if (instance != null) {
            throw new RuntimeException("You can only init Paxo handler once");
        }
        instance = new PaxoCoreHandler(id);
    }
    public static PaxoCoreHandler getInstance() {
        if (instance == null) {
            throw new RuntimeException("Paxo handler init yet");
        }
        return instance;
    }
    private PaxoCoreHandler(int id) {
        this.id = id;
        ballotNum = new Tuple(0,0,0);
        acceptNum = new Tuple(0,0,0);
        acceptVal = "none";
        initVal = "none";
        blockChain = null;
        balance = 100;
        balanceChecking = 0;
        depth = 0;

        ACKMajorityCount = 1;
        ACKMajorityCheck = false;
        acceptedMajorityCount = 1;
        acceptedMajorityCheck = false;
    }

    private final int id;
    private Tuple ballotNum;
    private Tuple acceptNum;
    private String acceptVal;
    private String initVal;
    private int ACKMajorityCount;
    private boolean ACKMajorityCheck;
    private int acceptedMajorityCount;
    private boolean acceptedMajorityCheck;
    private Block blockChain;
    private int depth;
    private int balance;
    private int balanceChecking;
    private Gson gson = new Gson();
    private boolean receiveFromMajority = false;
    private final Queue<Transaction> pendingTransaction = new LinkedList<>();

//    public void resetFollowerPaxo() {
//        acceptVal = "none";
//    }

    public boolean addTransaction(Transaction t) {
        balanceChecking += t.getAmt();
        if (balanceChecking <= balance) {
            pendingTransaction.add(t);
            return true;
        } else {
            System.out.println("Transaction drop, low balance.");
            return false;
        }
    }



    public String getPrepareMessage() throws NoSuchAlgorithmException {
        System.out.println("--> Get prepare message");
        String message;
        ProcessMessage processMessage;
        ballotNum.incrementFirst();
        ballotNum.setSecond(id);
        ballotNum.setThird(depth);
        processMessage = new ProcessMessage("prepare",
                                            ballotNum.getFirst(),
                                            ballotNum.getSecond(),
                                            ballotNum.getThird(),
                                            acceptVal);
        message = gson.toJson(processMessage);

        initVal = generateAcceptVal();
        if (initVal != null) {
            acceptVal = initVal;
            ACKMajorityCheck = false;
            return message;
        } else {
            return null;
        }
//        return message;
    }

    private String generateAcceptVal() throws NoSuchAlgorithmException {
        System.out.println("--> Generate acceptVal");
        String result = null;
        String transacToString;
        String nonce;
        String hash;
        NonceHandler nonceHandler;
        HashHandler hashHandler;
        Block newBlock;

        if (!pendingTransaction.isEmpty()) {
            ArrayList<Transaction> tmp = new ArrayList(pendingTransaction);
            TransactionsHandler transactionsHandler = new TransactionsHandler();
            transacToString = transactionsHandler.transacListToString(tmp);

            if (blockChain == null) {
                nonceHandler = new NonceHandler(10, tmp, "");
                nonce = nonceHandler.getNonce();
                hash = "";
                newBlock = new Block(null, tmp, nonce, hash);
            } else {
                nonceHandler = new NonceHandler(10, tmp, blockChain.getHash());
                nonce = nonceHandler.getNonce();
                hashHandler = new HashHandler();
                hash = hashHandler.generateHash(transacToString + nonce);
                newBlock = new Block(null, tmp, nonce, hash);
            }
            result = gson.toJson(newBlock);
        }

        return result;
    }

    public ProcessMessage handleIncomeMessage(String message, int proNum) throws InterruptedException, NoSuchAlgorithmException {
        int majorityNum;
        majorityNum = proNum / 2 + 1;
        ProcessMessage processMessage;
        ProcessMessage returnMessage;
        processMessage = gson.fromJson(message, ProcessMessage.class);
        switch (processMessage.getMessageType()) {
            case "prepare":
                if (processMessage.getDepth() >= depth) {
//                if (processMessage.getDepth() >= ballotNum.getThird()) {
                    if (processMessage.getSeqNum() > ballotNum.getFirst() || (processMessage.getSeqNum() == ballotNum.getFirst() && processMessage.getPro_id() > ballotNum.getSecond())) {
                        ballotNum.setFirst(processMessage.getSeqNum());
                        ballotNum.setSecond(processMessage.getPro_id());
                        ballotNum.setThird(processMessage.getDepth());
                        returnMessage = new ProcessMessage("ACK",
                                                            ballotNum.getFirst(),
                                                            ballotNum.getSecond(),
                                                            ballotNum.getThird(),
                                                            acceptVal);
                        return returnMessage;
                    }
                } else {
                    returnMessage = new ProcessMessage("low_depth",
                            -1,
                            processMessage.getPro_id(),
                            depth,
                            "");
                    return returnMessage;
                }
                return null;
            case "ACK":
                ACKMajorityCount++;

                if (!processMessage.getVal().equals("none") && ACKMajorityCount <= majorityNum) {
                    acceptVal = processMessage.getVal();
                }

//                System.out.println(">>>> " + ACKMajorityCount + " >>>> " + majorityNum + " >>>> " + ACKMajorityCheck);
                if (ACKMajorityCount >= majorityNum && !ACKMajorityCheck) {
                    ACKMajorityCheck = true;
                    returnMessage = new ProcessMessage("accept",
                                                        ballotNum.getFirst(),
                                                        ballotNum.getSecond(),
                                                        ballotNum.getThird(),
                                                        acceptVal);
                    acceptedMajorityCheck = false;
                    return returnMessage;
                }
                return null;
            case "accept":
                if ((processMessage.getSeqNum() == ballotNum.getFirst() && processMessage.getPro_id() == ballotNum.getSecond())
                     || (processMessage.getSeqNum() > ballotNum.getFirst() || (processMessage.getSeqNum() == ballotNum.getFirst() && processMessage.getPro_id() > ballotNum.getSecond()))) {
                    acceptNum.setFirst(processMessage.getSeqNum());
                    acceptNum.setSecond(processMessage.getPro_id());
                    acceptNum.setThird(processMessage.getDepth());
                    acceptVal = processMessage.getVal();
                    returnMessage = new ProcessMessage("accepted",
                                                        acceptNum.getFirst(),
                                                        acceptNum.getSecond(),
                                                        acceptNum.getThird(),
                                                        acceptVal);
                    return returnMessage;
                }
                return null;

            case "accepted":
//                majorityNum = proNum / 2 + 1;
                acceptedMajorityCount++;
                System.out.println(">>>> " + acceptedMajorityCount + " >>>> " + majorityNum + " >>>> " + acceptedMajorityCheck);
                if (acceptedMajorityCount >= majorityNum && !acceptedMajorityCheck) {
                    acceptedMajorityCheck = true;
                    System.out.println("--> Generating decide message at acceptedMajorityCount " + acceptedMajorityCount);
                    addBlockToChain(gson.fromJson(processMessage.getVal(), Block.class));
                    // depth update
                    returnMessage = new ProcessMessage("decide",
                                                        ballotNum.getFirst(),
                                                        ballotNum.getSecond(),
                                                        ballotNum.getThird(),
                                                        processMessage.getVal());

                    updateBalanceWithSingleBlock(processMessage.getVal());

                    acceptVal = "none";
                    balanceChecking = 0;
                    ACKMajorityCount = 1;
                    acceptedMajorityCount = 1;

                    System.out.println("****" + processMessage.getVal());
                    System.out.println("****" + initVal);
                    System.out.println("**** clean list check " + processMessage.getVal().equals(initVal));
                    if (processMessage.getVal().equals(initVal)) {
                        pendingTransaction.clear();
                    }

                    return returnMessage;
                }
                return null;

            case "decide":
                addBlockToChain(gson.fromJson(processMessage.getVal(), Block.class));
                // reset follower paxo
                acceptVal = "none";

                System.out.println("****" + processMessage.getVal());
                System.out.println("****" + initVal);
                if (processMessage.getVal().equals(initVal)) {
                    pendingTransaction.clear();
                }
                updateBalanceWithSingleBlock(processMessage.getVal());
                return null;

            case "low_depth":
                returnMessage = new ProcessMessage("need_update",
                        -1,
                        -1,
                        processMessage.getDepth(),
                        "");
                System.out.println("--> Clean pending transactions");
                pendingTransaction.clear();
                return returnMessage;

            case "fail_link":
            case "fix_link":
            case "fail_process":
            case "fix_process":
                return processMessage;
            case "request_blockchain":
                processMessage.setMessageType("latest_blockchain");
                processMessage.setDepth(depth);
                processMessage.setVal(gson.toJson(blockChain));
                return processMessage;

            case "latest_blockchain":
                updateLatestBlockchain(processMessage.getVal());
                depth = processMessage.getDepth();
//                Block block = gson.fromJson(processMessage.getVal(), Block.class);
//                this.blockChain = block;
                System.out.println("--> Updated latest blockchain");
                return null;
            default:
                return null;
        }
    }

    private void addBlockToChain(Block block) {
        Block newBlock = block;
        if (blockChain != null) {
            newBlock.setPreviousBlock(blockChain);
        } else {
            newBlock.setPreviousBlock(null);
        }
        depth++;
        blockChain = newBlock;
    }


    public void printBlockChain() {
        System.out.println("==> Print Block Chain: ");
        if (blockChain != null) {
            System.out.println(blockChain.toString());
        } else {
            System.out.println("There is no block");
        }
    }

    public void printBalance() {
        System.out.println("==> Print Balance: $" + balance);
    }

    public void printPendingQueue() {
        if (!pendingTransaction.isEmpty()) {
            TransactionsHandler th = new TransactionsHandler();
            String output = th.transacListToString(new ArrayList(pendingTransaction));
            System.out.println(output);
        }
    }

    public int sizeOfPendingQueue() {
        return pendingTransaction.size();
    }

    public boolean pendingQueueIsEmpty() {
        return pendingTransaction.isEmpty();
    }

    public int getDepth() {
        return depth;
    }


    private void updateLatestBlockchain(String val) {
        Block latestBlockchain = gson.fromJson(val, Block.class);
        Block tmp = latestBlockchain;
        ArrayList<Transaction> tmpList;

        if (blockChain == null) {
            while (tmp != null) {
                tmpList = tmp.getTransacList();
                calculateBalance(tmpList);
                tmp = tmp.getPreviousBlock();
            }
        } else {
            while (!tmp.getHash().equals(blockChain.getHash())) {
                tmpList = tmp.getTransacList();
                calculateBalance(tmpList);
                tmp = tmp.getPreviousBlock();
            }
        }

        blockChain = latestBlockchain;
    }

    private void calculateBalance(ArrayList<Transaction> tList) {
        for (Transaction t : tList) {
            if (t.getSenderId() == this.id) {
                balance -= t.getAmt();
            } else if (t.getReceiverId() == this.id) {
                balance += t.getAmt();
            }
        }
    }

    private void updateBalanceWithSingleBlock(String val) {
        Block tmpBlock = gson.fromJson(val, Block.class);
        for (Transaction t: tmpBlock.getTransacList()) {
            if (t.getSenderId() == id) {
                this.balance -= t.getAmt();
            } else if (t.getReceiverId() == id) {
                this.balance += t.getAmt();
            }
        }
    }
}

