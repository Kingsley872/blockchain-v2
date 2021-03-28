package com.xiongjin.process.handlers;

import com.xiongjin.process.models.Transaction;

import javax.xml.transform.Transformer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

public class NonceHandler {

    private final int nonceLength;
    private final ArrayList<Transaction> transacList;
    private final String h_n;

    public NonceHandler(int nonceLength, ArrayList<Transaction> transacList, String h_n) {
        this.nonceLength = nonceLength;
        this.transacList = transacList;
        this.h_n = h_n;
    }

    private String getAlphaNumericString() {

        // length is bounded by 256 Character
        int tmpLength = nonceLength;

        final byte[] array = new byte[256];
        new Random().nextBytes(array);

        final String randomString = new String(array, Charset.forName("UTF-8"));

        // Create a StringBuffer to store the result
        final StringBuffer r = new StringBuffer();

        // Append first 20 alphanumeric characters
        // from the generated random String into the result
        for (int k = 0; k < randomString.length(); k++) {

            final char ch = randomString.charAt(k);

            if (((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) && (tmpLength > 0)) {

                r.append(ch);
                tmpLength--;
            }
        }

        // return the resultant string
        return r.toString();
    }

    public String getNonce() throws NoSuchAlgorithmException {
        // size of random alphanumeric string
//        boolean generating = true;
        String nonce;
        String h = "";
        HashHandler hashHandler = new HashHandler();
        TransactionsHandler transactionsHandler = new TransactionsHandler();
        String transacListToString = transactionsHandler.transacListToString(transacList);

        // Get and display the alphanumeric string
        while (true) {
            nonce = getAlphaNumericString();
            h = hashHandler.generateHash(transacListToString + nonce + h_n);
            System.out.println("h_n: " + h);
            char last = h.charAt(h.length() - 1);
            if (last == '0' || last == '1' || last == '2' || last == '3' || last == '4') {
                System.out.println("Nonce: " + nonce);
                return nonce;
            }
        }
    }
}
