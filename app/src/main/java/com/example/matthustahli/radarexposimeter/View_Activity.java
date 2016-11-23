package com.example.matthustahli.radarexposimeter;

import java.util.Arrays;

import static java.lang.Thread.sleep;

/**
 * Created by Remo on 18.10.2016.
 */
public class View_Activity extends Activity_Superclass{
    Thread thread;
    private double[] Peak;
    private double[] RMS;
    WifiDataBuffer buffer;
    Activity_Superclass Cali_Tables;

    View_Activity(WifiDataBuffer buf, Activity_Superclass cali_tables, final int device_id, final int attenuator, final char measurement_type){

        this.buffer = buf;
        this.Cali_Tables = cali_tables;
        double[] array = new double[96];
        Arrays.fill(array, 1000);
        Peak = array;
        RMS = array;
        thread = new Thread(){
            public void run(){
                startMeasurement(device_id, attenuator, measurement_type);
            }
        };
        thread.start();
    }

    public void startMeasurement (int device_id, int attenuator, char measurement_type) {

        View_Packet_Trigger triggerClass = new View_Packet_Trigger(device_id,attenuator, measurement_type);
        byte[] triggerPacket = triggerClass.get_packet();

        //successful? => test boolean
        buffer.enqueue_ToESP(triggerPacket);

        while(true) {
            while (!buffer.isDataWaiting_FromESP()) {
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            byte[] packet_in = buffer.deque_FromESP();

            Data_Packet_Exposi packetExposi = new Data_Packet_Exposi(packet_in);

            int freq = packetExposi.get_frequency();
            int rms_exposi = packetExposi.get_rawData_rms();
            int peak_exposi = packetExposi.get_rawData_peak();

            double rms = Cali_Tables.get_rms(attenuator,freq, rms_exposi);
            double peak = Cali_Tables.get_peak(attenuator, freq, peak_exposi);
            updatePeak(peak, freq);
            updateRMS(rms, freq);
        }
    }

    private synchronized void updatePeak(double newPeak, int freq){
        Peak[((freq - 500) / 100)] = newPeak;
    }

    private synchronized void updateRMS(double newRMS, int freq){
        RMS[((freq - 500) / 100)] = newRMS;
    }

    public synchronized double[] readPeak(){
        return Peak;
    }

    public synchronized double[] readRMS(){
        return RMS;
    }

    public synchronized void stop(){
        thread.stop();
    }
}
