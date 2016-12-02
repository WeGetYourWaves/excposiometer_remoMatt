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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Thread.sleep;

public class OverviewScanPlotActivity extends AppCompatActivity implements View.OnClickListener {

    private final String CHOOSENFREQ = "my_freq";
    private double[] rms = new double [96];
    private double[] peak = new double [96];
    //public double[] peak = {302, 203, 345, 196, 191, 305, 256, 385, 6000, 4003, 304, 252, 152, 2403, 2454, 5276, 1131, 3812, 1186, 3037, 457, 251, 330, 314, 201, 107, 235, 280, 470, 460, 394, 418, 378, 437, 260, 130, 449, 446, 277, 182, 240, 147, 316, 184, 350, 466, 441, 328, 411, 166, 127, 471, 248, 112, 226, 426, 319, 358, 149, 115, 408, 172, 436, 476, 361, 266, 366, 202, 375, 151, 171, 207, 106, 103, 224, 110, 410, 258, 297, 307, 209, 211, 262, 292, 370, 405, 417, 170, 220, 444, 176, 331, 190, 406, 430, 416, 494, 387, 348, 431, 246, 117, 145, 393, 129, 100, 447, 490, 404, 175, 395, 125, 478, 198, 159, 354, 452, 360, 162, 114, 433, 272, 222, 264, 458, 349, 329, 270, 438, 309, 100};
    //public double[] rms = {348, 435, 332, 368, 271, 404, 346, 320, 371, 217, 126, 201, 118, 121, 199, 316, 310, 115, 361, 213, 196, 173, 114, 152, 480, 300, 285, 146, 194, 278, 353, 102, 179, 296, 182, 192, 272, 347, 407, 161, 448, 207, 256, 240, 253, 472, 153, 424, 323, 266, 185, 344, 484, 423, 134, 349, 209, 321, 269, 198, 302, 414, 254, 120, 224, 379, 488, 168, 382, 497, 359, 381, 243, 128, 410, 125, 291, 212, 276, 445, 474, 260, 362, 181, 372, 341, 401, 438, 406, 340, 113, 117, 363, 210, 178, 354, 314, 318, 384, 108, 400, 338, 233, 251, 208, 467, 479, 328, 288, 148, 216, 297, 265, 337, 249, 145, 174, 206, 277, 230, 171, 373, 186, 351, 376, 188, 315, 279, 331, 232, 100};
    private int attenuator;
    private int device_id;
    private char measurement_type = 'P';
    final String LOG_TAG = "Overview";
    OverviewScanPlotActivityReceiver overviewScanPlotActivityReceiver = new OverviewScanPlotActivityReceiver(LOG_TAG);
    Activity_Superclass calibration;



    //----------------------------------------------------------------------
    //setup variables
    Rectangle coord;
    int colorFix, colorBar, colorActive, colorLimit, colorToHigh ;
    Paint paintFix, paintBar, paintActive, paintLimit,paintToHigh;
    Display display;
    Point size;
    ImageView imageView;
    float abstandZwischenBalken = 5; //5dp
    int anzahlBalken = 96;//null zählt auch als balken
    Bitmap bitmap;
    Canvas canvas;
    Integer activeBar = 0;
    TextView selectedFreq, selectedValue;
    ArrayList<Integer> fixedBars = new ArrayList<Integer>();
    LinearLayout settings;
    private String myMode;
    private ArrayList<LiveMeasure> measures = new ArrayList<LiveMeasure>();
    //All what has something to do with buttons
    private Integer clickCounterStatusPlot = 0;
    Animation animationSlideDown;
    Button b_normal, b_21dB, b_41dB, b_accu, b_peak;
    ImageButton b_settings, addButton, clearButton, refreshButton, nextButton;

//----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview_scan_plot);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // this removes the status bar (the one showing time, batterie ect..)
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getModeFromIntent();

        initializeButtons();
        //handlesActivatingDropDown();
        setButtonsOnClickListener();

        SetUpValuesForPlot();  //Makes the plot and draws it.
        makePlot();

