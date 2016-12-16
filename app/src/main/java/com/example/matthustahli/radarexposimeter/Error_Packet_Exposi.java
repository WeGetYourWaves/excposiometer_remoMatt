package com.example.matthustahli.radarexposimeter;

/**
 * Created by Remo on 02.12.2016.
 * Implements all required methods to parse the error-packet.
 */

public class Error_Packet_Exposi extends Packet_Exposimeter {

    Error_Packet_Exposi(byte[] packet_in) {
        packet = packet_in;
    }

    /*
    methods for accessing the data either in bytes or the corresponding data type
     */
    public byte[] get_prefixB() {
        return split_packet(0, 3);
    }

    public String get_prefix() {
        return String.valueOf(byteArray_to_charArray(get_prefixB()));
    }

    public byte[] get_packetTypeB() {
        return split_packet(4, 7);
    }

    public String get_packetType() {
        return String.valueOf(byteArray_to_charArray(get_packetTypeB()));
    }

    public byte[] get_errorCodeB() {
        return split_packet(8, 9);
    }

    public int get_errorCode() {
        return byteArray_to_int(get_errorCodeB());
    }

    public byte[] get_errorMessageB() {
        return split_packet(10, 129);
    }

    public String get_errorMessage() {
        return String.valueOf(byteArray_to_charArray(get_errorMessageB()));
    }

    public byte[] get_postfixB() {
        return split_packet(130, 133);
    }

    public String get_postfix() {
        return String.valueOf(byteArray_to_charArray(get_postfixB()));
    }

}
