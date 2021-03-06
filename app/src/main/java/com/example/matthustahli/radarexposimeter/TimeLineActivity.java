package com.example.matthustahli.radarexposimeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Timer;

import static java.lang.Math.incrementExact;
import static java.lang.Math.log;
import static java.lang.Math.round;

public class TimeLineActivity extends AppCompatActivity implements View.OnClickListener {

    Integer anzahlBalken = 40;
    Button b_modeNormal, b_mode21dB, b_mode42dB, b_mode_accumulation, b_startStop;
    ImageButton b_settings;
    LinearLayout settings;
    String myMode;
    Animation animationSlideDown;
    double[] qickFixArray;
    private int attenuator;
    private int device_id;
    final String LOG_TAG = "Timeline";
    TimelineActivityReceiver timelineActivityReceiver = new TimelineActivityReceiver(LOG_TAG);
    Activity_Superclass calibration;
    boolean makePlotRunning = true;


    //variables for timer
    Timer timer;
    Runnable runnable;
    Handler handler;
    int counter = 0;
    //int size;

    //variables for plot
    Rectangle LargePeakBars, SmallRMSBars;
    Display display;
    int colorFix, colorBar, colorActive, colorLimit, colorEmpty, colorButtonActiveMode,colorButtonInactive,colorStartStop,activeBar = 0;
    double abstandZwischenBalken = 5.0; //5dp
    Paint paintFix, paintBar, paintActive, paintLimit, paintEmpty,paintStartStop;
    TextView TVMaxValue, TVMinValue, TVMiddleValue, tv_peakValue, tv_rmsValue, tv_selectedFreq;
    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Point size;
    double scaleY = 0.9;
    double scaleX = 0.9;
    double minPlot, maxPlot,hightPlotScaled;
    float lastValuePeak, lastValueRms, maxHight;
    float[] AllPlotValuesPeak, AllPlotValuesRms;
    double[] rmsValues, peakValues;

    //valiables for data exchange
    private final static char measurement_type = 'A';
    int freq;//frequencies are in MHz  //beinhaltet die zu betrachtenden frequenzen    //make switch funktion that deletes element at certain place and reorders them
    double rms;
    double peak;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getSettingsFromIntent();

