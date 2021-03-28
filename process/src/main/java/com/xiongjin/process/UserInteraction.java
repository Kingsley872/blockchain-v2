package com.xiongjin.process;

import com.google.gson.Gson;
import com.xiongjin.process.handlers.ConnectionHandler;
import com.xiongjin.process.models.Transaction;
import com.xiongjin.process.models.UserMessage;

import java.util.Scanner;
import java.util.StringTokenizer;

public class UserInteraction implements Runnable {

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String command;
        String contact;
        String messageType;
        String src_id;
        String des_id;
        String amt;
        Gson gson = new Gson();
        Transaction transaction;
        StringTokenizer st = null;
        UserMessage userMessage;

        while (true) {
            System.out.print("Enter Command: ");
            command = scanner.nextLine().trim();
            switch (command) {
                case "id":
                    messageType = "print_id";
                    userMessage = new UserMessage(messageType, "", "", "");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "mt":
                    System.out.print("Transaction detail(format: srcID,desID,Amount): ");
                    contact = scanner.nextLine().trim();
                    st = new StringTokenizer(contact, ",");
                    messageType = "money_transfer";
                    src_id = st.nextToken();
                    des_id = st.nextToken();
                    amt = st.nextToken();
                    if (Integer.parseInt(amt) <= 0) {
                        System.out.println("--> Entered incorrect amount");
                    } else {
                        userMessage = new UserMessage(messageType, src_id, des_id, amt);
                        ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    }
                    break;
                case "fail":
                    messageType = "fail_link";
                    System.out.print("Fail Link from scr to des: ");
                    contact = scanner.nextLine().trim();
                    st = new StringTokenizer(contact, ",");
                    src_id = st.nextToken();
                    des_id = st.nextToken();
                    userMessage = new UserMessage(messageType, src_id, des_id, "-1");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "fix":
                    messageType = "fix_link";
                    System.out.print("Fix Link from scr to des: ");
                    contact = scanner.nextLine().trim();
                    st = new StringTokenizer(contact, ",");
                    src_id = st.nextToken();
                    des_id = st.nextToken();
                    userMessage = new UserMessage(messageType, src_id, des_id, "-1");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "update":
                    messageType = "update_blockchain";
                    userMessage = new UserMessage(messageType, "", "", "");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "failp":
//                    messageType = "fail_process";
//                    System.out.print("Fail process id: ");
//                    contact = scanner.nextLine();
//                    userMessage = new UserMessage(messageType, contact,  "", "-1");
//                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
//                    break;
                    messageType = "fail_process";
                    userMessage = new UserMessage(messageType, "", "", "");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "fixp":
                    messageType = "fix_process";
//                    System.out.print("Enter pro id (must in in fail process): ");
//                    contact = scanner.nextLine().trim();
//                    userMessage = new UserMessage(messageType, contact,  "", "-1");
                    userMessage = new UserMessage(messageType, "", "", "");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "pbc":
                    messageType = "print_blockchain";
                    userMessage = new UserMessage(messageType, "", "", "");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "pb":
                    messageType = "print_balance";
                    userMessage = new UserMessage(messageType, "", "", "");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                case "pq":
                    messageType = "print_queue";
                    userMessage = new UserMessage(messageType, "", "", "");
                    ConnectionHandler.addUserEvent(gson.toJson(userMessage));
                    break;
                default:
                    System.out.println("--> Wrong command!");
                    break;
            }
        }
    }
}
