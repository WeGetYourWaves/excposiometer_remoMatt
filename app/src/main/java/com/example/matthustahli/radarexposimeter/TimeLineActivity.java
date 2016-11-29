package com.example.matthustahli.radarexposimeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.log;

public class TimeLineActivity extends AppCompatActivity implements View.OnClickListener {

    Integer counterB=0, anzahlBalken=30, activeBar;
    Button b_modeNormal,b_mode21dB, b_mode42dB,b_mode_accumulation,b_switchMode;
    ImageButton b_settings;
    TextView tv_status;
    LinearLayout settings;
    String myMode;
    Animation animationSlideDown;
    double[] qickFixArray;
    Integer peak[]= {302, 400, 6000, 100, 191, 305, 256, 385, 119, 403, 304, 252, 152, 243, 254, 276, 131, 312, 116, 337, 457, 251, 330, 314, 201, 107, 235, 280, 470, 460, 394, 418, 378, 437, 260, 130, 449, 446, 277, 182, 240, 147, 316, 184, 350, 466, 441, 328, 411, 166, 127, 471, 248, 112, 226, 426, 319, 358, 149, 115, 408, 172, 436, 476, 361, 266, 366, 202, 375, 151, 171, 207, 106, 103, 224, 110, 410, 258, 297, 307, 209, 211, 262, 292, 370, 405, 417, 170, 220, 444, 176, 331, 190, 406, 430, 416, 494, 387, 348, 431, 246, 117, 145, 393, 129, 100, 447, 490, 404, 175, 395, 125, 478, 198, 159, 354, 452, 360, 162, 114, 433, 272, 222, 264, 458, 349, 329, 270, 438, 309, 100};
    Integer rms[]= {4000, 1, 200, 3000, 400, 200, 100, 10, 371, 217, 126, 201, 118, 121, 199, 316, 310, 115, 361, 213, 196, 173, 114, 152, 480, 300, 285, 146, 194, 278, 353, 102, 179, 296, 182, 192, 272, 347, 407, 161, 448, 207, 256, 240, 253, 472, 153, 424, 323, 266, 185, 344, 484, 423, 134, 349, 209, 321, 269, 198, 302, 414, 254, 120, 224, 379, 488, 168, 382, 497, 359, 381, 243, 128, 410, 125, 291, 212, 276, 445, 474, 260, 362, 181, 372, 341, 401, 438, 406, 340, 113, 117, 363, 210, 178, 354, 314, 318, 384, 108, 400, 338, 233, 251, 208, 467, 479, 328, 288, 148, 216, 297, 265, 337, 249, 145, 174, 206, 277, 230, 171, 373, 186, 351, 376, 188, 315, 279, 331, 232, 100};
    private int attenuator;
    private int device_id;
    final String LOG_TAG = "Timeline";
    TimelineActivityReceiver timelineActivityReceiver = new TimelineActivityReceiver(LOG_TAG);
    Activity_Superclass calibration;


    //variables for timer
    Timer timer;
    Runnable runnable;
    Handler handler;
    int counter=0;
    //int size;

    //variables for plot
    Rectangle coord;
    Display display;
    int colorFix, colorBar, colorActive, colorLimit ;
    double abstandZwischenBalken =5.0; //5dp
    Paint paintFix, paintBar, paintActive, paintLimit;
    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Point size;
    double scaleY = 0.8;
    double scaleX = 0.9;
    float lastValue =0,maxHight;

