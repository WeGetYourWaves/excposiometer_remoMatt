package com.example.matthustahli.radarexposimeter;

import java.io.Serializable;
import java.util.Arrays;
import static java.lang.Math.abs;


/**
 * Created by Remo Steiner on 27.09.2016.
 * used to convert the data given by the device from the response unit to V/m
 */
public class Calibration implements Serializable{

    //public fields of Class Calibration
    /**
     * cali_table: 2d Array with calibration values
     * cali_table[frequency][magnitude]
     * in the 1d Array cali_table[][0] the possible frequencies are stored (in MHz)
     * in the 1d Array cali_table[0][] the field strengths (in V/m) are stored
     * in cali_table[3][2] the magnitude measured by the device corresponding to
     * the third frequency and the second field_strength is stored
     */
    int[][] cali_table;
    int freq_indexes;           //length of array cali_table [][i] for all i
    int strength_indexes;           //length of array cali_table [i][] for all i



    //Constructors of Class Calibration
    //Constructor
    public Calibration (int init_cali_table[][]){

        freq_indexes = init_cali_table.length;
        strength_indexes = init_cali_table[0].length;
        cali_table = init_cali_table;
    }

    //Default Constructor (for testing purposes)
    public Calibration (){

        //set length of default_cali_table[][]
        freq_indexes = 100;
        strength_indexes = 1000;
        int default_cali_table[][] = new int[freq_indexes][strength_indexes];
        default_cali_table[0][0] = 0; //no information here

        //initialize frequencies in cali_table[][0] (in MHz)
        for (int freq = 1; freq < freq_indexes; freq++){
            default_cali_table[freq][0] = 1000 + 100*(freq);
        }

        //initialize magnitudes in cali_table[0][] (response unit)
        for (int magn = 1; magn < strength_indexes; magn++){
            default_cali_table[0][magn] = 2000 + 50*(magn);
        }

        //initialize corresponding field strengths (in V/m)
        for(int freq = 1; freq < freq_indexes; freq++){
            for(int magn = 1; magn < strength_indexes; magn++){
                default_cali_table[freq][magn] = freq*1000 + magn;
            }
        }

        //set default_cali_table as cali_table
        cali_table = default_cali_table;
    }



    //Public Methods of Class Calibration
    //set values of cali_values
    public void setCal_values (int new_cali_table [][]){

        cali_table = new_cali_table;
        freq_indexes = new_cali_table.length;
        strength_indexes = new_cali_table[0].length;
    }

    /**
     * returns the field strength (V/m) of a magnitude (abs_magn, response unit)
     * measured by the device in a certain frequency (abs_freq, MHz).
     * if magnitude out of bound it returns -2 (too big) or -3 (too small)
     * if frequency out of bound it returns -1
     */
    public double get_field_strength (int abs_freq, int abs_magn){

        double field_strength;

        //frequency out of bound
        if (abs_freq > cali_table[freq_indexes - 1][0] || abs_freq < cali_table[1][0]){

            return -1;
        }

        //create freq_array
        double[] freq_array = new double[freq_indexes];
        for (int j = 0; j < freq_indexes; ++j){

            freq_array[j] = cali_table[j][0];
        }
        //search for the index in the freq_array_array corresponding to the abs_freq
        int freq_index = Arrays.binarySearch(freq_array , 1, freq_indexes, abs_freq);

        /**
         * abs_freq not in cali_table (no entry in the cali_table for the wished frequency)
         * If wished frequency is between two stored frequencies it chooses nearest possible stored frequency in cali_table.
         */
        if (freq_index <= 0){
            freq_index = -freq_index - 1;
            if (abs(cali_table[freq_index - 1][0] - abs_freq) < abs(cali_table[freq_index][0] - abs_freq)){
                freq_index = freq_index -1;
            }
        }

        //magnitude too small
        if (abs_magn > cali_table[freq_index][strength_indexes - 1]){

            return -2;
        }

        //magnitude too big
        if (abs_magn < cali_table[freq_index][1]){

            return -3;
        }

        //create magn_array
        int[] magn_array = new int[strength_indexes];
        for (int j = 0; j < strength_indexes; ++j) {

            magn_array[j] = cali_table[freq_index][j];
        }
        //search for the index in the magn_array corresponding to the abs_magn
        int strength_index = Arrays.binarySearch(magn_array, 1, strength_indexes, abs_magn);


        /**
         * abs_magn not in cali_table (no entry in the cali_table for the wished magnitude)
         * interpolates the the field_strength
         */
        if(strength_index <= 0){
            strength_index = -strength_index - 1;
            field_strength = interpolate(abs_magn, strength_index, freq_index);
        }

        else {
            field_strength = cali_table[0][strength_index];        //chooses corresponding field strength
        }

        return field_strength/1000;     //in V/m
    }

    /**
     * Interpolates the field strength by linearizing the
     * magnitude and field strength relation between to known points.
     * Returns the interpolated field strength.
     */
    public double interpolate(double abs_magn, int strength_index, int freq_index){

        double left_value = cali_table [freq_index][strength_index - 1];
        double right_value = cali_table [freq_index][strength_index];
        double low_value = cali_table [0][strength_index - 1];
        double high_value = cali_table [0][strength_index];

        double linearized = low_value + (high_value - low_value)/(right_value - left_value)*(abs_magn - left_value);

        return linearized;

    }


}
