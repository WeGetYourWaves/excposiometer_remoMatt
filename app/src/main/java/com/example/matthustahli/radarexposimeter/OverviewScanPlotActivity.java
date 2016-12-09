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
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.log;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;

public class OverviewScanPlotActivity extends AppCompatActivity implements View.OnClickListener {

    private final String CHOOSENFREQ = "my_freq";
    private double[] rms = new double [96];
    private double[] peak = new double [96];
    private boolean[] isBarToHigh;
    private int attenuator;
    private int device_id;
    private char measurement_type = 'P';
    final String LOG_TAG = "Overview";
    OverviewScanPlotActivityReceiver overviewScanPlotActivityReceiver = new OverviewScanPlotActivityReceiver(LOG_TAG);
    Activity_Superclass calibration;
    boolean stateAddButton=true, stateNextButton=false, goingToNextActivity = true;



    //----------------------------------------------------------------------
    //setup variables
    Rectangle coord;
    int colorFix, colorBar, colorActive, colorLimit, colorToHigh, colorButtonActiveMode, colorButtonInactive ;
    Paint paintFix, paintBar, paintActive, paintLimit,paintToHigh;
    Display display;
    Point size;
    ImageView imageView;
    float abstandZwischenBalken = 5; //5dp
    int anzahlBalken = 96;//null zählt auch als balken
    Bitmap bitmap;
    Canvas canvas;
    Integer activeBar = 0;
    TextView selectedFreq, selectedValue, TVMaxValue,TVMinValue;
    ArrayList<Integer> fixedBars = new ArrayList<Integer>();
    LinearLayout settings;
    private String myMode;
    //All what has something to do with buttons
    private Integer clickCounterStatusPlot = 0;
    Animation animationSlideDown;
    Button b_normal, b_21dB, b_41dB, b_accu, b_peak;
    ImageButton b_settings, addButton, clearButton, refreshButton, nextButton;
    double maxPlot, minPlot, hightPlotScaled, scaleY=0.85, scaleX=0.95;
    int counter =0;

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
        setButtonsOnClickListener();
        refreshStatusButtons();

        SetUpValuesForPlot();  //Makes the plot and draws it.
        makePlot();