    //valiables for data exchange
    private char measurement_type = 'P';
    int freq;//frequencies are in MHz  //beinhaltet die zu betrachtenden frequenzen    //make switch funktion that deletes element at certain place and reorders them
    double rms1;
    double peak1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_line);
        getSettingsFromIntent();

        initalizeButtonsAndIcons();
        activateOnclickListener();
        ActivateTouchOnPlot();
        SetUpValuesForPlot();
        makePlot();
    }

    public void onStart() {
        Log.d(LOG_TAG , "onStart called");
        super.onStart();
    }

    public void onStop(){
        Log.d(LOG_TAG , "onStop called");
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
        freq = intent.getIntExtra("frequency",0);
        freq = 500 + freq*100;        //freq = value of freq MHz;
    }


    private void makePlot() {
        if (timer != null){
            timer.cancel();
        }
        counter =0;
        maxHight= modeMaxSize();
        canvas.drawColor(Color.WHITE);
        imageView.setImageBitmap(bitmap);
        lastValue= CalcBarHight();
        canvas.drawRect(coord.getLeft(counter), lastValue, coord.getRight(counter), coord.getBottom(counter), paintActive);
        counter++;
        handler = new Handler();
        timer = new Timer();
        runnable = new Runnable(){
            public void run() {
                int next = counter %anzahlBalken;
                int last =(counter-1)% anzahlBalken;
                canvas.drawRect(coord.getLeft(last), lastValue, coord.getRight(last), coord.getBottom(last), paintBar);
                lastValue= CalcBarHight();// will be used, and then waits 500 milsec until recalculated for the next bar.
                canvas.drawRect(coord.getLeft(next), lastValue, coord.getRight(next), coord.getBottom(next), paintActive);
                imageView.setImageBitmap(bitmap);
                counter++;
            }
        };
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(runnable);
            }
        },0,500);  // time when new bar appears.
    }

    public float modeMaxSize(){
        int maxSizeInVolt=0;
        switch (myMode){
            case "normal mode":
                maxSizeInVolt=50;
                break;
            case "-21 dB":
                maxSizeInVolt=500;
                break;
            case "-42 dB":
                maxSizeInVolt=5000;
                break;
            case "LNA on":
                maxSizeInVolt=5;
                break;
        }
        return (float) log(maxSizeInVolt);
    }


    private float CalcBarHight() {
       double value;
       if (measurement_type == 'R') {
           value = readRMS();
       }else {
           value = readPeak();
       }
       if(value<=1) {
           return (float) (size.y); // empty size
       }
       if(value >= maxHight) {
           return (float) (size.y - size.y * scaleY); //full size
       }else{
           return (float) (size.y - log(value)/maxHight*size.y*scaleY);
       }
    }


    public void SetUpValuesForPlot() {
        qickFixArray = new double[anzahlBalken];
        Arrays.fill(qickFixArray,0);
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        imageView = (ImageView) findViewById(R.id.imageView_timeline_bitmap);
        bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        paintFix = new Paint();
        paintBar = new Paint();
        paintActive= new Paint();
        paintLimit= new Paint();
        colorFix = TimeLineActivity.this.getResources().getColor(R.color.fixedBar);
        colorBar = TimeLineActivity.this.getResources().getColor(R.color.normalBar);
        colorActive = TimeLineActivity.this.getResources().getColor(R.color.activeBar);
        colorLimit = TimeLineActivity.this.getResources().getColor(R.color.limitBar);
        paintFix.setColor(colorFix);
        paintLimit.setColor(colorLimit);
        paintFix.setStyle(Paint.Style.FILL);
        paintBar.setColor(colorBar);
        paintBar.setStyle(Paint.Style.FILL);
        paintActive.setColor(colorActive);
        paintActive.setStyle(Paint.Style.FILL);
        canvas = new Canvas(bitmap);
        double[] rms = {100.0}; //quickfix
        coord = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, qickFixArray, myMode, scaleX, scaleY);
    }

    private void initalizeButtonsAndIcons() {
        b_modeNormal = (Button) findViewById(R.id.b_mode_normal);
        b_mode21dB = (Button) findViewById(R.id.b_mode_21db);
        b_mode42dB = (Button) findViewById(R.id.b_mode_42db);
        b_mode_accumulation = (Button) findViewById(R.id.b_mode_LNA);
        b_settings = (ImageButton) findViewById(R.id.setting_button);
        b_switchMode = (Button) findViewById(R.id.switch_to_peak);
        tv_status = (TextView) findViewById(R.id.tv_status);
        settings = (LinearLayout) findViewById(R.id.layout_setting);
    }

    private void activateOnclickListener() {
        b_modeNormal.setOnClickListener(this);
        b_mode21dB.setOnClickListener(this);
        b_mode42dB.setOnClickListener(this);
        b_mode_accumulation.setOnClickListener(this);
        b_settings.setOnClickListener(this);
        b_switchMode.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.b_mode_normal:
                myMode = "normal mode";
                makePlot();
                break;
            case R.id.b_mode_21db:
                myMode = "-21 dB";
                makePlot();
                break;
            case R.id.b_mode_42db:
                myMode = "-42 dB";
                makePlot();
                break;
            case R.id.b_mode_LNA:
                myMode = "LNA on";
                makePlot();
                break;
            case R.id.setting_button:
                if (settings.getVisibility() == LinearLayout.VISIBLE) {
                    settings.setVisibility(LinearLayout.GONE);
                } else {
                    settings.setVisibility(LinearLayout.VISIBLE);
                }
                break;
            case R.id.switch_to_peak:
                if(measurement_type == 'R'){
                    measurement_type = 'P';
                    tv_status.setText("Peak");
                    b_switchMode.setText("RMS");
                    makePlot();
                }else{
                    measurement_type = 'R';
                    tv_status.setText("RMS");
                    b_switchMode.setText("Peak");
                    makePlot();
                }
                break;
        }
    }

    public void ActivateTouchOnPlot() {

        View touchView = findViewById(R.id.activity_time_line);
        touchView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN: {
                        try {
                            if (settings.getVisibility() != LinearLayout.GONE) {
                                settings.setVisibility(LinearLayout.GONE);
                            }
                        } catch (NullPointerException e) {}
                        //int position = returnPosition((int) event.getX());
                        //changeBarColorToActiv(position);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        //int position = returnPosition((int) event.getX());
                        //changeBarColorToActiv(position);
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
        int fromLeftToRight = (int) coord.getLeft(i);
        while (fromLeftToRight < x) {
            if (i == anzahlBalken) {        //boundary condition on right edge
                return i;
            }
            i++;
            fromLeftToRight = (int) coord.getLeft(i);
        }
        if (i <= 0) {       //boundary condition on left edge
            return 0;
        }
        return i - 1;
    }

    //change color of only one bar and sets up textview
    public void changeBarColorToActiv(int position) {
        //desactivate last visited bar
        changeBarColorToNOTactiv(activeBar);
        activeBar = position;       //with this position we can also use the add button to put it in a list!
        //update textview
        //set color to active
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paintActive);
        imageView.setImageBitmap(bitmap);
    }

    //changes Bar back to gray color
    private void changeBarColorToNOTactiv(Integer position) {
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paintBar);
        imageView.setImageBitmap(bitmap);
    }

    //handles the allert bar, for example when connection is lost.
    private void opensLostConnection( Integer downOrUp) {
        //sets listener, and handles drop down and drop up
        animationSlideDown = AnimationUtils.loadAnimation(this, R.anim.anim_drop_down);
        final LinearLayout layout_dropDown = (LinearLayout) findViewById(R.id.layout_dropDown);
        TextView allert_text = (TextView) findViewById(R.id.textView_dropDownAllert);
        if(downOrUp==0){
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
        peak1 = newPeak;
    }

    private synchronized void updateRMS(double newRMS){
        rms1 = newRMS;
    }

    public synchronized double readPeak(){
        return peak1;
    }

    public synchronized double readRMS(){
        return rms1;
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
                        updatePeak(peak);
                        updateRMS(rms);
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

}

