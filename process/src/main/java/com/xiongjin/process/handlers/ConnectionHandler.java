package com.xiongjin.process.handlers;

import com.google.gson.Gson;
import com.xiongjin.process.Process;
import com.xiongjin.process.models.ProcessMessage;
import com.xiongjin.process.models.Transaction;
import com.xiongjin.process.models.UserMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/*
* Connection Handler handles all connection operations
*   it is also a transfer center all messages including messages from node to node, and user to the application
* */
public class ConnectionHandler {

    private int id;
    private int port;
    private final InetAddress srcIP = InetAddress.getByName("localhost");
    private final InetAddress desIP = InetAddress.getByName("localhost");
    private static final int MAX_NODES = 5;
    private int proNum = 5;
    private final Timer timer1 = new Timer();
    private final Timer timer2 = new Timer();
    private boolean timered = false ;
//    private static boolean allowAddingTransac = true;
    private boolean processIsFail;

    // Information: all nodes in the network
    // normally it is from discover server. It is hard coded because of testing reason.
    private final HashMap<Integer, Integer> proInfoList = new HashMap<Integer, Integer>() {
        {
            put(1, 1234);
            put(2, 1235);
            put(3, 1236);
            put(4, 1237);
            put(5, 1238);
        }
    };

    // All the input stream and output objects from all connection
    private final HashMap<Integer, DataOutputStream> outstreamList = new HashMap<>();
    private final HashMap<Integer, DataInputStream> instreamList = new HashMap<>();
    // this is the copy of all input stream and output stream
    // those two copies only for disconnecting operation.
    private HashMap<Integer, DataOutputStream> outstreamListCopy;
    private HashMap<Integer, DataInputStream> instreamListCopy;

    // User actions are converted into events, and events are store here
    private static final Queue<String> userEventQueue= new LinkedList<>();
    private Gson gson = new Gson();


    // All functions related to the connection and message transferring
    public ConnectionHandler (String id, String port) throws UnknownHostException {
        this.id = Integer.parseInt(id);
        this.port = Integer.parseInt(port);
        PaxoCoreHandler.init(Integer.parseInt(id));

        processIsFail = false;
        outstreamListCopy = null;
        instreamListCopy = null;
    }

    public void start() throws IOException, InterruptedException {
//        identifyMyself();
        System.out.println("==> My id: " + id + " My portNum: " + port);
        // each node creates a listener thread to take care of incoming connecting requests
        createConnectListenerRunnable();
        // wait until all nodes are activated
        Thread.sleep(20000);
        // connection nodes base on the all nodes information "proInfoList"
        connectEachOther();

        // pause little bit for debugging reason
        while (instreamList.size() != 4 || outstreamList.size() != 4) {
            // wait
            Thread.sleep(500);
        }

        // Display information of all connection after done connecting process
        System.out.println("==> Done connect to each other ");
        System.out.println(instreamList.keySet());
        System.out.println(outstreamList.keySet());
        outstreamListCopy = new HashMap(outstreamList);
        instreamListCopy = new HashMap(instreamList);

        startUserListener();
    }