        ActivateTouchOnPlot();
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
            rotate.setRepeatCount(3);
            refreshButton.startAnimation(rotate);
        }
    }

    //downOrUp: 0=godown, 1=goup
    private void handlesActivatingDropDown(Integer downOrUp, String errormessage) {
        //sets listener, and handles drop down and drop up
        animationSlideDown = AnimationUtils.loadAnimation(this, R.anim.anim_drop_down);
        final LinearLayout layout_dropDown = (LinearLayout) findViewById(R.id.layout_dropDown);
        TextView allert_text = (TextView) findViewById(R.id.textView_dropDownAllert);
        if(downOrUp==0){
            goingToNextActivity=false;
            allert_text.setText(errormessage);
            selectedFreq.setText("");
            selectedValue.setText("");
            layout_dropDown.setVisibility(View.VISIBLE);
            layout_dropDown.bringToFront();
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

    private void changeColorOfSelectedMode(){
        b_normal.setBackgroundColor(colorButtonInactive);
        b_21dB.setBackgroundColor(colorButtonInactive);
        b_41dB.setBackgroundColor(colorButtonInactive);
        b_accu.setBackgroundColor(colorButtonInactive);
        switch (myMode){
            case "normal mode":
                b_normal.setBackgroundColor(colorButtonActiveMode);
                break;
            case "-21 dB":
                b_21dB.setBackgroundColor(colorButtonActiveMode);
                break;
            case "-42 dB":
                b_41dB.setBackgroundColor(colorButtonActiveMode);
                break;
            case "LNA on":
                b_accu.setBackgroundColor(colorButtonActiveMode);
                break;
        }
    }

    //----------------------------------------------------------------
    @Override
    public void onClick(View v) {
        View_Packet_Trigger viewStop = new View_Packet_Trigger(device_id, attenuator, (char) 0);

        switch (v.getId()) {
            case R.id.b_mode_normal:
                attenuator = 0;
                change_MinMaxPlot();
                myMode = "normal mode";
                //makePlot();
                settings.setVisibility(LinearLayout.GONE);
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger0 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger0.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 0");
                break;
            case R.id.b_mode_21db:
                myMode = "-21 dB";
                attenuator = 1;
                change_MinMaxPlot();
                //makePlot();
                settings.setVisibility(LinearLayout.GONE);
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger1 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger1.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 1");
                break;
            case R.id.b_mode_42db:
                myMode = "-42 dB";
                attenuator = 2;
                change_MinMaxPlot();
                //makePlot();

                settings.setVisibility(LinearLayout.GONE);
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger2 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger2.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 2");
                break;

            case R.id.b_mode_LNA:
                myMode = "LNA on";
                attenuator = 3;
                change_MinMaxPlot();
                //makePlot();
                settings.setVisibility(LinearLayout.GONE);
                sendTrigger(viewStop.get_packet());
                View_Packet_Trigger view_packet_trigger3 = new View_Packet_Trigger(device_id, attenuator, measurement_type);
                sendTrigger(view_packet_trigger3.get_packet());
                Log.d(LOG_TAG, "sent SCAN Trigger attenuator 3");
                break;
            case R.id.switch_to_peak:
                if (measurement_type == 'P')    measurement_type = 'R';
                else measurement_type = 'P';
                change_MinMaxPlot();

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
                stateNextButton=true;       //because we just added something
                if(checkIfFreqAlreadyAdded()){break;}
                if (fixedBars.size() < 6) {
                    fixedBars.add(activeBar);
                    chandeBarColorToFixed();
                }
                if(fixedBars.size()>=6){
                    stateAddButton=false;
                    //Toast.makeText(OverviewScanPlotActivity.this, "FULL ARRAY", Toast.LENGTH_SHORT).show();
                }
                refreshStatusButtons();
                break;
            case R.id.clear_button:
                stateAddButton=true;
                if (fixedBars.size() > 0) {
                    //setbuttonstatus
                    int lastAddedFreq = fixedBars.get(fixedBars.size() - 1);
                    fixedBars.remove(fixedBars.size() - 1);
                    changeBarColorToNOTactiv(lastAddedFreq);

                }
                if(fixedBars.size()==0){
                stateNextButton=false;
            }
                changeBarColorToNOTactiv(activeBar);
                selectedFreq.setText("");
                selectedValue.setText("");
                refreshStatusButtons();
                break;
            case R.id.next_button:
                if(stateNextButton==true && goingToNextActivity==true){
                    sendTrigger(viewStop.get_packet());
                    OpenDetailViewActivity();
                }
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
                    changeColorOfSelectedMode();
                }

                break;
        }
    }

    private void refreshStatusButtons(){
        if(stateNextButton==true){
            nextButton.setBackgroundResource(R.drawable.oval_next);
        }else{
            nextButton.setBackgroundResource(R.drawable.oval_next_inactive);
        }
        if(stateAddButton==true){
            addButton.setBackgroundResource(R.drawable.oval_add);
        }else{
            addButton.setBackgroundResource(R.drawable.oval_add_inactive);
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
        TVMaxValue = (TextView) findViewById(R.id.tv_maxValueScale);
        TVMinValue = (TextView) findViewById((R.id.tv_minValueScale));
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
                    selectedValue.setText('<' + Double.toString(minPlot));
                }else{
                    selectedValue.setText('>' + Double.toString(maxPlot));
                }
            }else{
            double valueToShow = rms[position] ;
            valueToShow= Math.round(valueToShow*1000);  // runden auf ##.#
            valueToShow = valueToShow/1000;
            selectedValue.setText(String.valueOf(valueToShow)+ " V/m ");
            }
        }else{
            if(peak[position]<-1){
                if(peak[position]<-2.5){
                    selectedValue.setText('<' + Double.toString(minPlot));
                }else{
                    selectedValue.setText('>' + Double.toString(maxPlot));
                }
            }else {
                double valueToShow = peak[position];
                valueToShow = Math.round(valueToShow * 1000);  // runden auf ##.#
                valueToShow = valueToShow / 1000;
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
        colorButtonActiveMode = OverviewScanPlotActivity.this.getResources().getColor(R.color.active_mode_button_color);
        colorButtonInactive = OverviewScanPlotActivity.this.getResources().getColor(R.color.inactive_mode_button_color);
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
        isBarToHigh = new boolean[anzahlBalken];
        double [] quickfix = new double[anzahlBalken];
        Arrays.fill(quickfix, 0);
        setMarginOfMaxValueView();
    }
    public void setMarginOfMaxValueView(){
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) TVMaxValue.getLayoutParams();
        params.topMargin = (int) (size.y*0.15);
    }

    public void makePlot() {
        canvas.drawColor(Color.WHITE);
        canvas.drawRect(0,(float) (size.y*0.15),size.x,(float) (size.y*0.14),paintLimit);
        imageView.setImageBitmap(bitmap);
        if(measurement_type=='R') {
            coord = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, readRMS(),scaleX,scaleY,maxPlot,minPlot);
        }else {
            coord = new Rectangle(anzahlBalken, abstandZwischenBalken, size.x, size.y, readPeak(), scaleX, scaleY,maxPlot,minPlot);
        }
        for (int i = 0; i < anzahlBalken; i++) {
            canvas.drawRect(coord.getLeft(i), coord.getTop(i), coord.getRight(i), coord.getBottom(i), paintBar);      //somehow i get bottom wrong!
            Log.d("values plot", String.valueOf(coord.getTop(i)));
        }
        chandeBarColorToFixed();
        imageView.setImageBitmap(bitmap);
        setValueOfMaxAndMinInYAxe();
    }