        ActivateTouchOnPlot();
        // ActivateAddButton();
        Log.d("activeBar(onCreate): ", String.valueOf(activeBar));
    }

    private void RequestCALD(){
        Log.d(LOG_TAG, "Requested Calipack");
        Intent intent = new Intent();
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.COMMAND_Act2Serv, CommunicationService.CMD_getCALI);
        sendBroadcast(intent);
    }

    private void sendTrigger(byte[] TriggerPack) {
        flushArrays();
        Intent intent = new Intent();
        intent.setAction(CommunicationService.ACTION_FROM_ACTIVITY);
        intent.putExtra(CommunicationService.TRIGGER_Act2Serv, TriggerPack);
        sendBroadcast(intent);
    }

    public void onStart() {
        Log.d(LOG_TAG , "onStart called");
        super.onStart();
    }

    public void onStop(){
        Log.d(LOG_TAG , "onStop called");
        super.onStop();
    }

    protected byte[] split_packet (int start, int end, byte[] packet){

        int length = end - start + 1;
        byte[] splitted = new byte[length];
        for (int i = 0; i < length; i++){
            splitted[i] = packet[i + start];
        }

        return splitted;
    }



//TODO  when element deleted, update length of Value Bar's

//----------------------------------------------------------------------

    private void getModeFromIntent() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        myMode = intent.getStringExtra("MODE");
        if (myMode == "-21 dB")  attenuator = 1;
        else if (myMode == "LNA on")  attenuator = 3;
        else if(myMode == "normal mode")   attenuator = 0;
        else if(myMode == "-42 dB") attenuator = 2;
        Toast.makeText(OverviewScanPlotActivity.this, myMode, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "in onPause");
        super.onPause();
        unregisterReceiver(overviewScanPlotActivityReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "in onResume");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CommunicationService.TRIGGER_Serv2Act);
        registerReceiver(overviewScanPlotActivityReceiver, intentFilter);
        StartService();
        RequestCALD();
    }


    // animation dropdown allert
    //----------------------------------------------------------------


    private void letButtonTurn(Integer turnOrStop){

        if(turnOrStop==0|true){
            RotateAnimation rotate = new RotateAnimation(360,0,refreshButton.getWidth()/2 ,refreshButton.getHeight() / 2);
            rotate.setDuration(500);
            rotate.setRepeatCount(2);
            refreshButton.startAnimation(rotate);
        }
    }

    //downOrUp: 0=godown, 1=goup
    private void handlesActivatingDropDown(Integer downOrUp) {
        //sets listener, and handles drop down and drop up
        animationSlideDown = AnimationUtils.loadAnimation(this, R.anim.anim_drop_down);
        final LinearLayout layout_dropDown = (LinearLayout) findViewById(R.id.layout_dropDown);
        TextView allert_text = (TextView) findViewById(R.id.textView_dropDownAllert);
        if(downOrUp==0){
            selectedFreq.setText("");
            selectedValue.setText("");
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

    //----------------------------------------------------------------
    @Override
    public void onClick(View v) {
        View_Packet_Trigger viewStop = new View_Packet_Trigger(device_id, attenuator, (char) 0);

        switch (v.getId()) {
            case R.id.b_mode_normal:
                attenuator = 0;
                myMode = "normal mode";
                //makePlot();
                settings.setVisibility(LinearLayout.GONE);
                Toast.makeText(OverviewScanPlotActivity.this, myMode, Toast.LENGTH_SHORT).show();
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger0 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger0.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 0");
                break;
            case R.id.b_mode_21db:
                myMode = "-21 dB";
                attenuator = 1;
                //makePlot();
                settings.setVisibility(LinearLayout.GONE);
                Toast.makeText(OverviewScanPlotActivity.this, myMode, Toast.LENGTH_SHORT).show();
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger1 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger1.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 1");
                break;
            case R.id.b_mode_42db:
                myMode = "-42 dB";
                attenuator = 2;
                //makePlot();

                settings.setVisibility(LinearLayout.GONE);
                Toast.makeText(OverviewScanPlotActivity.this, myMode, Toast.LENGTH_SHORT).show();
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger2 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger2.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 2");
                break;

            case R.id.b_mode_LNA:
                myMode = "LNA on";
                attenuator = 3;
                //makePlot();
                settings.setVisibility(LinearLayout.GONE);
                Toast.makeText(OverviewScanPlotActivity.this, myMode, Toast.LENGTH_SHORT).show();
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger3 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger3.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 3");
                break;
            case R.id.switch_to_peak:
                if (measurement_type == 'P')    measurement_type = 'R';
                else measurement_type = 'P';

                //downOrUp: 0=godown, 1=goup
                //handlesActivatingDropDown(clickCounterStatusPlot%2); // to show connection bar
                clickCounterStatusPlot++;

                if (measurement_type == 'P') {
                    sendTrigger(viewStop.get_packet());
                    View_Packet_Trigger view_packet_triggerP = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                    sendTrigger(view_packet_triggerP.get_packet());
                    Log.d(LOG_TAG, "sent SCAN Trigger Peak");
                    b_peak.setText("RMS");
                    TextView statusView = (TextView) findViewById(R.id.status_textview);
                    statusView.setText("PeakPlot");
                }
                if (measurement_type == 'R') {
                    sendTrigger(viewStop.get_packet());
                    View_Packet_Trigger view_packet_triggerR = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                    sendTrigger(view_packet_triggerR.get_packet());
                    Log.d(LOG_TAG, "sent SCAN Trigger RMS");
                    b_peak.setText("Peak");
                    TextView statusView = (TextView) findViewById(R.id.status_textview);
                    statusView.setText("RmsPlot");
                }
                break;
            case R.id.add_freq_button:
                if(checkIfFreqAlreadyAdded()){break;}
                if (fixedBars.size() < 4) {
                    fixedBars.add(activeBar);
                    chandeBarColorToFixed();
                } else {
                    Toast.makeText(OverviewScanPlotActivity.this, "FULL ARRAY", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.clear_button:
                if (fixedBars.size() > 0) {
                    int lastAddedFreq = fixedBars.get(fixedBars.size() - 1);
                    fixedBars.remove(fixedBars.size() - 1);
                    changeBarColorToNOTactiv(lastAddedFreq);}
                changeBarColorToNOTactiv(activeBar);
                selectedFreq.setText("");
                selectedValue.setText("");
                break;
            case R.id.next_button:
                sendTrigger(viewStop.get_packet());
                OpenDetailViewActivity();
                break;
            case R.id.refresh_button:
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_triggerRefresh = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_triggerRefresh.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger Refresh");

                //reloading and making new plot
                letButtonTurn(clickCounterStatusPlot);
                makePlot();
                clickCounterStatusPlot++;
                break;
            case R.id.setting_button:
                if (settings.getVisibility() == LinearLayout.VISIBLE) {
                    settings.setVisibility(LinearLayout.GONE);
                } else {
                    settings.setVisibility(LinearLayout.VISIBLE);
                }

                break;
        }
    }

    private void initializeButtons() {
        //initialize Buttons
        b_normal = (Button) findViewById(R.id.b_mode_normal);
        b_21dB = (Button) findViewById(R.id.b_mode_21db);
        b_41dB = (Button) findViewById(R.id.b_mode_42db);
        b_accu = (Button) findViewById(R.id.b_mode_LNA);
        addButton = (ImageButton) findViewById(R.id.add_freq_button);
        nextButton = (ImageButton) findViewById(R.id.next_button);
        clearButton = (ImageButton) findViewById(R.id.clear_button);
        b_peak = (Button) findViewById(R.id.switch_to_peak);
        refreshButton = (ImageButton) findViewById(R.id.refresh_button);
        b_settings = (ImageButton) findViewById(R.id.setting_button);
        // initialize Text
        selectedFreq = (TextView) findViewById(R.id.selected_freq);
        selectedValue = (TextView) findViewById(R.id.textView_value);
        //initialize Layouts
        settings = (LinearLayout) findViewById(R.id.layout_setting);

    }

    private void setButtonsOnClickListener() {
        b_normal.setOnClickListener(this);
        b_21dB.setOnClickListener(this);
        b_41dB.setOnClickListener(this);
        b_accu.setOnClickListener(this);
        addButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        b_peak.setOnClickListener(this);
        refreshButton.setOnClickListener(this);
        b_settings.setOnClickListener(this);
    }

    private boolean checkIfFreqAlreadyAdded(){
        for(int i=0; i<fixedBars.size();i++) {
            if(activeBar==fixedBars.get(i))
                return true;
        }
        return false;
    }



    //this lets us go to the next activity
    private void OpenDetailViewActivity() {
        Intent intent = new Intent(OverviewScanPlotActivity.this, DetailViewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList(CHOOSENFREQ, fixedBars);
        intent.putExtras(bundle);
        intent.putExtra("MODE", myMode);
        intent.putExtra("type", measurement_type);
        startActivity(intent);
    }

    public void ActivateTouchOnPlot() {
        View touchView = findViewById(R.id.activity_overview_scan_plot);
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
        int fromLeftToRight = (int) coord.getLeft(i);
        while (fromLeftToRight < x) {
            if (i == anzahlBalken-1) {        //boundary condition on right edge
                return i;
            }
            i++;
            fromLeftToRight = (int) coord.getLeft(i);
        }
        return i;
    }

    //change color of only one bar and sets up textview
    public void changeBarColorToActiv(int position) {
        //desactivate last visited bar
        changeBarColorToNOTactiv(activeBar);
        activeBar = position;       //with this position we can also use the add button to put it in a list!
        updateActiveFrequency(position); //update textview
        //set color to active
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paintActive);
        imageView.setImageBitmap(bitmap);
        chandeBarColorToFixed();
    }

    //updates the textviews value of peak and choosen frequency
    private void updateActiveFrequency(int position){
        double freqToShow= 0.5 + 0.1*position;
        freqToShow= Math.round(freqToShow*10);  // runden auf ##.#
        freqToShow = freqToShow/10;
        //find the actiual freq repsresenting the the freq in the array
        selectedFreq.setText(String.valueOf(freqToShow) + " GHz"); //sets selected freq into textVie
        if(measurement_type=='R'){
            if(rms[position]<-1){
                if(rms[position]<-2.5){
                    selectedValue.setText("< min ");
                }else{
                    selectedValue.setText("> max ");
                }
            }else{
            double valueToShow = rms[position] ;
            valueToShow= Math.round(valueToShow*100);  // runden auf ##.#
            valueToShow = valueToShow/100;
            selectedValue.setText(String.valueOf(valueToShow)+ " V/m ");
            }
        }else{
            if(peak[position]<-1){
                if(peak[position]<-2.5){
                    selectedValue.setText("< min ");
                }else{
                    selectedValue.setText("> max ");
                }
            }else {
                double valueToShow = peak[position];
                valueToShow = Math.round(valueToShow * 100);  // runden auf ##.#
                valueToShow = valueToShow / 100;
                selectedValue.setText(String.valueOf(valueToShow) + " V/m ");
            }
        }
    }

    //changes Bar back to gray color
    private void changeBarColorToNOTactiv(Integer position) {
        canvas.drawRect(coord.getLeft(position), coord.getTop(position), coord.getRight(position), coord.getBottom(position), paintBar);
        imageView.setImageBitmap(bitmap);
        chandeBarColorToFixed();
    }

    //fix added Frequencies and change theire color!
    public void chandeBarColorToFixed() {

        for (int i = 0; i < fixedBars.size(); i++) {
            canvas.drawRect(coord.getLeft(fixedBars.get(i)), coord.getTop(fixedBars.get(i)), coord.getRight(fixedBars.get(i)), coord.getBottom(fixedBars.get(i)), paintFix);
            imageView.setImageBitmap(bitmap);
        }
        markTheHighOnes();
    }

    private void markTheHighOnes(){
        if(measurement_type=='R'){
            for (int i = 0; i < anzahlBalken; i++) {
                if (rms[i]< -1.0 && rms[i] > -2.5) {
                    canvas.drawRect(coord.getLeft(i), coord.getTop(i), coord.getRight(i), coord.getBottom(i), paintToHigh);
                    imageView.setImageBitmap(bitmap);
                } else {}
            }
        }else {
            for (int i = 0; i < anzahlBalken; i++) {
                    if (peak[i]<-1.0 && peak[i] > -2.5) {
                        canvas.drawRect(coord.getLeft(i), coord.getTop(i), coord.getRight(i), coord.getBottom(i), paintToHigh);
                        imageView.setImageBitmap(bitmap);
                } else {}
            }
        }
    }

    //clears all fixed bars and removes them from list
    public void clearAllFixedBars() {
        for (int i = 0; i < fixedBars.size(); i++) {
            canvas.drawRect(coord.getLeft(fixedBars.get(i)), coord.getTop(fixedBars.get(i)), coord.getRight(fixedBars.get(i)), coord.getBottom(fixedBars.get(i)), paintBar);
            imageView.setImageBitmap(bitmap);
        }
        fixedBars.clear();
    }

    public void SetUpValuesForPlot() {
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        imageView = (ImageView) findViewById(R.id.image_bitmap);
        bitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        paintFix = new Paint();
        paintBar = new Paint();
        paintActive= new Paint();
        paintLimit = new Paint();
        paintToHigh = new Paint();
        colorLimit = OverviewScanPlotActivity.this.getResources().getColor(R.color.limitBar);
        colorFix = OverviewScanPlotActivity.this.getResources().getColor(R.color.fixedBar);
        colorBar = OverviewScanPlotActivity.this.getResources().getColor(R.color.normalBar);
        colorActive = OverviewScanPlotActivity.this.getResources().getColor(R.color.activeBar);
        colorToHigh = OverviewScanPlotActivity.this.getResources().getColor(R.color.toHighColor);
        paintToHigh.setColor(colorToHigh);
        paintLimit.setColor(colorLimit);
        paintLimit.setStyle(Paint.Style.FILL);
        paintFix.setColor(colorFix);
        paintFix.setStyle(Paint.Style.FILL);
        paintBar.setColor(colorBar);
        paintBar.setStyle(Paint.Style.FILL);
        paintActive.setColor(colorActive);
        paintActive.setStyle(Paint.Style.FILL);
        canvas = new Canvas(bitmap);
    }

    public void makePlot() {
        canvas.drawColor(Color.WHITE);
        canvas.drawRect(0,(float) (size.y*0.15),size.x,(float) (size.y*0.14),paintLimit);
        imageView.setImageBitmap(bitmap);
        if(measurement_type=='R') {
            coord = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, readRMS(), myMode,0.95,0.85);
        }else {
            coord = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, readPeak(), myMode, 0.95, 0.85);
        }
        for (int i = 0; i < anzahlBalken; i++) {
            canvas.drawRect(coord.getLeft(i), coord.getTop(i), coord.getRight(i), coord.getBottom(i), paintBar);      //somehow i get bottom wrong!
            Log.d("values plot", String.valueOf(coord.getTop(i)));
        }
        chandeBarColorToFixed();
        imageView.setImageBitmap(bitmap);
    }

    private void StartService() {
        Intent intent = new Intent(this, CommunicationService.class);
        startService(intent);
    }

    public class OverviewScanPlotActivityReceiver extends BroadcastReceiver {
        final String LOG_TAG;

        public OverviewScanPlotActivityReceiver(String LOG_TAG){
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

                    View_Packet_Trigger view_packet_trigger = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                    sendTrigger(view_packet_trigger.get_packet());
                    Log.d(LOG_TAG, "sent SCAN Trigger");

                }
                else if(new String(split_packet(4, 7, orgData)).equals("SCAN")){

                    Log.d(LOG_TAG, "got SCAN data");
                    Data_Packet_Exposi packetExposi = new Data_Packet_Exposi(orgData);
                    int freq = packetExposi.get_frequency();
                    int rms_exposi = packetExposi.get_rawData_rms();
                    int peak_exposi = packetExposi.get_rawData_peak();

                    double rms = calibration.get_rms(attenuator,freq, rms_exposi);
                    double peak = calibration.get_peak(attenuator, freq, peak_exposi);
                    if (peak != -1){
                        updatePeak(peak, freq);
                    }
                    if(rms != -1){
                        updateRMS(rms, freq);

                    }
                    makePlot();
                }
            }
        }
    }

    private synchronized void updatePeak(double newPeak, int freq){
        peak[((freq - 500) / 100)] = newPeak;
        Log.d(LOG_TAG, "updated Peak");

    }

    private synchronized void updateRMS(double newRMS, int freq){
        rms[((freq - 500) / 100)] = newRMS;
        Log.d(LOG_TAG, "updated RMS");

    }

    public synchronized double[] readPeak(){
        return peak;
    }

    public synchronized double[] readRMS(){
        return rms;
    }

    public synchronized void flushArrays() {
        Arrays.fill(rms, 0);
        Arrays.fill(peak, 0);
    }
}






//----------------------------------------------------------------------