        initalizeButtonsAndIcons();
        activateOnclickListener();
        ActivateTouchOnPlot();
        SetUpValuesForPlot();
    }

    public void onStart() {
        Log.d(LOG_TAG, "onStart called");
        super.onStart();
    }

    public void onStop() {
        Log.d(LOG_TAG, "onStop called");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "in onPause");
        super.onPause();
        unregisterReceiver(timelineActivityReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "in onResume");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommunicationService.TRIGGER_Serv2Act);
        registerReceiver(timelineActivityReceiver, intentFilter);
        StartService();
        RequestCALD();
    }

    public void getSettingsFromIntent() {
        Intent intent = getIntent();
        myMode = intent.getStringExtra("myMode");
        if (myMode.equals("-21 dB")) attenuator = 1;
        else if (myMode.equals("LNA on")) attenuator = 3;
        else if (myMode.equals("normal mode")) attenuator = 0;
        else attenuator = 2;
        freq = intent.getIntExtra("frequency", 0);
        //freq = 500 + freq*100;        //freq = value of freq MHz;
        Log.d("Timeline freq", String.valueOf(freq));
        modeMaxSize();
    }

    public void modeMaxSize() {
        int maxSizeInVolt = 0;
        switch (myMode) {
            case "normal mode":
                maxSizeInVolt = 50;
                break;
            case "-21 dB":
                maxSizeInVolt = 500;
                break;
            case "-42 dB":
                maxSizeInVolt = 5000;
                break;
            case "LNA on":
                maxSizeInVolt = 5;
                break;
        }
        maxHight = (float) log(maxSizeInVolt);
    }

    public void SetUpValuesForPlot() {
        qickFixArray = new double[anzahlBalken];
        Arrays.fill(qickFixArray, 0);
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        imageView = (ImageView) findViewById(R.id.imageView_timeline_bitmap);
        bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        AllPlotValuesPeak = new float[anzahlBalken];
        AllPlotValuesRms = new float[anzahlBalken];
        rmsValues =new double[anzahlBalken];
        peakValues= new double[anzahlBalken];
        Arrays.fill(AllPlotValuesPeak, size.y);
        Arrays.fill(AllPlotValuesRms, size.y);
        Arrays.fill(rmsValues,0);
        Arrays.fill(peakValues,0);
        paintFix = new Paint();
        paintBar = new Paint();
        paintActive = new Paint();
        paintLimit = new Paint();
        paintEmpty = new Paint();
        paintStartStop = new Paint();
        colorFix = TimeLineActivity.this.getResources().getColor(R.color.fixedBar);
        colorBar = TimeLineActivity.this.getResources().getColor(R.color.normalBar);
        colorActive = TimeLineActivity.this.getResources().getColor(R.color.activeBar);
        colorLimit = TimeLineActivity.this.getResources().getColor(R.color.limitBar);
        colorButtonActiveMode = TimeLineActivity.this.getResources().getColor(R.color.active_mode_button_color);
        colorButtonInactive = TimeLineActivity.this.getResources().getColor(R.color.inactive_mode_button_color);
        colorEmpty = TimeLineActivity.this.getResources().getColor(R.color.background);
        colorStartStop = TimeLineActivity.this.getResources().getColor(R.color.status_plot);
        paintStartStop.setColor(colorStartStop);
        paintStartStop.setStyle(Paint.Style.FILL);
        paintEmpty.setColor(colorEmpty);
        paintEmpty.setStyle(Paint.Style.FILL);
        paintFix.setColor(colorFix);
        paintLimit.setColor(colorLimit);
        paintFix.setStyle(Paint.Style.FILL);
        paintBar.setColor(colorBar);
        paintBar.setStyle(Paint.Style.FILL);
        paintActive.setColor(colorActive);
        paintActive.setStyle(Paint.Style.FILL);
        canvas = new Canvas(bitmap);
        LargePeakBars = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, qickFixArray, scaleX, scaleY,maxPlot,minPlot);
        SmallRMSBars = new Rectangle(anzahlBalken, abstandZwischenBalken , size.x, size.y, qickFixArray, scaleX, scaleY,maxPlot,minPlot);
        TVMaxValue = (TextView) findViewById(R.id.tv_maxValueScale);
        TVMiddleValue = (TextView) findViewById(R.id.tv_middleValueScale);
        TVMinValue = (TextView) findViewById(R.id.tv_minValueScale);
        SetPositionOnScreenOfTextViews();
        tv_selectedFreq.setText(String.valueOf(freq/1000.0)+" GHz");
        //tv_peakValue.setPaintFlags(paintActive.getColor());
        //tv_peakValue.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
    }

    private void SetPositionOnScreenOfTextViews() {
        ViewGroup.MarginLayoutParams top = (ViewGroup.MarginLayoutParams) TVMaxValue.getLayoutParams();
        top.topMargin = (int) (size.y * (1.0 - scaleY)+33);
        ViewGroup.MarginLayoutParams middle = (ViewGroup.MarginLayoutParams) TVMiddleValue.getLayoutParams();
        middle.topMargin = (int) (size.y / 2.0 + size.y * (1.0 - scaleY) / 2.0);
        ViewGroup.MarginLayoutParams bottom = (ViewGroup.MarginLayoutParams) TVMinValue.getLayoutParams();
        bottom.topMargin = (int) (size.y - size.y * (1.0 - scaleY));

    }

    synchronized private void makePlot() {
        if (makePlotRunning == true) {
            int next = counter % anzahlBalken;
            int updater = (counter+1)%anzahlBalken;
            if(next== activeBar){ tv_rmsValue.setText(""); tv_peakValue.setText("");}
            peakValues[next] = readPeak();  //need that to be able to show values in textviews
            rmsValues[next] = readRMS();
            lastValuePeak = (float) peakValues[next];
            lastValueRms = (float) rmsValues[next];
            Log.d("Timeline peak", String.valueOf(lastValuePeak));
            Log.d("Timeline rms", String.valueOf(lastValueRms));
            //delet bar first
            canvas.drawRect(LargePeakBars.getLeft(updater),(float) (size.y*(1-scaleY)), LargePeakBars.getRight(updater), LargePeakBars.getBottom(updater), paintStartStop); //paintEmpty also possible
            canvas.drawRect(LargePeakBars.getLeft(next), 0, LargePeakBars.getRight(next), LargePeakBars.getBottom(next), paintEmpty);
            //canvas.drawRect(0,(float) (size.y*(1-scaleY)),size.x,(float) (size.y*(1-scaleY*0.99)),paintLimit);   //draws limit bar horizontal
            //draw the bar
            if (lastValuePeak < -1) {
                if (lastValuePeak < -2.5) {
                    lastValuePeak = size.y;
                } else {
                    lastValuePeak = (float) (size.y - size.y * scaleY); //full size
                    //canvas.drawRect(LargePeakBars.getLeft(next), lastValuePeak, LargePeakBars.getRight(next), LargePeakBars.getBottom(next), paintLimit);
                    canvas.drawRect(LargePeakBars.getLeft(next), lastValuePeak, LargePeakBars.getRight(next), LargePeakBars.getBottom(next), paintBar);
                }
            } else {
                lastValuePeak = (float) (size.y- ((log(lastValuePeak)-log(minPlot)) / hightPlotScaled)*size.y*scaleY);
                canvas.drawRect(LargePeakBars.getLeft(next), lastValuePeak, LargePeakBars.getRight(next), LargePeakBars.getBottom(next), paintBar);
            }
            if (lastValueRms < -1) {
                if (lastValueRms < -2.5) {
                    lastValueRms = size.y;
                } else {
                    lastValueRms = (float) (size.y - size.y * scaleY); //full size
                    //canvas.drawRect(SmallRMSBars.getLeft(next), lastValueRms, SmallRMSBars.getRight(next), SmallRMSBars.getBottom(next), paintLimit);
                    canvas.drawRect(SmallRMSBars.getLeft(next), lastValueRms, SmallRMSBars.getRight(next), SmallRMSBars.getBottom(next), paintActive);
                }
            } else {
                lastValueRms = (float) (size.y-((log(lastValueRms)-log(minPlot)) / hightPlotScaled)*size.y*scaleY);
                canvas.drawRect(SmallRMSBars.getLeft(next), lastValueRms, SmallRMSBars.getRight(next), SmallRMSBars.getBottom(next), paintActive);
            }

            AllPlotValuesPeak[next] = lastValuePeak;        //need that to be able to make the touch on bars possible, as i need to draw them back..
            AllPlotValuesRms[next] = lastValueRms;
            imageView.setImageBitmap(bitmap);
            counter++;
        } else {
        }
    }

    synchronized private void resetPlotToBeginning() {
        Arrays.fill(AllPlotValuesPeak, size.y);
        Arrays.fill(AllPlotValuesRms, size.y);
        Arrays.fill(rmsValues,0);
        Arrays.fill(peakValues,0);
        canvas.drawColor(Color.WHITE);
        imageView.setImageBitmap(bitmap);
        counter = 0;
    }

    private void initalizeButtonsAndIcons() {
        b_modeNormal = (Button) findViewById(R.id.b_mode_normal);
        b_mode21dB = (Button) findViewById(R.id.b_mode_21db);
        b_mode42dB = (Button) findViewById(R.id.b_mode_42db);
        b_mode_accumulation = (Button) findViewById(R.id.b_mode_LNA);
        b_settings = (ImageButton) findViewById(R.id.setting_button);
        b_startStop = (Button) findViewById(R.id.startStopButton);
        tv_peakValue = (TextView) findViewById(R.id.tv_peakValue);
        tv_rmsValue = (TextView) findViewById(R.id.tv_rmsValue);
        tv_selectedFreq = (TextView) findViewById(R.id.tv_selectedFreq_inTimeLine);
        settings = (LinearLayout) findViewById(R.id.layout_setting);
    }

    private void activateOnclickListener() {
        b_modeNormal.setOnClickListener(this);
        b_mode21dB.setOnClickListener(this);
        b_mode42dB.setOnClickListener(this);
        b_mode_accumulation.setOnClickListener(this);
        b_settings.setOnClickListener(this);
        b_startStop.setOnClickListener(this);
    }

    private void changeColorOfSelectedMode(){
        b_modeNormal.setVisibility(View.GONE);
        b_mode21dB.setVisibility(View.GONE);
        b_mode42dB.setVisibility(View.GONE);
        b_mode_accumulation.setVisibility(View.GONE);
        /*b_modeNormal.setBackgroundColor(colorButtonInactive);
        b_mode21dB.setBackgroundColor(colorButtonInactive);
        b_mode42dB.setBackgroundColor(colorButtonInactive);
        b_mode_accumulation.setBackgroundColor(colorButtonInactive);*/
        switch (myMode){
            case "normal mode":
                b_modeNormal.setVisibility(View.VISIBLE);
                b_modeNormal.setBackgroundColor(colorButtonActiveMode);
                break;
            case "-21 dB":
                b_mode21dB.setVisibility(View.VISIBLE);
                b_mode21dB.setBackgroundColor(colorButtonActiveMode);
                break;
            case "-42 dB":
                b_mode42dB.setVisibility(View.VISIBLE);
                b_mode42dB.setBackgroundColor(colorButtonActiveMode);
                break;
            case "LNA on":
                b_mode_accumulation.setVisibility(View.VISIBLE);
                b_mode_accumulation.setBackgroundColor(colorButtonActiveMode);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        Timeline_Packet_Trigger timeStop = new Timeline_Packet_Trigger(device_id, attenuator, freq, (char) 0);
        switch (v.getId()) {
            case R.id.b_mode_normal:
                settings.setVisibility(View.GONE);
                break;
            case R.id.b_mode_21db:
                settings.setVisibility(LinearLayout.GONE);
                break;
            case R.id.b_mode_42db:
                settings.setVisibility(LinearLayout.GONE);
                break;
            case R.id.b_mode_LNA:
                settings.setVisibility(LinearLayout.GONE);
                break;
            /*case R.id.b_mode_normal:
                myMode = "normal mode";
                attenuator = 0;
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                sendTrigger(timeStop.get_packet());
                Timeline_Packet_Trigger timeline_packet_trigger0 = new Timeline_Packet_Trigger(device_id, attenuator, freq, measurement_type);
                sendTrigger(timeline_packet_trigger0.get_packet());
                settings.setVisibility(LinearLayout.GONE);
                modeMaxSize();
                resetPlotToBeginning();
                break;
            case R.id.b_mode_21db:
                myMode = "-21 dB";
                attenuator = 1;
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                sendTrigger(timeStop.get_packet());
                Timeline_Packet_Trigger timeline_packet_trigger1 = new Timeline_Packet_Trigger(device_id, attenuator, freq, measurement_type);
                sendTrigger(timeline_packet_trigger1.get_packet());
                settings.setVisibility(LinearLayout.GONE);
                modeMaxSize();
                resetPlotToBeginning();
                break;
            case R.id.b_mode_42db:
                myMode = "-42 dB";
                attenuator = 2;
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                sendTrigger(timeStop.get_packet());
                Timeline_Packet_Trigger timeline_packet_trigger2 = new Timeline_Packet_Trigger(device_id, attenuator, freq, measurement_type);
                sendTrigger(timeline_packet_trigger2.get_packet());
                settings.setVisibility(LinearLayout.GONE);
                modeMaxSize();
                resetPlotToBeginning();
                break;
            case R.id.b_mode_LNA:
                myMode = "LNA on";
                attenuator = 3;
                change_MinMaxPlot();
                makePlotRunning = true;
                b_startStop.setText("Stop");
                sendTrigger(timeStop.get_packet());
                Timeline_Packet_Trigger timeline_packet_trigger3 = new Timeline_Packet_Trigger(device_id, attenuator, freq, measurement_type);
                sendTrigger(timeline_packet_trigger3.get_packet());
                settings.setVisibility(LinearLayout.GONE);
                modeMaxSize();
                resetPlotToBeginning();
                break;*/
            case R.id.setting_button:
                if (settings.getVisibility() == LinearLayout.VISIBLE) {
                    settings.setVisibility(LinearLayout.GONE);
                } else {
                    settings.setVisibility(LinearLayout.VISIBLE);
                    changeColorOfSelectedMode();
                }
                break;
            case R.id.startStopButton:
                if (makePlotRunning == true) {
                    sendTrigger(timeStop.get_packet());
                    makePlotRunning = false;
                    b_startStop.setText("Start");
                } else {
                    makePlotRunning = true;
                    Timeline_Packet_Trigger timeline_packet_triggerRun = new Timeline_Packet_Trigger(device_id, attenuator, freq, measurement_type);
                    sendTrigger(timeline_packet_triggerRun.get_packet());
                    b_startStop.setText("Stop");
                }
                break;
        }
    }

    //this is for closing settings and registrating touch on plot
    public void ActivateTouchOnPlot() {
        View touchView = findViewById(R.id.imageView_timeline_bitmap);
        touchView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        try {
                            if (settings.getVisibility() != LinearLayout.GONE) {
                                settings.setVisibility(LinearLayout.GONE);
                            }
                        } catch (NullPointerException e) {
                        }
                        int position = returnPosition((int) event.getX());
                        changeBarColorToActiv(position);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        int position = returnPosition((int) event.getX());
                        changeBarColorToActiv(position);
                        break;
                    }
                }
                return true; //true= we handled the event!!
            }
        });
    }

    //gets position from coordinates
    public int returnPosition(int x) {
        int i = 0;
        int fromLeftToRight = (int) LargePeakBars.getLeft(i);
        while (fromLeftToRight < x) {
            if (i == anzahlBalken - 1) {        //boundary condition on right edge
                return i;
            }
            i++;
            fromLeftToRight = (int) LargePeakBars.getLeft(i);
        }
        return i;
    }

    public void changeBarColorToActiv(int position) {
        //desactivate last visited bar
        changeBarColorToNOTactiv(activeBar);
        activeBar = position;       //with this position we can also use the add button to put it in a list!
        updateTextViewsShowingValues(position); //update textview
        //set color to active
        canvas.drawRect(LargePeakBars.getLeft(position), AllPlotValuesPeak[position], LargePeakBars.getRight(position), LargePeakBars.getBottom(position), paintFix);
        imageView.setImageBitmap(bitmap);
    }

    //changes Bar back to normal plot color
    private void changeBarColorToNOTactiv(Integer position) {
        if(false){
        //if(peakValues[position]== -2){
            canvas.drawRect(LargePeakBars.getLeft(position), AllPlotValuesPeak[position], LargePeakBars.getRight(position), LargePeakBars.getBottom(position), paintLimit);
        }else {

            canvas.drawRect(LargePeakBars.getLeft(position), AllPlotValuesPeak[position], LargePeakBars.getRight(position), LargePeakBars.getBottom(position), paintBar);
            canvas.drawRect(SmallRMSBars.getLeft(position), AllPlotValuesRms[position], SmallRMSBars.getRight(position), SmallRMSBars.getBottom(position), paintActive);
        }
        imageView.setImageBitmap(bitmap);
    }

    private void updateTextViewsShowingValues(int position) {
        if (rmsValues[position] < -1) {
            if (rmsValues[position] < -2.5) {
                tv_rmsValue.setText('<' + Double.toString(minPlot));
            } else {
                tv_rmsValue.setText('>' + Double.toString(maxPlot));
            }
        } else {
            double valueToShow = rmsValues[position];
            valueToShow = Math.round(valueToShow * 100);  // runden auf ##.#
            valueToShow = valueToShow / 100;
            tv_rmsValue.setText(String.valueOf((valueToShow))+ " V/m");
        }
        if (peakValues[position] < -1) {
            if (peakValues[position] < -2.5) {
                tv_peakValue.setText('<' + Double.toString(minPlot));
            }else {
                tv_peakValue.setText('>' + Double.toString(maxPlot));
            }
        } else {
            double valueToShow = peakValues[position];
            valueToShow = Math.round(valueToShow * 100);  // runden auf ##.#
            valueToShow = valueToShow / 100;
            tv_peakValue.setText(String.valueOf(valueToShow)+ " V/m");
        }
    }

    //handles the allert bar, for example when connection is lost.
    private void ConnectionLostDropDown( Integer downOrUp, String errormessage) {
        //sets listener, and handles drop down and drop up
        animationSlideDown = AnimationUtils.loadAnimation(this, R.anim.anim_drop_down);
        final LinearLayout layout_dropDown = (LinearLayout) findViewById(R.id.layout_dropDown);
        TextView allert_text = (TextView) findViewById(R.id.textView_dropDownAllert);
        if(downOrUp==0){
            allert_text.setText(errormessage);
            layout_dropDown.setVisibility(View.VISIBLE);
            ScaleAnimation scale = new ScaleAnimation(1,1,0,1);
            scale.setDuration(400);
            allert_text.startAnimation(scale);
        }else{
            ScaleAnimation scale = new ScaleAnimation(1,1,1,0);
            scale.setDuration(400);
            allert_text.startAnimation(scale);
            scale.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    layout_dropDown.setVisibility(View.GONE);}
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }
    }


    private synchronized void updatePeak(double newPeak){
        peak = newPeak;
    }

    private synchronized void updateRMS(double newRMS){
        rms = newRMS;
    }

    public synchronized double readPeak(){
        return peak;
    }

    public synchronized double readRMS(){
        return rms;
    }

    public synchronized int readFreq(){
        return freq;
    }

    protected byte[] split_packet (int start, int end, byte[] packet){

        int length = end - start + 1;
        byte[] splitted = new byte[length];
        for (int i = 0; i < length; i++){
            splitted[i] = packet[i + start];
        }

        return splitted;
    }

    //receiver of the TimeLineActivity which receives the data from the CommunicationService
    public class TimelineActivityReceiver extends BroadcastReceiver {
        final String LOG_TAG;

        public TimelineActivityReceiver(String LOG_TAG){
            this.LOG_TAG = LOG_TAG;
        }
        @Override
        public void onReceive(Context arg0, Intent data) {
            Log.d(LOG_TAG, "MyActivityReceiver in onReceive");
            byte[] orgData = data.getByteArrayExtra(CommunicationService.DATA_BACK);
            if (orgData != null) {
                if(new String(split_packet(4, 7, orgData)).equals("CALD")) {
                    Cal_Packet_Exposi cal_packet_exposi = new Cal_Packet_Exposi(orgData);
                    device_id = cal_packet_exposi.get_device_id();
                    calibration = new Activity_Superclass(orgData);
                    change_MinMaxPlot();
                    Log.d(LOG_TAG, "saved Calibration Tables");

                    Timeline_Packet_Trigger timeline_packet_trigger = new Timeline_Packet_Trigger(device_id, attenuator, freq, measurement_type);
                    sendTrigger(timeline_packet_trigger.get_packet());
                    Log.d(LOG_TAG, "sent TIME Trigger");

                }
                else if(new String(split_packet(4, 7, orgData)).equals("TIME")){

                    Log.d(LOG_TAG, "got TIME data");
                    Timeline_Packet_Exposi packetExposi = new Timeline_Packet_Exposi(orgData);
                    int freq_exp = packetExposi.get_frequency();
                    if (freq_exp == freq){
                        int rms_exposi = packetExposi.get_rawData_rms();
                        int peak_exposi = packetExposi.get_rawData_peak();

                        double rms = calibration.get_rms(attenuator,freq, rms_exposi);
                        double peak = calibration.get_peak(attenuator, freq, peak_exposi);

                        if (peak != -1){
                            updatePeak(peak);
                        }
                        if(rms != -1){
                            updateRMS(rms);

                        }
                        makePlot();
                    }
                }
                else if (new String(split_packet(4, 7, orgData)).equals("EROR")){

                    Log.d(LOG_TAG, "got EROR packet");
                    Error_Packet_Exposi error_packet = new Error_Packet_Exposi(orgData);
                    int errorCode = error_packet.get_errorCode();
                    String errorMessage = error_packet.get_errorMessage();
                    if (errorCode == 1){
                        //connection to ESP lost
                        ConnectionLostDropDown(0, errorMessage);
                    }
                }
            }
        }
    }

    private void StartService() {
        Intent intent = new Intent(this, CommunicationService.class);
        startService(intent);
    }

    private void RequestCALD(){
        Log.d(LOG_TAG, "Requested Calipack");
        Intent intent = new Intent();
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.COMMAND_Act2Serv, CommunicationService.CMD_getCALI);
        sendBroadcast(intent);
    }

    public void sendTrigger(byte[] TriggerPack) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.TRIGGER_Act2Serv, TriggerPack);
        sendBroadcast(intent);
    }

    public void change_MinMaxPlot(){
        maxPlot = calibration.get_maxPlot(attenuator, 'P');
        minPlot = calibration.get_minPlot(attenuator, 'P');
        hightPlotScaled= log(maxPlot)-log(minPlot);
        TVMaxValue.setText(String.valueOf((maxPlot))+" V/m");
        TVMinValue.setText(String.valueOf((minPlot))+" V/m");
        TVMiddleValue.setVisibility(View.GONE);
        TVMinValue.bringToFront();
        TVMaxValue.bringToFront();
    }

    private double roundDouble(double toRound){
        double output = toRound*1000;
        output = round(output);
        output = output/1000.0;
        return output;
    }

}

