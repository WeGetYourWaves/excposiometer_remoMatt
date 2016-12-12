package com.example.matthustahli.radarexposimeter;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

import static java.lang.Math.log;
import static java.lang.Math.min;

/**
 * Created by matthustahli on 06/10/16.
 */


public class Rectangle extends AppCompatActivity {

    private int anzahlBalken;
    private double abgedekteLange,sizeX,sizeY,breiteBalken,abstandZwischenBalken,maxPlot,minPlot, scaleY, scaleX, deltaMaxToMin  ;
    private ArrayList<Float> left;
    private ArrayList<Float> right;
    private ArrayList<Float> top;
    private ArrayList<Float> bottom;
    public double[] values;

    //initialize
    public Rectangle( int anzahlBalkenIn, double abstandZwischenBalkenIn, int sizexIn, int sizeyIn, double[] valuesIn,double scaleXIn, double scaleYIn,double inMaxPlot, double inMinPlot) {
        super();
        scaleX=scaleXIn;
        scaleY=scaleYIn;
        maxPlot = inMaxPlot;
        minPlot = inMinPlot;
        deltaMaxToMin = log(maxPlot)-log(minPlot);
        sizeX = sizexIn*scaleX;
        sizeY = sizeyIn;
        abstandZwischenBalken=abstandZwischenBalkenIn;
        anzahlBalken = anzahlBalkenIn;
        values = valuesIn;

        breiteBalken = (sizeX-200) / anzahlBalken + ((1 - anzahlBalken) * abstandZwischenBalken) / anzahlBalken;
        abgedekteLange = breiteBalken + abstandZwischenBalken;
        left = lengthFromLeft();
        right = lengthFromRight();
        top = lengthFromTop();
        bottom = lengthFromBottom();
        //Log.i("size_x: ", String.valueOf(sisex));
        //Log.i("size_y: ", String.valueOf(sisey));
    }


    public ArrayList<Float> lengthFromLeft() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i < anzahlBalken; i++) {
            toReturn.add(i, (float) (abgedekteLange * i + i * 0.5));        //the 0.5 is for numerical reasons.. as somehow the distances change over groth of x.. dont know how to fix it.
        }
        Log.i("from left_first: ", String.valueOf(toReturn.get(0)));


        return toReturn;
    }

    public ArrayList<Float> lengthFromRight() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i < anzahlBalken; i++) {
            toReturn.add(i, (float) (breiteBalken + i * abgedekteLange + i * 0.5));
        }
        //Log.i("from right: ", String.valueOf(toReturn.get(anzahlBalken)));

        return toReturn;
    }

    //represents the hight
    public ArrayList<Float> lengthFromTop() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i < anzahlBalken; i++) {
            if (values[i] < -1) {
                if (values[i] < -2.5) {
                    toReturn.add(i, (float) (sizeY)); // empty size
                } else {
                    toReturn.add(i, (float) (sizeY - sizeY*scaleY)); //full size
                }
            } else {
                double var = (log(values[i]) - log(minPlot)) / deltaMaxToMin;
                toReturn.add(i, (float) (sizeY - var * sizeY * scaleY));
            }
        }
        return toReturn;
    }

    //always start on bottom.
    public ArrayList<Float> lengthFromBottom() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i < anzahlBalken; i++) {
            toReturn.add(i, (float)sizeY);
        }
        Log.i("Value of Bottom: ", String.valueOf(sizeY));

        return toReturn;
    }


    public float getLeft(int position) {
        return left.get(position);
    }

    public float getRight(int position) {
        return right.get(position);
    }

    public float getTop(int position) {
        return top.get(position);
    }

    public float getBottom(int position) {
        return bottom.get(position);
    }


}
