package com.example.matthustahli.radarexposimeter;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by matthustahli on 06/10/16.
 */


public class Rectangle extends AppCompatActivity {

    private int anzahlBalken;
    private float frequency;
    private float breiteBalken;
    private float abgedekteLange;
    private ArrayList<Float> left;
    private ArrayList<Float> right;
    private ArrayList<Float> top;
    private ArrayList<Float> bottom;
    private float hans;
    private int sizex;
    public int[] height;
    String active;

    //initialize
    public Rectangle(float balkenBreite, int balkenAnzahl, float abstandZwischenBalken, int sisey, int sisex) {
        super();
        hans = (float) sisey;
        sizex = sisex;
        breiteBalken = balkenBreite;
        //frequency = height;
        anzahlBalken = balkenAnzahl;
        abgedekteLange = balkenBreite + abstandZwischenBalken;
        OverviewScanPlotActivity helpMe = new OverviewScanPlotActivity();
        height= helpMe.peak;
        left = lengthFromLeft();
        right = lengthFromRight();
        top = lengthFromTop();
        bottom = lengthFromBottom();
        Log.i("size_x: ", String.valueOf(sisex));
        Log.i("size_y: ", String.valueOf(sisey));
       // active = mode;


    }

    //accessable funktions
    public ArrayList<Float> lengthFromLeft() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i <= anzahlBalken; i++) {
            toReturn.add(i, (float) (abgedekteLange * i + i * 0.5));        //the 0.5 is for numerical reasons.. as somehow the distances change over groth of x.. dont know how to fix it.
        }
        Log.i("from left_first: ", String.valueOf(toReturn.get(0)));
        Log.i("from left_last: ", String.valueOf(toReturn.get(anzahlBalken)));


        return toReturn;
    }

    public ArrayList<Float> lengthFromRight() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i <= anzahlBalken; i++) {
            toReturn.add(i, (float) (breiteBalken + i * abgedekteLange + i * 0.5));
        }
        Log.i("from right: ", String.valueOf(toReturn.get(anzahlBalken)));

        return toReturn;
    }


    //represents the hight
    public ArrayList<Float> lengthFromTop() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i <= anzahlBalken; i++) {
            toReturn.add(i, (float)200+ height[i]);
        }
        Log.i("top hight: ", String.valueOf(toReturn.get(0)));

        return toReturn;
    }

    //always start on bottom.
    public ArrayList<Float> lengthFromBottom() {
        ArrayList<Float> toReturn = new ArrayList<Float>();
        for (int i = 0; i <= anzahlBalken; i++) {
            toReturn.add(i, hans);
        }
        Log.i("Value of Bottom: ", String.valueOf(hans));

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
