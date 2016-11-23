package com.example.matthustahli.radarexposimeter;

import java.util.Arrays;

/**
 * Created by Remo on 15.10.2016.
 */
public class Build_Packet_Test extends Packet {

    public byte[] test_packet;

    Build_Packet_Test(int length){

        byte[] packet_in = new byte[length];
        test_packet= packet_in;

        if (length == 53423){
            for (int i = 0; i < 8; i++)
            {test_packet[i] = (byte)(97 + i);}
            for (int i = 8; i < 12; i++)
            {test_packet[i] = (byte)(1);}
            for (int i = 12; i < 76; i++)
            {test_packet[i] = (byte)('b');}
            for (int i = 76; i < 82; i++)
            {test_packet[i] = (byte)(4);}
            test_packet[82] = 3;
            test_packet[83] = 8;
            test_packet[85] = 100;
            test_packet[87] = 16;
            //packet type
            test_packet[88] = 'P';
            test_packet[6754] = 'P';
            test_packet[13420] = 'P';
            test_packet[20086] = 'P';
            test_packet[26752] = 'R';
            test_packet[33418] = 'R';
            test_packet[40084] = 'R';
            test_packet[46750] = 'R';
            //attenuators
            test_packet[89] = 0;
            test_packet[6755] = 1;
            test_packet[13421] = 2;
            test_packet[20087] = 3;
            test_packet[26753] = 0;
            test_packet[33419] = 1;
            test_packet[40085] = 2;
            test_packet[46751] = 3;
            //data
            for (int i = 90; i < 6753; i++){test_packet[i+1] = (byte) 0;}
            for (int i = 6756; i < 13419; i++){test_packet[i+1] = (byte) (1);}
            for (int i = 13422; i < 20085; i++){test_packet[i+1] = (byte) (2);}
            for (int i = 20088; i < 26751; i++){test_packet[i+1] = (byte) (3);}
            for (int i = 26754; i < 33417; i++){test_packet[i+1] = (byte) (4);}
            for (int i = 33420; i < 40083; i++){test_packet[i+1] = (byte) (5);}
            for (int i = 40086; i < 46749; i++){test_packet[i+1] = (byte) (6);}
            for (int i = 46752; i < 53415; i++){test_packet[i] = (byte) (7);}

            test_packet[53416] = 10;
            test_packet[53417] = (byte) 1000;
            for (int i = 53419; i < 53423; i++){test_packet[i] = (byte) ('e');}
        }
        else{
            Arrays.fill(test_packet, (byte) '3');
        }

    }
}
