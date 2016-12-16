package com.example.matthustahli.radarexposimeter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by andre_eggli on 10/3/16.
 * Note that LinkedBlockingQueue<> implements a locking algorithm. No additional synchronisation is required. 
 * If Markus-Thread and Matthü-Thread try to dequeue/enqueue a wifiPackage at the same time, one of the Threads waits till the other is done.
 */

public class WifiDataBuffer implements Serializable {

    LinkedBlockingQueue<byte[]> ToESP; // DataType "WifiPackage" is a seperate class in this package.
    LinkedBlockingQueue<byte[]> FromESP;

    public WifiDataBuffer() {
        ToESP = new LinkedBlockingQueue<>(5); // Max Size = 5
        FromESP = new LinkedBlockingQueue<>(10); // Max 1000 unprocessed Packages at same time allowed
    }

    public boolean enqueue_ToESP(byte[] packet) {
        try{
            ToESP.add(packet);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            dequeue_ToESP();
            enqueue_ToESP(packet);
            SendErrorToActivity(1, "There's no need to hurry. Relax it.");
        }
        return true;
    }

    public byte[] dequeue_ToESP() {
        return ToESP.poll();
    }

    public boolean enque_FromESP(byte[] packet) {

        try {
            FromESP.add(packet);
        } catch (IllegalStateException e){
            e.printStackTrace();
            deque_FromESP();
            enque_FromESP(packet);
        }
        return true;
    }

    public byte[] deque_FromESP() {
        return FromESP.poll();
    }

    public boolean isDataWaiting_ToESP() {
        return !ToESP.isEmpty();
    }

    public boolean isDataWaiting_FromESP() {
        return !FromESP.isEmpty();
    }

    public void SendErrorToActivity (Integer Code, String Message){
        ByteArrayOutputStream errorbuffer = new ByteArrayOutputStream(134);
        try {
            errorbuffer.write("RD16EROR".getBytes());
            errorbuffer.write(new byte[]{0, Code.byteValue()});
            errorbuffer.write(Message.getBytes());
            for (int i = 0; i < 120-Message.length(); ++i){
                errorbuffer.write(((Integer) 0).byteValue());
            }
            errorbuffer.write("PEND".getBytes());
            enque_FromESP(errorbuffer.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
