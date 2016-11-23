package com.example.matthustahli.radarexposimeter;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by matthustahli on 28/09/16.
 */
public class LiveMeasure extends AppCompatActivity implements Parcelable {
    private int frequency;
    private int rawData;
    private int rms;
    private int peak;




    //this is my connection to get the data..

    //construct
    public LiveMeasure(int frequency, int rawData, int rms, int peak){
        this.frequency = frequency;
        this.rawData = rawData;
        this.rms = rms;
        this.peak = peak;

    }


    protected LiveMeasure(Parcel in) {
        frequency = in.readInt();
        rawData = in.readInt();
        rms = in.readInt();
        peak = in.readInt();
    }


    public static final Creator<LiveMeasure> CREATOR = new Creator<LiveMeasure>() {
        @Override
        public LiveMeasure createFromParcel(Parcel in) {
            return new LiveMeasure(in);
        }

        @Override
        public LiveMeasure[] newArray(int size) {
            return new LiveMeasure[size];
        }
    };



    //access to this class
    public int getFrequency(){
        return frequency;
    }
    public int getRMS(){
        return rms;
    }
    public int getPeak(){
        return peak;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(frequency);
        dest.writeInt(rawData);
        dest.writeInt(rms);
        dest.writeInt(peak);
    }


}