//    synchronized public void makePlot() {
//        canvas.drawColor(Color.WHITE);
//        canvas.drawRect(0, (float) (size.y * 0.15), size.x, (float) (size.y * 0.14), paintLimit); //sets the limitbar.
//        imageView.setImageBitmap(bitmap);
//        int i = counter % anzahlBalken;
//        //Log.d("values plot", String.valueOf(coord.getTop(i)));
//            if (measurement_type == 'R') {
//                double rmsValue = readRMS()[i];
//                canvas.drawRect(coord.getLeft(i), readRMS(), coord.getRight(i), coord.getBottom(i), paintBar);      //somehow i get bottom wrong!
//            } else {
//                double peakValue = readPeak()[i];
//                canvas.drawRect(coord.getLeft(i), re, coord.getRight(i), coord.getBottom(i), paintBar);      //somehow i get bottom wrong!
//            }
//            imageView.setImageBitmap(bitmap);
//            chandeBarColorToFixed();
//            setValueOfMaxAndMinInYAxe();
//        counter ++;
//    }

    /*private float hightOpBar(double meassurement) {
        double rValue=size.y;
        if (meassurement < -1) {
            if (meassurement < -2.5) {                  //balken grösse =0
            } else {                                    //balkengrösse ist max
                rValue = (float) (size.y - size.y * scaleY); //full size
            }
        } else {//draw normal value    from top, so sizeY - hightInRelation
            rValue = (log(maxPlot) - log(meassurement)) / hightPlotScaled;  //(log(max)-log(input))/(log(max)-log(min)) = %normed hight
            rValue = rValue * size.y * scaleY; /*//*plot hight
            rValue = size.y - rValue;   //as we need value from top of screen to top of bar for its hight.
        }
        return (float)rValue;
    }*/

    private void setValueOfMaxAndMinInYAxe(){
        TVMaxValue.setText(String.valueOf(maxPlot)+" V/m");
        TVMinValue.setText(String.valueOf(minPlot)+" V/m");
        TVMinValue.bringToFront();
        TVMaxValue.bringToFront();
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
                    change_MinMaxPlot();
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
                else if (new String(split_packet(4, 7, orgData)).equals("EROR")){

                    Log.d(LOG_TAG, "got EROR packet");
                    Error_Packet_Exposi error_packet = new Error_Packet_Exposi(orgData);
                    int errorCode = error_packet.get_errorCode();
                    String errorMessage = error_packet.get_errorMessage();
                    if (errorCode == 1){
                        //connection to ESP lost
                        handlesActivatingDropDown(0, errorMessage);
                    }
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

    public void change_MinMaxPlot(){
        maxPlot = calibration.get_maxPlot(attenuator, measurement_type);
        minPlot = calibration.get_minPlot(attenuator, measurement_type);
        hightPlotScaled = log(maxPlot)- log(minPlot); //value for scaling the meassurements to the plot size. See makePlot()
        TVMaxValue.setText(String.valueOf(maxPlot)+" V/m");
        TVMinValue.setText(String.valueOf(minPlot)+" V/m");
        TVMinValue.bringToFront();
        TVMaxValue.bringToFront();
    }
}






//----------------------------------------------------------------------