    private void createConnectListenerRunnable() throws IOException {
        // each node is acting like a server to accept connection request
        System.out.println("==> Create connection Listener");
        final ServerSocket serverSocket = new ServerSocket(this.port);
        Thread connectionListener = new Thread(() -> {
            Socket socket;
            String message;
            DataInputStream dis;
            while (true) {
                try {
                    // open socket and make connections based on incoming requests
                    socket = serverSocket.accept();
                    dis = new DataInputStream(socket.getInputStream());
                    message = dis.readUTF();

                    instreamList.put(Integer.parseInt(message), dis);
                    System.out.println("Connection: " + message + "->" + id);

                    // for each incoming connection, a message listener is created
                    // this is for disconnect operation
                    createMessageListenerRunnable(dis);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        connectionListener.start();
    }

    /*
    * Create listening thread for each connection
    *   1. the incoming process is passed into Paxo core handler
    *   2. the Paxo core handler process the incoming messages and pass the result back to here
    *   3. the listener sends result back to the sender
    * */
    private void createMessageListenerRunnable(DataInputStream dis) {
        System.out.println("==> Connect to other as client");
        Thread messageListener = new Thread(() -> {
            String message = null;
            ProcessMessage processMessage;
            while (true) {
                if (!processIsFail) {
                    try {
                        message = dis.readUTF();
                        System.out.println("==> Get message: " + message);
                        Thread.sleep(5000);
                        // NOTE: the listener does not need to know what is the message
                        processMessage = PaxoCoreHandler.getInstance().handleIncomeMessage(message, proNum);

                        // NOTE: the listener take care of the processed result based on Paxo core handler's command.
                        if (processMessage != null) {
                            switch (processMessage.getMessageType()) {
                                case "ACK":
                                    System.out.println("==> Send ACK to proposer");
                                    sendToSenderMessage(processMessage);
                                    break;
                                case "accept":
                                    System.out.println("==> Broadcast accept message: " + gson.toJson(processMessage));
                                    broadcastMessage(processMessage);
                                    break;
                                case "accepted":
                                    System.out.println("==> Broadcast accepted message: " + gson.toJson(processMessage));
                                    sendToSenderMessage(processMessage);
                                    break;
                                case "decide":
//                                    allowAddingTransac = true;
                                    System.out.println("==> Broadcast decide message: " + gson.toJson(processMessage));
                                    broadcastMessage(processMessage);
                                    break;
                                case "low_depth":
                                case "latest_blockchain":
                                    sendToSenderMessage(processMessage);
                                    break;
                                case "need_update":
                                    System.out.println("==> BlockChain need to update");
                                    System.out.println("==> This depth->" + PaxoCoreHandler.getInstance().getDepth());
                                    System.out.println("==> Current depth->" + processMessage.getDepth());
//                                    allowAddingTransac = true;
                                    break;
                                case "fail_link":
                                    updateFailConnection(processMessage.getSeqNum(), processMessage.getPro_id());
                                    break;
                                case "fix_link":
                                    updateFixConnection(processMessage.getSeqNum(), processMessage.getPro_id());
                                    break;
                                case "fail_process":
                                    updateFailProcess(processMessage);
                                    break;
                                case "fix_process":
                                    updateFixProcess(processMessage);
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch(IOException | InterruptedException | NoSuchAlgorithmException e){
                        e.printStackTrace();
                    }
                }
            }
        });
        messageListener.start();
    }

    /*
    * Sends connecting requests except itself
    * */
    private void connectEachOther() throws IOException, InterruptedException {
        DataOutputStream dos;
        for (int i = 1; i <= MAX_NODES; i++) {
            if (this.id != i) {
                Socket socket = new Socket(desIP, proInfoList.get(i));
                dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF(String.valueOf(this.id));
                outstreamList.put(i, dos);
                System.out.println("Connection: " + id + "->" + i);
                Thread.sleep(1000); // for debug reason
            }
        }
    }

    /*
    *  Creates user listening thread
    *   1. it transfers user events into Paxo core handler
    *   2. Paxo core handler's process user message and sends to other components
    * */
    private void startUserListener() {
        System.out.println("==> Start user listener thread");
        Gson gson = new Gson();
//        UserMessage userMessage = null;
        Thread userListener = new Thread(() -> {
            int src_id;
            int des_id;
            int amt;
            ProcessMessage processMessage;
            while(true) {
                if (!userEventQueue.isEmpty()) {
                    UserMessage userMessage = gson.fromJson(userEventQueue.poll(), UserMessage.class);
                    switch (userMessage.getMessageType()) {
                        case "print_id":
                            System.out.println("==> This ID: " + this.id);
                            break;
                        case "money_transfer":
                            src_id = Integer.parseInt(userMessage.getSrc_id());
                            des_id = Integer.parseInt(userMessage.getDes_id());
                            amt = Integer.parseInt(userMessage.getAmt());
                            if (PaxoCoreHandler.getInstance().addTransaction(new Transaction(src_id, des_id, amt))) {
                                startLeaderElection();
                            }
                            break;
                        case "fail_link":
                            processMessage = new ProcessMessage("fail_link", Integer.parseInt(userMessage.getSrc_id()), Integer.parseInt(userMessage.getDes_id()), -1, "");
                            try {
                                broadcastMessage(processMessage);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            updateFailConnection(Integer.parseInt(userMessage.getSrc_id()), Integer.parseInt(userMessage.getDes_id()));
                            break;
                        case "fix_link":
                            processMessage = new ProcessMessage("fix_link", Integer.parseInt(userMessage.getSrc_id()), Integer.parseInt(userMessage.getDes_id()), -1, "");
                            try {
                                broadcastMessage(processMessage);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            updateFixConnection(Integer.parseInt(userMessage.getSrc_id()), Integer.parseInt(userMessage.getDes_id()));
                            break;

                        case "fail_process":
//                            processMessage = new ProcessMessage("fail_process", -1, Integer.parseInt(userMessage.getSrc_id()), -1, "");
//                            try {
//                                broadcastMessage(processMessage);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            updateFailProcess(processMessage);
                            System.out.println("==> Process " + this.id + " is down");
                            processIsFail = true;
                            break;
                        case "fix_process":
//                            processMessage = new ProcessMessage("fix_process", -1, Integer.parseInt(userMessage.getSrc_id()), -1, "");
//                            updateFixProcess(processMessage);
//                            try {
//                                broadcastMessage(processMessage);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                            System.out.println("==> Process " + this.id + " is alive now");
                            try {
                                requestLatestBlockChain();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            processIsFail = false;
                            break;
                        case "print_blockchain":
                            System.out.println("~~> Get user event print blockchain");
                            PaxoCoreHandler.getInstance().printBlockChain();
                            break;
                        case "print_balance":
                            PaxoCoreHandler.getInstance().printBalance();
                            break;
                        case "print_queue":
                            PaxoCoreHandler.getInstance().printPendingQueue();
                            break;
                        case "update_blockchain":
                            System.out.println("~~> Get user event blockchain update");
                            try {
                                requestLatestBlockChain();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            break;
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        userListener.start();
    }

    /*
    * User interaction thread grape user's action (event) and add it into event queue by using this function
    * */
    public static void addUserEvent(String userEvent){
        userEventQueue.add(userEvent);
    }
    //End of all functions related to the connection and message transferring



    // All functions related to Blockchain protocol
    private void startLeaderElection() {
        // check if leader election is started or not
        if (PaxoCoreHandler.getInstance().sizeOfPendingQueue() == 1 && !timered) {
            // generate random wait time
            int waitTime;
            Random random = new Random();
            TimerTask timerTask1 = new TimerTask() {
                @Override
                public void run() {
                    try {
                        propose();
                    } catch (IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            };

            TimerTask timerTask2 = new TimerTask() {
                @Override
                public void run() {
                    retryLeaderElection();
                }
            };

            waitTime = random.nextInt(5) + 10;
            System.out.println("Random wait time " + waitTime + " sec");
            // timer1: pause then leader election
            timer1.schedule(timerTask1, waitTime * 1000);
            // my transaction might or might not promoted, double check after some seconds
            timer2.schedule(timerTask2,35000);
            timered = true;
        }
    }

    private void retryLeaderElection() {
        System.out.println("===> Retry");
        System.out.println("===> Retry check " + !PaxoCoreHandler.getInstance().pendingQueueIsEmpty());
        if (!PaxoCoreHandler.getInstance().pendingQueueIsEmpty()) {
            int waitTime;
            Random random = new Random();
            TimerTask timerTask1 = new TimerTask() {
                @Override
                public void run() {
                    try {
                        propose();
                    } catch (IOException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            };
            TimerTask timerTask2 = new TimerTask() {
                @Override
                public void run() {
                    startLeaderElection();
                }
            };

            waitTime = random.nextInt(5) + 10;
            System.out.println("Random wait time " + waitTime + " sec");
            timer1.schedule(timerTask1, waitTime * 1000);
            timer2.schedule(timerTask2,35000);
            timered = true;
        }
    }

    private void propose() throws IOException, NoSuchAlgorithmException {
        System.out.println("==> Start leader election");
        timered = false;
//        allowAddingTransac = false;
        String outMessage;
        DataOutputStream dos;
        outMessage = PaxoCoreHandler.getInstance().getPrepareMessage();
        if (outMessage != null) {
            System.out.println(outMessage);

            for (Map.Entry<Integer, DataOutputStream> entry : outstreamList.entrySet()) {
                dos = entry.getValue();
                if (dos != null) {
                    dos.writeUTF(outMessage);
                }
            }
        }
    }

    private void sendToSenderMessage(ProcessMessage processMessage) throws IOException {
        DataOutputStream dos = outstreamList.get(processMessage.getPro_id());
        if (dos != null) {
            dos.writeUTF(gson.toJson(processMessage));
        }
    }

    private void broadcastMessage(ProcessMessage processMessage) throws IOException {
        DataOutputStream dos;
        for (Map.Entry<Integer,DataOutputStream> entry : outstreamList.entrySet()) {
            dos = entry.getValue();
            if (dos != null) {
                dos.writeUTF(gson.toJson(processMessage));
            }
        }

//        allowAddingTransac = true;
    }

    private void updateFailConnection(int src, int des) {
        if (src == this.id) {
            System.out.println("Disconnected from " + id + " to " + des);
            outstreamList.put(des, null);
            instreamList.put(des, null);
        } else if (des == this.id) {
            System.out.println("Disconnected from " + id + " to " + src);
            outstreamList.put(src, null);
            instreamList.put(src, null);
        }
        System.out.println(instreamList.keySet());
        System.out.println(instreamList.values());
        System.out.println(outstreamList.keySet());
        System.out.println(outstreamList.values());
    }

    private void updateFixConnection(int src, int des) {
        if (src == this.id) {
            System.out.println("Connecting from " + id + " to " + des);
            if (outstreamListCopy.get(des) != null) {
                outstreamList.put(des, outstreamListCopy.get(des));
            } else {
                throw new RuntimeException("~~> Already connected with des");
            }
            if (instreamListCopy.get(des) != null) {
                instreamList.put(des, instreamListCopy.get(des));
            } else {
                throw new RuntimeException("~~> Already connected with des");
            }
        } else if (des == this.id) {
            System.out.println("Connecting from " + id + " to " + src);
            if (outstreamListCopy.get(src) != null) {
                outstreamList.put(src, outstreamListCopy.get(src));
            } else {
                throw new RuntimeException("~~> Already connected with src");
            }
            if (instreamListCopy.get(src) != null) {
                instreamList.put(src, instreamListCopy.get(src));
            } else {
                throw new RuntimeException("~~> Already connected with src");
            }
        }
        System.out.println(instreamList.keySet());
        System.out.println(instreamList.values());
        System.out.println(outstreamList.keySet());
        System.out.println(outstreamList.values());
    }

    private void requestLatestBlockChain() throws IOException {
        System.out.println("==> Sending request for latest blockchain");
        ProcessMessage processMessage = new ProcessMessage("request_blockchain", -1, this.id, -1, "");
        DataOutputStream dos;
        boolean sentRequest = false;
        for (Map.Entry<Integer,DataOutputStream> entry : outstreamList.entrySet()) {
            dos = entry.getValue();
            if (dos != null && !sentRequest) {
                dos.writeUTF(gson.toJson(processMessage));
                sentRequest = true;
            }
        }
    }

    private void updateFailProcess(ProcessMessage processMessage) {
        System.out.println("==> Update fail process");
        int pro_id = processMessage.getPro_id();
        if (pro_id == this.id) {
            for (Map.Entry<Integer,DataOutputStream> entry : outstreamList.entrySet()) {
                entry.setValue(null);
            }
            for (Map.Entry<Integer,DataInputStream> entry : instreamList.entrySet()) {
                entry.setValue(null);
            }
            proNum = 0;
        } else {
            outstreamList.put(pro_id, null);
            instreamList.put(pro_id, null);
            proNum--;
        }
        System.out.println(instreamList.keySet());
        System.out.println(instreamList.values());
        System.out.println(outstreamList.keySet());
        System.out.println(outstreamList.values());
        System.out.println("==> Processes in the net: " + proNum);
    }

    private void updateFixProcess(ProcessMessage processMessage) {
        System.out.println("==> Update fix process");
        int pro_id = processMessage.getPro_id();
        if (pro_id == this.id) {
            for (Map.Entry<Integer,DataOutputStream> entry : outstreamList.entrySet()) {
                entry.setValue(outstreamListCopy.get(entry.getKey()));
            }
            for (Map.Entry<Integer,DataInputStream> entry : instreamList.entrySet()) {
                entry.setValue(instreamListCopy.get(entry.getKey()));
            }
            proNum = 5;
        } else {
            outstreamList.put(pro_id, outstreamListCopy.get(pro_id));
            instreamList.put(pro_id, instreamListCopy.get(pro_id));
            proNum++;
        }
        System.out.println(instreamList.keySet());
        System.out.println(instreamList.values());
        System.out.println(outstreamList.keySet());
        System.out.println(outstreamList.values());
        System.out.println("==> Processes in the net: " + proNum);
    }
    //End of functions related to Blockchain protocol
}

