package com.example.matthustahli.radarexposimeter;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.Serializable;

/**
 * Created by Remo on 20.10.2016.
 */
public class Activity_Superclass implements Serializable{

    public Calibration cali_NormRMS;
    public Calibration cali_NormPeak;
    public Calibration cali_att21RMS;
    public Calibration cali_att21Peak;
    public Calibration cali_att42RMS;
    public Calibration cali_att42Peak;
    public Calibration cali_LNA_RMS;
    public Calibration cali_LNA_Peak;

    Activity_Superclass (byte [] caliPack) {
        init_tables(caliPack);
    }

    public double get_rms(int attenuator, int freq, int rms_exposi){
        if (attenuator == 0){
            return cali_NormRMS.get_field_strength(freq, rms_exposi);
        }
        else if (attenuator == 1){
            return cali_att21RMS.get_field_strength(freq, rms_exposi);
        }
        else if (attenuator == 2){
            return cali_att42RMS.get_field_strength(freq, rms_exposi);
        }
        else return cali_LNA_RMS.get_field_strength(freq, rms_exposi);
    }

    public double get_peak(int attenuator, int freq, int peak_exposi){
        if (attenuator == 0){
            return cali_NormPeak.get_field_strength(freq, peak_exposi);
        }
        else if (attenuator == 1){
            return cali_att21Peak.get_field_strength(freq, peak_exposi);
        }
        else if (attenuator == 2){
            return cali_att42Peak.get_field_strength(freq, peak_exposi);
        }
        else return cali_LNA_Peak.get_field_strength(freq, peak_exposi);
    }

    public void init_tables(byte[] packet_in){

        Cal_Packet_Exposi exposiPacket = new Cal_Packet_Exposi(packet_in);

        char type1 = exposiPacket.get_table_measurement_type(1);
        char type2 = exposiPacket.get_table_measurement_type(2);
        char type3 = exposiPacket.get_table_measurement_type(3);
        char type4 = exposiPacket.get_table_measurement_type(4);
        char type5 = exposiPacket.get_table_measurement_type(5);
        char type6 = exposiPacket.get_table_measurement_type(6);
        char type7 = exposiPacket.get_table_measurement_type(7);
        char type8 = exposiPacket.get_table_measurement_type(8);
        int att1 = exposiPacket.get_table_attenuator(1);
        int att2 = exposiPacket.get_table_attenuator(2);
        int att3 = exposiPacket.get_table_attenuator(3);
        int att4 = exposiPacket.get_table_attenuator(4);
        int att5 = exposiPacket.get_table_attenuator(5);
        int att6 = exposiPacket.get_table_attenuator(6);
        int att7 = exposiPacket.get_table_attenuator(7);
        int att8 = exposiPacket.get_table_attenuator(8);

        save_table(type1, att1, exposiPacket.get_caliTable(1));
        save_table(type2, att2, exposiPacket.get_caliTable(2));
        save_table(type3, att3, exposiPacket.get_caliTable(3));
        save_table(type4, att4, exposiPacket.get_caliTable(4));
        save_table(type5, att5, exposiPacket.get_caliTable(5));
        save_table(type6, att6, exposiPacket.get_caliTable(6));
        save_table(type7, att7, exposiPacket.get_caliTable(7));
        save_table(type8, att8, exposiPacket.get_caliTable(8));
    }

    public void save_table(char type, int attenuator, int[][] cali_table){
        if(type == 'P'){
            if(attenuator == 0){
                cali_NormPeak = new Calibration(cali_table);
            }
            else if(attenuator == 1){
                cali_att21Peak = new Calibration(cali_table);
            }
            if(attenuator == 2){
                cali_att42Peak = new Calibration(cali_table);
            }
            else if(attenuator == 3){
                cali_LNA_Peak = new Calibration(cali_table);
            }
        }
        else if (type == 'R'){
            if(attenuator == 0){
                cali_NormRMS = new Calibration(cali_table);
            }
            else if(attenuator == 1){
                cali_att21RMS = new Calibration(cali_table);
            }
            if(attenuator == 2){
                cali_att42RMS = new Calibration(cali_table);
            }
            else if(attenuator == 3){
                cali_LNA_RMS = new Calibration(cali_table);
            }
        }
    }
}
