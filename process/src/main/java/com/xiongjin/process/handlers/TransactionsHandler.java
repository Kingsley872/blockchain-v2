package com.xiongjin.process.handlers;

import com.xiongjin.process.models.Transaction;

import java.util.ArrayList;

public class TransactionsHandler {

    public TransactionsHandler() {}
    
    public String transacListToString (ArrayList<Transaction> transacList) {
        String result = "[";
        for (int i = 0; i < transacList.size() - 1; i++) {
            result += transacList.get(i).toString() + ",";
        }
        result += transacList.get(transacList.size() - 1).toString();
        result += "]";
        return result;
    }
}
