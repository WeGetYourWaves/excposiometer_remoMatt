package com.example.matthustahli.radarexposimeter;

/**
 * Created by Remo on 04.11.2016.
 */

public class Ready_Packet_Exposi extends Packet_Exposimeter {

    Ready_Packet_Exposi(byte[] packet_in){
        packet = packet_in;
    }

    /*
    methods for accessing the data either in bytes or the corresponding data type
     */
    public byte[] get_prefixB(){
        return split_packet(0, 3);
    }

    public String get_prefix(){
        return String.valueOf(byteArray_to_charArray(get_prefixB()));
    }

    public byte[] get_packetTypeB(){
        return split_packet(4, 7);
    }

    public String get_packetType(){
        return String.valueOf(byteArray_to_charArray(get_packetTypeB()));
    }

    public byte[] get_device_idB(){
        return split_packet(8, 11);
    }

    public int get_device_id(){
        return byteArray_to_int(get_device_idB());
    }

    public byte get_attenuatorB(){
        return packet[12];
    }

    public int get_attenuator(){
        return get_attenuatorB() & 0xFF;
    }

    public byte[] get_reserved(){return split_packet(13, 24);}

    public byte get_battery_chargeB(){
        return packet[25];
    }

    public int get_battery_charge(){
        return (int) get_battery_chargeB() & 0xFF;
    }

    public byte[] get_battery_voltageB(){
        return split_packet(26, 27);
    }

    public int get_battery_voltage(){
        return byteArray_to_int(get_battery_voltageB());
    }

    public byte[] get_postfixB(){
        return split_packet(28, 31);
    }

    public String get_postfix(){
        return String.valueOf(byteArray_to_charArray(get_postfixB()));
    }
}
