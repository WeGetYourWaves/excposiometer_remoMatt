package com.example.matthustahli.radarexposimeter;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.lang.Byte;

import static java.lang.Math.pow;

/**
 * Created by Remo on 10.10.2016.
 * Contains all basic operations needed in packet parsing and generating.
 */
public class Packet {

    protected byte[] packet;

    protected byte[] int_to_byteArray (int Integr, int byteArray_length){

        byte[] byteArray = new byte[byteArray_length];
        if (Integr > pow((double) 2, (double) (8*byteArray_length)) - 1){
            Arrays.fill(byteArray,(byte) 0);
        }
        else {
            if (byteArray.length == 4){
                byteArray[0] = (byte)(Integr >> 24);
                byteArray[1] = (byte)(Integr >> 16);
                byteArray[2] = (byte)(Integr >> 8);
                byteArray[3] = (byte) Integr;
            }
            else if (byteArray.length == 2){
                byteArray[0] = (byte)(Integr >> 8);
                byteArray[1] = (byte)Integr;
            }
            else if (byteArray.length == 1){
                byteArray[0] = (byte) Integr;
            }

            // problems with wifiDataBuffer overflow: byteArray = ByteBuffer.allocate(byteArray_length).putInt(Integr).array();
        }

        return byteArray;
    }

    protected int byteArray_to_int (byte[] byteArray){

        int Integr = 0;

        if (byteArray.length == 2){
            Integr = (int) ((byteArray[0] & 0xFF) * pow(2, 8));
            Integr += byteArray[1] & 0xFF;
        }

        if (byteArray.length == 4){
            Integr = (int) ((byteArray[0] & 0xFF) * pow(2, 24));
            Integr += (int) ((byteArray[1] & 0xFF) * pow(2, 16));
            Integr += (int) ((byteArray[2] & 0xFF) * pow(2, 8));
            Integr += byteArray[3] & 0xFF;
        }

        return Integr;
    }

    protected int[] byteArray_to_intArray (byte[] byteArray, int bytes_per_int){
        byte[] oneInt = new byte[bytes_per_int];
        int[] result = new int[byteArray.length/bytes_per_int];
        for (int i = 0; i < byteArray.length/bytes_per_int; ++i){
            for (int j = 0; j < bytes_per_int; ++j){
                oneInt[j] = byteArray[bytes_per_int*i + j];
            }
            result[i] = byteArray_to_int(oneInt);
        }
        return result;
    }

    protected byte[] charArray_to_byteArray (char[] CharArray){

        byte[] byteArray = new byte[CharArray.length];

        for (int i = 0; i < CharArray.length; i++){

            byteArray[i]=(byte)CharArray[i];
        }
        return byteArray;
    }

    protected char[] byteArray_to_charArray (byte[] byteArray){

        char[] charArray = new char[byteArray.length];

        for (int i = 0; i < charArray.length; i++){

            charArray[i]=(char)byteArray[i];
        }
        return charArray;
    }

    protected byte[] get_packet(){
        return packet;
    }
}
